package sinc;

import sinc.common.Argument;
import sinc.common.Constant;
import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.impl.cached.MemKB;
import sinc.util.ComparableArray;

import java.util.*;

public class SincRecovery {
    protected final Set<Predicate> necessaryFacts;
    protected final Set<Predicate> counterExamples;
    protected final List<Rule> hypothesis;
    protected final Set<String> constants;
    protected final MemKB kb;
    protected final Set<Predicate> recoveredFacts = new HashSet<>();

    static class PredicateCache {
        public final Predicate predicate;
        public Map<String, Set<Predicate>>[] indexedInclusion;

        public PredicateCache(Predicate predicate, Map<String, Set<Predicate>>[] indexedInclusion) {
            this.predicate = new Predicate(predicate);
            this.indexedInclusion = indexedInclusion;
        }

        public PredicateCache(PredicateCache another) {
            this.predicate = new Predicate(another.predicate);
            this.indexedInclusion = another.indexedInclusion;  // copy on write
        }
    }

    public SincRecovery(
            List<Rule> hypothesis, Set<Predicate> necessaryFacts, Set<Predicate> counterExamples,
            Set<String> deltaConstantSet
    ) {
        this.hypothesis = hypothesis;
        this.necessaryFacts = necessaryFacts;
        this.counterExamples = counterExamples;
        this.kb = new MemKB();
        for (Predicate f: necessaryFacts) {
            kb.addFact(f);
        }

        /* 汇总constants(fatcs, counter examples, rules, supplementary) */
        constants = new HashSet<>(kb.allConstants());
        for (Rule r: hypothesis) {
            for (int pred_idx = 0; pred_idx < r.length(); pred_idx++) {
                final Predicate pred = r.getPredicate(pred_idx);
                /* 保险起见，把所有的functor声明一遍 */
                kb.declareFunctor(pred.functor, pred.arity());

                for (Argument argument: pred.args) {
                    if (null != argument && !argument.isVar) {
                        constants.add(argument.name);
                    }
                }
            }
        }
        for (Predicate ce: counterExamples) {
            for (Argument argument: ce.args) {
                constants.add(argument.name);
            }
        }
        constants.addAll(deltaConstantSet);
    }

    public Set<Predicate> recover() {
        recoveredFacts.addAll(necessaryFacts);
        int added_facts = 1;
        while (0 < added_facts) {
            added_facts = 0;
            for (Rule r : hypothesis) {
                added_facts += inferByRule(r);
            }
        }
//        recoveredFacts.removeIf(counterExamples::contains);
        return recoveredFacts;
    }

