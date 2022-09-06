package sinc;

import sinc.common.*;
import sinc.util.graph.BaseGraphNode;
import sinc.util.graph.FeedbackVertexSetSolver;
import sinc.util.graph.Tarjan;

import java.io.*;
import java.util.*;

public abstract class SInC {

    protected static final int CONST_ID = -1;
    protected static final BaseGraphNode<Predicate> AXIOM_NODE = new BaseGraphNode<>(new Predicate("⊥", 0));

    protected final SincConfig config;
    protected final String kbPath;
    protected final String dumpPath;
    protected final PrintWriter logger;

    protected final List<Rule> hypothesis = new ArrayList<>();
    protected final Map<Predicate, BaseGraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    protected final Map<BaseGraphNode<Predicate>, Set<BaseGraphNode<Predicate>>> dependencyGraph = new HashMap<>();
    protected final Set<Predicate> startSet = new HashSet<>();
    protected final Set<Predicate> counterExamples = new HashSet<>();
    protected final Set<String> supplementaryConstants = new HashSet<>();
    protected final PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    /* 终止执行的flag */
    protected boolean interrupted = false;

    protected static class GraphAnalyseResult {
        public int startSetSize = 0;
        public int startSetSizeWithoutFvs = 0;
        public int sccNumber = 0;
        public int sccVertices = 0;
        public int fvsVertices = 0;
    }

    public SInC(SincConfig config, String kbPath, String dumpPath, String logPath) {
        this.config = config;
        this.kbPath = kbPath;
        this.dumpPath = dumpPath;
        PrintWriter writer;
        try {
            writer = (null == logPath) ? new PrintWriter(System.out, true) : new PrintWriter(logPath);
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.logger = writer;
        Rule.MIN_FACT_COVERAGE = config.minFactCoverage;
        Rule.monitor = new RuleMonitor();
    }

    /**
     * BK format(each line):
     * [pred]\t[arg1]\t[arg2]\t...\t[argn]
     *
     * Assume no duplication.
     */
    abstract protected KbStatistics loadKb();

    abstract protected List<String> getTargetFunctors();

    abstract protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache);

    protected Rule findRule(String headFunctor) throws InterruptedSignal {
        final Set<RuleFingerPrint> cache = new HashSet<>();
        final Rule start_rule = getStartRule(headFunctor, cache);

        /* 初始化beams */
        final Eval.EvalMetric eval_metric = config.evalMetric;
        final int beam_width = config.beamWidth;
        Set<Rule> beams = new HashSet<>();
        beams.add(start_rule);
        PriorityQueue<Rule> optimals = new PriorityQueue<>(
                Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
        );

        /* 寻找局部最优（只要进入这个循环，一定有局部最优） */
        while (true) {
            /* 根据当前beam遍历下一轮的所有candidates */
            PriorityQueue<Rule> candidates = new PriorityQueue<>(
                    Comparator.comparingDouble((Rule r) -> r.getEval().value(eval_metric)).reversed()
            );
            for (Rule r: beams) {
                logger.printf("Extend: %s\n", r);
                logger.flush();

                /* 遍历r的邻居 */
                int existing_candidates = candidates.size();
                findExtension(r, candidates);
                int extensions_cnt = candidates.size() - existing_candidates;
                int origins_cnt = 0;
                if (config.searchOrigins) {
                    findOrigin(r, candidates);
                    origins_cnt = candidates.size() - existing_candidates - extensions_cnt;
                }

                if (0 == (extensions_cnt + origins_cnt)) {
                    optimals.add(r);
                }

                /* 监测：分支数量信息 */
                final PerformanceMonitor.BranchInfo branch_info = new PerformanceMonitor.BranchInfo(
                        r.size(), extensions_cnt, origins_cnt
                );
                performanceMonitor.branchProgress.add(branch_info);
            }

            /* 如果有多个optimal，选择最优的返回 */
            final Rule loc_opt = optimals.peek();
            if (null != loc_opt) {
                final Rule peek_rule = candidates.peek();
                if (
                        null == peek_rule ||
                        /* 如果local optimal在当前的candidates里面不是最优的，则排除 */
                        loc_opt.getEval().value(eval_metric) > peek_rule.getEval().value(eval_metric)
                ) {
                    return loc_opt;
                }
            }

            /* 找出下一轮的beams */
            Set<Rule> new_beams = new HashSet<>();
            Rule beam_rule;
            while (new_beams.size() < beam_width && (null != (beam_rule = candidates.poll()))) {
                new_beams.add(beam_rule);
            }
            beams = new_beams;
        }
    }