    protected int inferByRule(Rule r) {
        /* 统计head中的变量信息 */
        final Map<Integer, List<Integer>> head_var_2_loc_map = new HashMap<>();  // Head Only LV Locations
        int uv_id = r.usedBoundedVars();
        final Predicate head_pred = new Predicate(r.getHead());
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            final Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_var_2_loc_map.put(uv_id, new ArrayList<>(Collections.singleton(arg_idx)));
                uv_id++;
            } else {
                if (argument.isVar) {
                    final int idx = arg_idx;
                    head_var_2_loc_map.compute(argument.id, (id, locs) -> {
                        if (null == locs) {
                            locs = new ArrayList<>();
                        }
                        locs.add(idx);
                        return locs;
                    });
                }
            }
        }

        /* 统计Body中的变量信息 */
        class PredArgPos {
            final int predIdx;
            final int argIdx;

            public PredArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        final Map<Integer, List<PredArgPos>> body_var_2_loc_map = new HashMap<>();
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < r.length(); pred_idx++) {
            final Predicate body_pred = r.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    body_var_2_loc_map.put(uv_id, new ArrayList<>(Collections.singleton(
                            new PredArgPos(pred_idx, arg_idx))
                    ));
                    uv_id++;
                } else {
                    if (argument.isVar) {
                        final List<PredArgPos> pos_list = body_var_2_loc_map.computeIfAbsent(
                                argument.id, k -> new ArrayList<>()
                        );
                        pos_list.add(new PredArgPos(pred_idx, arg_idx));
                    }
                }
            }
        }

        /* 构造所有符合要求的grounding(for BLGV, BOLV, constant) */
        final List<List<PredicateCache>> groundings = new LinkedList<>();
        final List<PredicateCache> init_grounding = new ArrayList<>(Collections.singleton(null));  // 保持grounding中的pred_idx和rule中的一致
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < r.length(); pred_idx++) {
            /* 在这一步把常量过滤掉 */
            final Predicate body_pred = r.getPredicate(pred_idx);
            class ConstPos {
                public final int argIdx;
                public final String constant;

                public ConstPos(int argIdx, String constant) {
                    this.argIdx = argIdx;
                    this.constant = constant;
                }
            }

            /* 找到常量位置 */
            final List<ConstPos> const_pos_list = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final Argument argument = body_pred.args[arg_idx];
                if (null != argument && !argument.isVar) {
                    const_pos_list.add(new ConstPos(arg_idx, argument.name));
                }
            }
            if (const_pos_list.isEmpty()) {
                init_grounding.add(new PredicateCache(body_pred, kb.getAllArgIndices(body_pred.functor)));
            } else {
                final Set<Predicate> filtered_predicates = new HashSet<>();
                for (Predicate p: kb.getAllFacts(body_pred.functor)) {
                    boolean match_all = true;
                    for (ConstPos pos: const_pos_list) {
                        if (!pos.constant.equals(p.args[pos.argIdx].name)) {
                            match_all = false;
                            break;
                        }
                    }
                    if (match_all) {
                        filtered_predicates.add(p);
                    }
                }
                init_grounding.add(new PredicateCache(body_pred, buildArgIndices(filtered_predicates)));
            }
        }
        groundings.add(init_grounding);
        final Map<Integer, Map<Integer, List<Integer>>> body_var_2_pred_loc_map = new HashMap<>();  // <vid, <pred idx, [arg idx]>>
        for (Map.Entry<Integer, List<PredArgPos>> entry: body_var_2_loc_map.entrySet()) {
            final int var_id = entry.getKey();
            final List<PredArgPos> var_poss = entry.getValue();
            final Map<Integer, List<Integer>> pred_idx_2_arg_idxs_map = body_var_2_pred_loc_map.computeIfAbsent(  // <pred idx, [arg idx]>
                    var_id, k -> new HashMap<>()
            );
            for (PredArgPos pos: var_poss) {
                final List<Integer> arg_idxs = pred_idx_2_arg_idxs_map.computeIfAbsent(
                        pos.predIdx, k -> new ArrayList<>()
                );
                arg_idxs.add(pos.argIdx);
            }
        }
        for (Map.Entry<Integer, Map<Integer, List<Integer>>> entry: body_var_2_pred_loc_map.entrySet()) {
            /* 对于每一个Body LV，求得其取值，并过滤不符合要求的选项 */
            final int var_id = entry.getKey();
            final Map<Integer, List<Integer>> var_pos_map = entry.getValue();
            final List<PredArgPos> var_occurrences = body_var_2_loc_map.get(var_id);
            if (1 < var_occurrences.size()) {  /* 不考虑BUGV的情况 */
                /* BLGV or BOLV */
                final ListIterator<List<PredicateCache>> itr = groundings.listIterator();
                while (itr.hasNext()) {
                    final List<PredicateCache> grounding = itr.next();
                    itr.remove();

                    /* 找到当前LV的取值范围（注意这里可能存在相同var在一个pred中的情况） */
                    final PredArgPos init_pos = var_occurrences.get(0);
                    Set<String> crossed_values = new HashSet<>(
                            grounding.get(init_pos.predIdx).indexedInclusion[init_pos.argIdx].keySet()
                    );
                    for (Map.Entry<Integer, List<Integer>> entry1: var_pos_map.entrySet()) {
                        final int pred_idx = entry1.getKey();
                        final List<Integer> arg_idxs = entry1.getValue();
                        if (1 < arg_idxs.size()) {
                            /* 这个var在一个pred中有多重出现 */
                            final Set<String> co_occur_values = new HashSet<>();
                            for (Set<Predicate> pred_set: grounding.get(pred_idx).indexedInclusion[0].values()) {
                                for (Predicate p: pred_set) {
                                    final String constant = p.args[arg_idxs.get(0)].name;
                                    boolean all_match = true;
                                    for (int i = 1; i < arg_idxs.size(); i++) {
                                        if (!constant.equals(p.args[arg_idxs.get(i)].name)) {
                                            all_match = false;
                                            break;
                                        }
                                    }
                                    if (all_match) {
                                        co_occur_values.add(constant);
                                    }
                                }
                            }
                            crossed_values = setIntersection(crossed_values, co_occur_values);
                        } else {
                            /* 这个var在当前pred中只出现一次 */
                            crossed_values = setIntersection(
                                    crossed_values,
                                    grounding.get(pred_idx).indexedInclusion[arg_idxs.get(0)].keySet()
                            );
                        }
                    }

                    /* 根据取值范围展开grounding（注意这里可能存在相同var在一个pred中的情况） */
                    for (String constant: crossed_values) {
                        final List<PredicateCache> new_grounding = dupGrounding(grounding);
                        final Constant const_arg = new Constant(Rule.CONSTANT_ARG_ID, constant);
                        for (Map.Entry<Integer, List<Integer>> entry1: var_pos_map.entrySet()) {
                            final int pred_idx = entry1.getKey();
                            final List<Integer> arg_idxs = entry1.getValue();
                            final PredicateCache pred_cache = new_grounding.get(pred_idx);

                            /* 设置对应常量值 */
                            for (int arg_idx: arg_idxs) {
                                pred_cache.predicate.args[arg_idx] = const_arg;
                            }

                            /* 设置对应索引 */
                            if (1 < arg_idxs.size()) {
                                final Set<Predicate> preds_with_co_occurrences = new HashSet<>();
                                for (Set<Predicate> pred_set: grounding.get(pred_idx).indexedInclusion[0].values()) {
                                    for (Predicate p : pred_set) {
                                        boolean all_match = true;
                                        for (int arg_idx : arg_idxs) {
                                            if (!constant.equals(p.args[arg_idx].name)) {
                                                all_match = false;
                                                break;
                                            }
                                        }
                                        if (all_match) {
                                            preds_with_co_occurrences.add(p);
                                        }
                                    }
                                }
                                pred_cache.indexedInclusion = buildArgIndices(preds_with_co_occurrences);
                            } else {
                                pred_cache.indexedInclusion = buildArgIndices(
                                        pred_cache.indexedInclusion[arg_idxs.get(0)].get(constant)
                                );
                            }
                        }
                        itr.add(new_grounding);
                    }
                }
            }
        }

        /* 找到HGV,HOV和BUGV的位置 */
        class BGVLinkInfo {
            final int bodyPredIdx;
            final int bodyArgIdx;
            final Integer[] headVarLocs;

            public BGVLinkInfo(int bodyPredIdx, int bodyArgIdx, Integer[] headVarLocs) {
                this.bodyPredIdx = bodyPredIdx;
                this.bodyArgIdx = bodyArgIdx;
                this.headVarLocs = headVarLocs;
            }
        }
        final List<List<Integer>> head_ov_pos_list = new ArrayList<>();
        final List<BGVLinkInfo> head_gv_pos_list = new ArrayList<>();
        final Map<Integer, List<BGVLinkInfo>> body_idx_2_ugv_pos_map = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry: head_var_2_loc_map.entrySet()) {
            final int head_var_id = entry.getKey();
            final List<Integer> head_var_pos_list = entry.getValue();
            final List<PredArgPos> body_var_pos_list = body_var_2_loc_map.get(head_var_id);
            if (null == body_var_pos_list) {
                /* Head OV */
                head_ov_pos_list.add(head_var_pos_list);
            } else if (1 >= body_var_pos_list.size()) {
                /* BUGV(注意这里可能存在多个BUGV在一个pred中的情况，以及一个BUGV对应Head中多个Arg的情况) */
                final PredArgPos body_pos = body_var_pos_list.get(0);
                final List<BGVLinkInfo> bugv_link_list = body_idx_2_ugv_pos_map.computeIfAbsent(
                        body_pos.predIdx, k -> new ArrayList<>()
                );
                bugv_link_list.add(new BGVLinkInfo(
                        body_pos.predIdx, body_pos.argIdx, head_var_pos_list.toArray(new Integer[0])
                ));
            } else {
                /* BLGV (HGV) (注意这里可能存在对应Head中多个Arg的情况) */
                final PredArgPos body_pos = body_var_pos_list.get(0);
                head_gv_pos_list.add(new BGVLinkInfo(
                        body_pos.predIdx, body_pos.argIdx, head_var_pos_list.toArray(new Integer[0])
                ));
            }
        }
        final Integer[][] head_ov_poss = new Integer[head_ov_pos_list.size()][];
        for (int i = 0; i < head_ov_pos_list.size(); i++) {
            head_ov_poss[i] = head_ov_pos_list.get(i).toArray(new Integer[0]);
        }

        /* 将HGV的取值迭代在Head中，并对所有HOV进行迭代 */
        final Set<Predicate> head_templates = new HashSet<>();
        if (body_idx_2_ugv_pos_map.isEmpty()) {
            for (final List<PredicateCache> grounding : groundings) {
                final Predicate head_template = new Predicate(head_pred);
                for (final BGVLinkInfo pos : head_gv_pos_list) {
                    final Predicate body_pred = grounding.get(pos.bodyPredIdx).predicate;
                    final Argument argument = body_pred.args[pos.bodyArgIdx];
                    for (int loc : pos.headVarLocs) {
                        head_template.args[loc] = argument;
                    }
                }
                head_templates.add(head_template);
            }
        } else {
            /* 按predicate组合Body FV */
            final Map.Entry<Integer, List<BGVLinkInfo>>[] body_idx_2_ugv_pos_entry_list =
                    body_idx_2_ugv_pos_map.entrySet().toArray(new Map.Entry[0]);

            for (final List<PredicateCache> grounding : groundings) {
                /* 给Body GV赋值 */
                final Predicate head_template = new Predicate(head_pred);
                for (final BGVLinkInfo pos : head_gv_pos_list) {
                    final Predicate body_pred = grounding.get(pos.bodyPredIdx).predicate;
                    final Argument argument = body_pred.args[pos.bodyArgIdx];
                    for (int loc : pos.headVarLocs) {
                        head_template.args[loc] = argument;
                    }
                }

                /* 加上Body Fv */
                /* Body FV 的取值范围不是全部constant，且要按照pred进行组合 */
                final Set<ComparableArray<String>>[] uv_within_pred_bindings =
                        new Set[body_idx_2_ugv_pos_entry_list.length];
                for (int i = 0; i < body_idx_2_ugv_pos_entry_list.length; i++) {
                    final Map.Entry<Integer, List<BGVLinkInfo>> entry = body_idx_2_ugv_pos_entry_list[i];
                    final int body_pred_idx = entry.getKey();
                    final List<BGVLinkInfo> bugv_links = entry.getValue();
                    final Set<ComparableArray<String>> values = new HashSet<>();
                    final PredicateCache pred_cache = grounding.get(body_pred_idx);
                    for (Set<Predicate> pred_set: pred_cache.indexedInclusion[0].values()) {
                        for (Predicate included_pred : pred_set) {
                            final String[] fv_within_pred_binding = new String[bugv_links.size()];
                            for (int j = 0; j < fv_within_pred_binding.length; j++) {
                                fv_within_pred_binding[j] = included_pred.args[bugv_links.get(j).bodyArgIdx].name;
                            }
                            values.add(new ComparableArray<>(fv_within_pred_binding));
                        }
                    }
                    uv_within_pred_bindings[i] = values;
                }
                final Set<ComparableArray<ComparableArray<String>>> fv_bindings = new HashSet<>();
                addBodyFvBindings(fv_bindings, uv_within_pred_bindings);
                for (ComparableArray<ComparableArray<String>> fv_binding: fv_bindings) {
                    for (int i = 0; i < body_idx_2_ugv_pos_entry_list.length; i++) {
                        final Map.Entry<Integer, List<BGVLinkInfo>> entry =
                                body_idx_2_ugv_pos_entry_list[i];
                        final ComparableArray<String> fv_value_combination = fv_binding.arr[i];
                        final List<BGVLinkInfo> bugv_links = entry.getValue();
                        for (int j = 0; j < fv_value_combination.arr.length; j++) {
                            final BGVLinkInfo bugv_link = bugv_links.get(j);
                            for (int head_arg_idx: bugv_link.headVarLocs) {
                                head_template.args[head_arg_idx] = new Constant(
                                        Rule.CONSTANT_ARG_ID, fv_value_combination.arr[j]
                                );
                            }
                        }
                    }
                    head_templates.add(new Predicate(head_template));
                }
            }
        }

        int original_size = recoveredFacts.size();
        if (head_ov_pos_list.isEmpty()) {
            /* 不需要替换变量 */
//            recoveredFacts.addAll(head_templates);
            for (Predicate fact: head_templates) {
                if (!counterExamples.contains(fact) && kb.addFact(fact)) {
                    recoveredFacts.add(fact);
                }
            }
        } else {
            /* 需要替换head中的变量 */
            final Set<Predicate> inferred_facts = new HashSet<>();
            for (Predicate head_template: head_templates) {
                iterate4Facts(inferred_facts, head_template, 0, head_ov_poss);
            }
            for (Predicate fact: inferred_facts) {
                if (!counterExamples.contains(fact) && kb.addFact(fact)) {
                    recoveredFacts.add(fact);
                }
            }
        }
        return recoveredFacts.size() - original_size;
    }

    protected Map<String, Set<Predicate>>[] buildArgIndices(Set<Predicate> predicates) {
        final Map<String, Set<Predicate>>[] arg_indices = new Map[predicates.iterator().next().arity()];
        for (int arg_idx = 0; arg_idx < arg_indices.length; arg_idx++) {
            arg_indices[arg_idx] = new HashMap<>();
        }
        for (Predicate p: predicates) {
            for (int arg_idx = 0; arg_idx < p.arity(); arg_idx++) {
                final Set<Predicate> pred_set = arg_indices[arg_idx].computeIfAbsent(
                        p.args[arg_idx].name, k -> new HashSet<>()
                );
                pred_set.add(p);
            }
        }
        return arg_indices;
    }

    protected <T> Set<T> setIntersection(Set<T> set1, Set<T> set2) {
        final Set<T> intersection = new HashSet<>();
        final Set<T> comparing_set, compared_set;
        if (set1.size() <= set2.size()) {
            comparing_set = set1;
            compared_set = set2;
        } else {
            comparing_set = set2;
            compared_set = set1;
        }
        for (T t: comparing_set) {
            if (compared_set.contains(t)) {
                intersection.add(t);
            }
        }
        return intersection;
    }

    protected List<PredicateCache> dupGrounding(List<PredicateCache> grounding) {
        final List<PredicateCache> new_grounding = new ArrayList<>(grounding.size());
        final Iterator<PredicateCache> itr = grounding.listIterator();
        new_grounding.add(itr.next());  // add first null
        while (itr.hasNext()) {
            new_grounding.add(new PredicateCache(itr.next()));
        }
        return new_grounding;
    }

    protected void iterate4Facts(Set<Predicate> factSet, Predicate template, int idx, Integer[][] varLocs) {
        final Integer[] locations = varLocs[idx];
        if (idx < varLocs.length - 1) {
            /* 递归 */
            for (String constant_symbol: constants) {
                final Constant constant = new Constant(Rule.CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                iterate4Facts(
                        factSet, template, idx + 1, varLocs
                );
            }
        } else {
            /* 已经到了最后的位置，不递归 */
            for (String constant_symbol: constants) {
                final Constant constant = new Constant(Rule.CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                factSet.add(new Predicate(template));
            }
        }
    }

    private void addBodyFvBindings(
            Set<ComparableArray<ComparableArray<String>>> bindings,
            Set<ComparableArray<String>>[] values
    ) {
        addBodyFvBindingsHandler(
                bindings,
                values,
                new ComparableArray<ComparableArray<String>>(new ComparableArray[values.length]),
                0
        );
    }

    private void addBodyFvBindingsHandler(
            Set<ComparableArray<ComparableArray<String>>> bindings,
            Set<ComparableArray<String>>[] values,
            ComparableArray<ComparableArray<String>> template,
            int idx
    ) {
        final Set<ComparableArray<String>> value_set = values[idx];
        if (idx == values.length - 1) {
            for (ComparableArray<String> value: value_set) {
                template.arr[idx] = value;
                bindings.add(new ComparableArray<>(template));
            }
        } else {
            for (ComparableArray<String> value: value_set) {
                template.arr[idx] = value;
                addBodyFvBindingsHandler(
                        bindings, values, template, idx + 1
                );
            }
        }
    }
}