    /**
     * 遍历extensions，把得分更高的放入candidates列表
     */
    protected void findExtension(final Rule rule, PriorityQueue<Rule> candidates) throws InterruptedSignal {
        Eval eval = rule.getEval();
        if (config.stopCompressionRate <= eval.value(Eval.EvalMetric.CompressionRate) || 0 == eval.getNegCnt()) {
            /* 如果到达停止阈值，不再进行extension */
            return;
        }

        /* 先找到所有空白的参数 */
        class ArgPos {
            public final int predIdx;
            public final int argIdx;

            public ArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        List<ArgPos> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
            final Predicate pred_info = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                if (null == pred_info.args[arg_idx]) {
                    vacant_list.add(new ArgPos(pred_idx, arg_idx));
                }
            }
        }

        /* 尝试增加已知变量 */
        final Map<String, Integer> func_2_arity_map = getFunctor2ArityMap();
        for (int var_id = 0; var_id < rule.usedBoundedVars(); var_id++) {
            List<VarIndicator> var_locations = rule.getVarLocations(var_id);
            for (ArgPos vacant: vacant_list) {
                for (VarIndicator var_location: var_locations) {
                    if (columnSimilar(rule.getPredicate(vacant.predIdx).functor, vacant.argIdx, var_location.functor, var_location.idx)) {
                        /* 尝试将已知变量填入空白参数 */
                        final Rule new_rule = rule.clone();
                        final Rule.UpdateStatus update_status = new_rule.boundFreeVar2ExistingVar(
                                vacant.predIdx, vacant.argIdx, var_id
                        );
                        checkThenAddRule(update_status, new_rule, rule, candidates);
                        break;
                    }
                }
            }

            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    for (VarIndicator var_location: var_locations) {
                        if (columnSimilar(functor, arg_idx, var_location.functor, var_location.idx)) {
                            final Rule new_rule = rule.clone();
                            final Rule.UpdateStatus update_status = new_rule.boundFreeVar2ExistingVar(
                                    functor, arity, arg_idx, var_id
                            );
                            checkThenAddRule(update_status, new_rule, rule, candidates);
                        }
                    }
                }
            }
        }

        final Map<String, List<String>[]> func_2_promising_const_map = getFunctor2PromisingConstantMap();
        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            final ArgPos first_vacant = vacant_list.get(i);
            final String functor1 = rule.getPredicate(first_vacant.predIdx).functor;

            /* 拓展一个常量 */
            final Predicate predicate = rule.getPredicate(first_vacant.predIdx);
            final List<String> const_list = func_2_promising_const_map.get(predicate.functor)[first_vacant.argIdx];
            for (String const_symbol: const_list) {
                final Rule new_rule = rule.clone();
                final Rule.UpdateStatus update_status = new_rule.boundFreeVar2Constant(
                        first_vacant.predIdx, first_vacant.argIdx, const_symbol
                );
                checkThenAddRule(update_status, new_rule, rule, candidates);
            }

            /* 找到两个位置尝试同一个新变量 */
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                final ArgPos second_vacant = vacant_list.get(j);
                if (columnSimilar(functor1, first_vacant.argIdx,rule.getPredicate(second_vacant.predIdx).functor, second_vacant.argIdx)) {
                    final Rule new_rule = rule.clone();
                    final Rule.UpdateStatus update_status = new_rule.boundFreeVars2NewVar(
                            first_vacant.predIdx, first_vacant.argIdx, second_vacant.predIdx, second_vacant.argIdx
                    );
                    checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    if (columnSimilar(functor1, first_vacant.argIdx, functor, arg_idx)) {
                        final Rule new_rule = rule.clone();
                        final Rule.UpdateStatus update_status = new_rule.boundFreeVars2NewVar(
                                functor, arity, arg_idx, first_vacant.predIdx, first_vacant.argIdx
                        );
                        checkThenAddRule(update_status, new_rule, rule, candidates);
                    }
                }
            }
        }
    }

    abstract protected Map<String, Integer> getFunctor2ArityMap();

    abstract protected Map<String, List<String>[]> getFunctor2PromisingConstantMap();

    protected void findOrigin(Rule rule, PriorityQueue<Rule> candidates) throws InterruptedSignal {
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
            /* 从Head开始删除可能会出现Head中没有Bounded Var但是Body不为空的情况，按照定义来说，这种规则是不在
               搜索空间中的，但是会被isInvalid方法检查出来 */
            final Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (null != predicate.args[arg_idx]) {
                    final Rule new_rule = rule.clone();
                    final Rule.UpdateStatus update_status = new_rule.removeBoundedArg(pred_idx, arg_idx);
                    checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
        }
    }

    abstract protected UpdateResult updateKb(Rule rule);

    protected void updateGraph(List<Predicate[]> groundings) {
        for (Predicate[] grounding: groundings) {
            final Predicate head_pred = grounding[Rule.HEAD_PRED_IDX];
            final BaseGraphNode<Predicate> head_node = predicate2NodeMap.computeIfAbsent(
                    head_pred, k -> new BaseGraphNode<>(head_pred)
            );
            dependencyGraph.compute(head_node, (h, dependencies) -> {
                if (null == dependencies) {
                    dependencies = new HashSet<>();
                }
                if (1 >= grounding.length) {
                    /* dependency为公理 */
                    dependencies.add(AXIOM_NODE);
                } else {
                    for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < grounding.length; pred_idx++) {
                        final Predicate body_pred = grounding[pred_idx];
                        final BaseGraphNode<Predicate> body_node = predicate2NodeMap.computeIfAbsent(
                                body_pred, kk -> new BaseGraphNode<>(body_pred)
                        );
                        dependencies.add(body_node);
                    }
                }
                return dependencies;
            });
        }
    }

    protected GraphAnalyseResult findNecessaries() {
        /* 在更新KB的时候已经把Graph顺便做好了，这里只需要查找对应的点即可 */
        /* 找出所有不能被prove的点 */
        final GraphAnalyseResult result = new GraphAnalyseResult();
        for (Predicate fact : getOriginalKb()) {
            if (!dependencyGraph.containsKey(new BaseGraphNode<>(fact))) {
                startSet.add(fact);
            }
        }

        /* 找出所有SCC中的覆盖点 */
        result.startSetSizeWithoutFvs = startSet.size();
        final Tarjan<BaseGraphNode<Predicate>> tarjan = new Tarjan<>(dependencyGraph);
        final List<Set<BaseGraphNode<Predicate>>> sccs = tarjan.run();
        result.sccNumber = sccs.size();

        for (Set<BaseGraphNode<Predicate>> scc: sccs) {
            /* 找出FVS的一个解，并把之放入start_set */
            final FeedbackVertexSetSolver<BaseGraphNode<Predicate>> fvs_solver =
                    new FeedbackVertexSetSolver<>(dependencyGraph, scc);
            final Set<BaseGraphNode<Predicate>> fvs = fvs_solver.run();
            for (BaseGraphNode<Predicate> node: fvs) {
                startSet.add(node.content);
            }
            result.sccVertices += scc.size();
            result.fvsVertices += fvs.size();
        }

        result.startSetSize = startSet.size();
        return result;
    }

    protected void findSupplementaryConstants() {
        /* 汇总constants(fatcs, counter examples, rules) */
        final Set<String> occurred_constants = new HashSet<>();
        for (Predicate fact: startSet) {
            for (Argument argument: fact.args) {
                occurred_constants.add(argument.name);
            }
        }
        for (Predicate ce: counterExamples) {
            for (Argument argument: ce.args) {
                occurred_constants.add(argument.name);
            }
        }
        for (Rule r: hypothesis) {
            for (int pred_idx = 0; pred_idx < r.length(); pred_idx++) {
                final Predicate pred = r.getPredicate(pred_idx);
                for (Argument argument: pred.args) {
                    if (null != argument && !argument.isVar) {
                        occurred_constants.add(argument.name);
                    }
                }
            }
        }
        for (String constant: getAllConstants()) {
            if (!occurred_constants.contains(constant)) {
                supplementaryConstants.add(constant);
            }
        }
    }

    public boolean recover() {
        SincRecovery recovery = new SincRecovery(hypothesis, startSet, counterExamples, supplementaryConstants);
        final Set<Predicate> recovered_kb = recovery.recover();
        boolean success = getOriginalKb().equals(recovered_kb);
        if (!success) {
            System.err.printf(
                    "[ERROR] Validation failed: %d expected but only %d recovered.",
                    getOriginalKb().size(), recovered_kb.size()
            );
        }
        return success;
    }

    protected void dumpResult() {
        if (null == dumpPath) {
            return;
        }
        try {
            PrintWriter writer = new PrintWriter(dumpPath);
//            PrintWriter writer = (null == dumpPath) ? new PrintWriter(System.out) : new PrintWriter(dumpPath);
            /* Dump Hypothesis */
//            writer.println("# Hypothesis");
            for (Rule r: hypothesis) {
                writer.println(r.toDumpString());
            }
            writer.println();

            /* Dump Start Set */
//            writer.println("# Essential Knowledge");
            for (Predicate p: startSet) {
                writer.print(p.functor);
                for (Argument arg: p.args) {
                    writer.print('\t');
                    writer.print(arg.name);
                }
                writer.print('\n');
            }
            writer.println();

            /* Dump Counter Example Set */
//            writer.println("# Counter Examples");
            for (Predicate p: counterExamples) {
                writer.print(p.functor);
                for (Argument arg: p.args) {
                    writer.print('\t');
                    writer.print(arg.name);
                }
                writer.print('\n');
            }
            writer.println();

            /* Dump Supplementary Constant Symbols */
            for (String constant: supplementaryConstants) {
                writer.println(constant);
            }

            writer.close();
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] Dump Failed.");
            e.printStackTrace();
        }
    }

    abstract protected Set<Predicate> getOriginalKb();

    protected void showMonitor() {
        performanceMonitor.show(logger);
        Rule.monitor.show(logger);
        logger.flush();
    }

    public List<Rule> getHypothesis() {
        return hypothesis;
    }

    public Set<Predicate> getStartSet() {
        return startSet;
    }

    public Set<Predicate> getCounterExamples() {
        return counterExamples;
    }

    public Set<String> getSupplementaryConstants() {
        return supplementaryConstants;
    }

    public abstract Set<String> getAllConstants();

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    protected void checkThenAddRule(Rule.UpdateStatus updateStatus, Rule extendedRule, Rule originalRule, Queue<Rule> candidates)
            throws InterruptedSignal {
        switch (updateStatus) {
            case NORMAL:
                if (extendedRule.getEval().value(config.evalMetric) > originalRule.getEval().value(config.evalMetric)) {
                    candidates.add(extendedRule);
                }
                break;
            case INVALID:
                performanceMonitor.invalidSearches++;
                break;
            case DUPLICATED:
                performanceMonitor.duplications++;
                break;
            case INSUFFICIENT_COVERAGE:
                performanceMonitor.fcFilteredRules++;
                break;
            case TABU_PRUNED:
                performanceMonitor.tabuPruned++;
                break;
            default:
                throw new Error("Unknown Update Status of Rule: " + updateStatus.name());
        }
        recordRuleStatus(extendedRule, updateStatus);
        if (interrupted) {
            throw new InterruptedSignal("Interrupted");
        }
    }

    protected abstract void recordRuleStatus(Rule rule, Rule.UpdateStatus updateStatus);

    protected void targetDone(String functor) {
        /* 这里什么也不做，给后续处理留空间 */
    }

    public abstract String getModelName();

    protected abstract boolean columnSimilar(String functor1, int idx1, String functor2, int idx2);

    private void runHandler() {
        final long time_start = System.currentTimeMillis();
        try {
            /* 加载KB */
            KbStatistics kb_stat = loadKb();
            performanceMonitor.kbSize = kb_stat.facts;
            performanceMonitor.kbFunctors = kb_stat.functors;
            performanceMonitor.kbConstants = kb_stat.constants;
            performanceMonitor.totalConstantSubstitutions = kb_stat.totalConstantSubstitutions;
            performanceMonitor.actualConstantSubstitutions = kb_stat.actualConstantSubstitutions;
            final long time_kb_loaded = System.currentTimeMillis();
            performanceMonitor.kbLoadTime = time_kb_loaded - time_start;

            /* 逐个functor找rule */
            final List<String> target_head_functors = getTargetFunctors();
            final int total_targets = target_head_functors.size();
            do {
                final long time_rule_finding_start = System.currentTimeMillis();
                final int last_idx = target_head_functors.size() - 1;
                final String functor = target_head_functors.get(last_idx);
                final Rule rule = findRule(functor);
                final long time_rule_found = System.currentTimeMillis();
                performanceMonitor.hypothesisMiningTime += time_rule_found - time_rule_finding_start;

                if (null != rule && rule.getEval().useful(config.evalMetric)) {
                    logger.printf("Found: %s\n", rule);
                    hypothesis.add(rule);
                    performanceMonitor.hypothesisSize += rule.size();

                    /* 更新grpah和counter example */
                    UpdateResult update_result = updateKb(rule);
                    counterExamples.addAll(update_result.counterExamples);
                    updateGraph(update_result.groundings);
                    final long time_kb_updated = System.currentTimeMillis();
                    performanceMonitor.dependencyAnalysisTime += time_kb_updated - time_rule_found;
                } else {
                    target_head_functors.remove(last_idx);
                    logger.printf("Target Done: %d/%d\n", total_targets - target_head_functors.size(), total_targets);
                    targetDone(functor);
                }
            } while (!target_head_functors.isEmpty());
            performanceMonitor.hypothesisRuleNumber = hypothesis.size();
            performanceMonitor.counterExampleSize = counterExamples.size();

            /* 解析Graph找start set */
            final long time_graph_analyse_begin = System.currentTimeMillis();
            GraphAnalyseResult graph_analyse_result = findNecessaries();
            performanceMonitor.startSetSize = graph_analyse_result.startSetSize;
            performanceMonitor.startSetSizeWithoutFvs = graph_analyse_result.startSetSizeWithoutFvs;
            performanceMonitor.sccNumber = graph_analyse_result.sccNumber;
            performanceMonitor.sccVertices = graph_analyse_result.sccVertices;
            performanceMonitor.fvsVertices = graph_analyse_result.fvsVertices;
            findSupplementaryConstants();
            performanceMonitor.supplementaryConstants = supplementaryConstants.size();
            final long time_start_set_found = System.currentTimeMillis();
            performanceMonitor.dependencyAnalysisTime += time_start_set_found - time_graph_analyse_begin;

            /* 打印所有rules */
            logger.println("\n### Hypothesis Found ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();

            /* 记录结果 */
            dumpResult();
            final long time_dumped = System.currentTimeMillis();
            performanceMonitor.dumpTime = time_dumped - time_start_set_found;
            performanceMonitor.totalTime = time_dumped - time_start;

            /* 检查结果 */
            if (config.validation) {
                if (!recover()) {
                    System.err.println("[ERROR] Validation Failed");
                }
            }
            final long time_validation_done = System.currentTimeMillis();
            performanceMonitor.validationTime = time_validation_done - time_dumped;

            showMonitor();

            if (config.debug) {
                /* Todo: 图结构上传Neo4j */
                logger.println("[DEBUG] Upload Graph to Neo4J...");
            }
        } catch (InterruptedSignal e) {
            /* 处理interruption (stdin里随便输入点什么) */
            /* 从结束 Rule Finding 开始 */
            performanceMonitor.hypothesisRuleNumber = hypothesis.size();
            performanceMonitor.counterExampleSize = counterExamples.size();

            /* 解析Graph找start set */
            final long time_graph_analyse_begin = System.currentTimeMillis();
            GraphAnalyseResult graph_analyse_result = findNecessaries();
            performanceMonitor.startSetSize = graph_analyse_result.startSetSize;
            performanceMonitor.startSetSizeWithoutFvs = graph_analyse_result.startSetSizeWithoutFvs;
            performanceMonitor.sccNumber = graph_analyse_result.sccNumber;
            performanceMonitor.sccVertices = graph_analyse_result.sccVertices;
            performanceMonitor.fvsVertices = graph_analyse_result.fvsVertices;
            final long time_start_set_found = System.currentTimeMillis();
            performanceMonitor.dependencyAnalysisTime += time_start_set_found - time_graph_analyse_begin;

            /* 打印所有rules */
            logger.println("\n### Hypothesis Found (Interrupted) ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();

            /* 记录结果 */
            dumpResult();
            final long time_dumped = System.currentTimeMillis();
            performanceMonitor.dumpTime = time_dumped - time_start_set_found;
            performanceMonitor.totalTime = time_dumped - time_start;

            showMonitor();

            logger.println("!!! The Result is Reserved Before INTERRUPTION !!!");
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
            System.err.flush();

            /* 从结束 Rule Finding 开始 */
            performanceMonitor.hypothesisRuleNumber = hypothesis.size();
            performanceMonitor.counterExampleSize = counterExamples.size();

            /* 解析Graph找start set */
            final long time_graph_analyse_begin = System.currentTimeMillis();
            GraphAnalyseResult graph_analyse_result = findNecessaries();
            performanceMonitor.startSetSize = graph_analyse_result.startSetSize;
            performanceMonitor.startSetSizeWithoutFvs = graph_analyse_result.startSetSizeWithoutFvs;
            performanceMonitor.sccNumber = graph_analyse_result.sccNumber;
            performanceMonitor.sccVertices = graph_analyse_result.sccVertices;
            performanceMonitor.fvsVertices = graph_analyse_result.fvsVertices;
            final long time_start_set_found = System.currentTimeMillis();
            performanceMonitor.dependencyAnalysisTime += time_start_set_found - time_graph_analyse_begin;

            /* 打印当前已经得到的rules */
            logger.println("\n### Hypothesis Found (Before Error) ###");
            for (Rule rule : hypothesis) {
                logger.println(rule);
            }
            logger.println();

            /* 记录结果 */
            final long time_dump_start = System.currentTimeMillis();
            dumpResult();
            final long time_dumped = System.currentTimeMillis();
            performanceMonitor.dumpTime = time_dumped - time_dump_start;
            performanceMonitor.totalTime = time_dumped - time_start;

            showMonitor();

            logger.println("!!! The Result is Reserved Before EXCEPTION !!!");
        }
    }

    public final void run() {
        Thread task = new Thread(this::runHandler);
        task.start();

        try {
            while (task.isAlive() && (System.in.available() <= 0)) {
                Thread.sleep(1000);
            }
            logger.println("Exit normally");
            logger.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            interrupted = true;
            try {
                task.join();
                logger.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
