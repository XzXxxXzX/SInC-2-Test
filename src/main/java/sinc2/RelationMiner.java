package sinc2;

import sinc2.common.ArgLocation;
import sinc2.common.Argument;
import sinc2.common.InterruptedSignal;
import sinc2.common.Predicate;
import sinc2.kb.KbException;
import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;
import sinc2.kb.Record;
import sinc2.rule.*;
import sinc2.util.MultiSet;
import sinc2.util.graph.GraphNode;

import java.io.PrintWriter;
import java.util.*;

/**
 * A relation miner is used to induce logic rules that compress a single relation in a KB.
 *
 * @since 2.0
 */
public abstract class RelationMiner {
    /** The input KB */
    protected final NumeratedKb kb;
    /** The target relation numeration */
    protected final int targetRelation;
    /** The rule evaluation metric */
    protected final EvalMetric evalMetric;
    /** The beamwidth used in the rule mining procedure */
    protected final int beamwidth;
    /** The stopping compression ratio for inducing a single rule */
    protected final double stopCompressionRatio;
    /** A mapping from predicates to the nodes in the dependency graph */
    protected final Map<Predicate, GraphNode<Predicate>> predicate2NodeMap;
    /** The dependency graph, in the form of an adjacent list */
    protected final Map<GraphNode<Predicate>, Set<GraphNode<Predicate>>> dependencyGraph;
    /** The hypothesis set, i.e., a list of rules */
    protected final List<Rule> hypothesis = new ArrayList<>();
    /** The set of counterexamples */
    protected final Set<Record> counterexamples = new HashSet<>();
    /** The tabu set */
    protected final Map<MultiSet<Integer>, Set<Fingerprint>> tabuSet = new HashMap<>();

    /** Logger */
    protected final PrintWriter logger;

    /**
     * Construct by passing parameters from the compressor that loads the data.
     *
     * @param kb The input KB
     * @param targetRelation The target relation in the KB
     * @param evalMetric The rule evaluation metric
     * @param beamwidth The beamwidth used in the rule mining procedure
     * @param stopCompressionRatio The stopping compression ratio for inducing a single rule
     * @param predicate2NodeMap The mapping from predicates to the nodes in the dependency graph
     * @param dependencyGraph The dependency graph
     * @param logger A logger
     */
    public RelationMiner(
            NumeratedKb kb, int targetRelation, EvalMetric evalMetric, int beamwidth, double stopCompressionRatio,
            Map<Predicate, GraphNode<Predicate>> predicate2NodeMap,
            Map<GraphNode<Predicate>, Set<GraphNode<Predicate>>> dependencyGraph,
            PrintWriter logger
    ) {
        this.kb = kb;
        this.targetRelation = targetRelation;
        this.evalMetric = evalMetric;
        this.beamwidth = beamwidth;
        this.stopCompressionRatio = stopCompressionRatio;
        this.predicate2NodeMap = predicate2NodeMap;
        this.dependencyGraph = dependencyGraph;
        this.logger = logger;
    }

    /**
     * Create the starting rule at the beginning of a rule mining procedure.
     */
    abstract protected Rule getStartRule();

    /**
     * The rule mining procedure that finds a single rule in the target relation.
     *
     * @return The rule that can be used to compress the target relation. NULL if no proper rule can be found.
     */
    protected Rule findRule() {
        /* Create the beams */
        Rule[] beams = new Rule[beamwidth];
        beams[0] = getStartRule();
        Rule best_local_optimum = null;

        /* Find a local optimum (there is certainly a local optimum in the search routine) */
        while (true) {
            /* Find the candidates in the next round according to current beams */
            Rule[] best_candidates = new Rule[beamwidth];
            try {
                for (int i = 0; i < beamwidth && null != beams[i]; i++) {
                    Rule r = beams[i];
                    selectAsBeam(r);
                    logger.printf("Extend: %s\n", r.toDumpString(kb.getNumerationMap()));
                    logger.flush();

                    /* Find the specializations and generalizations of rule 'r' */
                    int specializations_cnt = findSpecializations(r, best_candidates);
                    int generalizations_cnt = findGeneralizations(r, best_candidates);
                    if (0 == specializations_cnt && 0 == generalizations_cnt) {
                        /* If no better specialized and generalized rules, 'r' is a local optimum */
                        /* Keep track of only the best local optimum */
                        if (null == best_local_optimum ||
                                best_local_optimum.getEval().value(evalMetric) < r.getEval().value(evalMetric)) {
                            best_local_optimum = r;
                        }
                    }
                }
            } catch (InterruptedSignal e) {
                /* Stop the finding procedure at the current stage and return the best rule */
                Rule best_rule = beams[0];
                for (int i = 1; i < beamwidth && null != beams[i]; i++) {
                    if (best_rule.getEval().value(evalMetric) < beams[i].getEval().value(evalMetric)) {
                        best_rule = beams[i];
                    }
                }
                for (int i = 0; i < beamwidth && null != best_candidates[i]; i++) {
                    if (best_rule.getEval().value(evalMetric) < best_candidates[i].getEval().value(evalMetric)) {
                        best_rule = best_candidates[i];
                    }
                }
                return (null != best_rule) ? (best_rule.getEval().useful() ? best_rule : null) : null;
            }

            /* Find the best candidate */
            Rule best_candidate = null;
            if (null != best_candidates[0]) {
                best_candidate = best_candidates[0];
                for (int i = 1; i < beamwidth && null != best_candidates[i]; i++) {
                    if (best_candidate.getEval().value(evalMetric) < best_candidates[i].getEval().value(evalMetric)) {
                        best_candidate = best_candidates[i];
                    }
                }
            }

            /* If there is a local optimum and it is the best among all, return the rule */
            if (null != best_local_optimum &&
                    (null == best_candidate ||
                            best_local_optimum.getEval().value(evalMetric) > best_candidate.getEval().value(evalMetric))
            ) {
                /* If the best is not useful, return NULL */
                return best_local_optimum.getEval().useful() ? best_local_optimum : null;
            }

            /* If the best rule reaches the stopping threshold, return the rule */
            /* The "best_candidate" is certainly not NULL if the workflow goes here */
            /* Assumption: the stopping threshold is no less than the threshold of usefulness */
            Eval best_eval = best_candidate.getEval();
            if (stopCompressionRatio <= best_eval.value(EvalMetric.CompressionRatio) || 0 == best_eval.getNegEtls()) {
                return best_candidate;
            }

            /* Update the beams */
            beams = best_candidates;
        }
    }

    /**
     * Find the specializations of a base rule. Only the specializations that have a better quality score is added to the
     * candidate list. The candidate list always keeps the best rules.
     *
     * @param rule The basic rule
     * @param candidates The candidate list
     * @return The number of added candidates
     * @throws InterruptedSignal Thrown when the workflow should be interrupted
     */
    protected int findSpecializations(final Rule rule, final Rule[] candidates) throws InterruptedSignal {
        int added_candidate_cnt = 0;

        /* Find all empty arguments */
        List<ArgLocation> empty_args = new ArrayList<>();
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.predicates(); pred_idx++) {
            final Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (Argument.isEmpty(predicate.args[arg_idx])) {
                    empty_args.add(new ArgLocation(pred_idx, arg_idx));
                }
            }
        }

        /* Add existing LVs (case 1 and 2) */
        final Collection<KbRelation> relations = kb.getRelations();
        for (int var_id = 0; var_id < rule.usedLimitedVars(); var_id++) {
            /* Case 1 */
            for (ArgLocation vacant: empty_args) {
                final Rule new_rule = rule.clone();
                final UpdateStatus update_status = new_rule.cvt1Uv2ExtLv(vacant.predIdx, vacant.argIdx, var_id);
                added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
            }

            /* Case 2 */
            for (KbRelation relation: relations) {
                for (int arg_idx = 0; arg_idx < relation.getArity(); arg_idx++) {
                    final Rule new_rule = rule.clone();
                    final UpdateStatus update_status = new_rule.cvt1Uv2ExtLv(
                            relation.getNumeration(), relation.getArity(), arg_idx, var_id
                    );
                    added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
        }

        /* Case 3, 4, and 5 */
        for (int i = 0; i < empty_args.size(); i++) {
            /* Find the first empty argument */
            final ArgLocation empty_arg_loc_1 = empty_args.get(i);
            final Predicate predicate1 = rule.getPredicate(empty_arg_loc_1.predIdx);

            /* Case 5 */
            final int[] const_list = kb.getPromisingConstants(predicate1.functor)[empty_arg_loc_1.argIdx];
            for (int constant: const_list) {
                final Rule new_rule = rule.clone();
                final UpdateStatus update_status = new_rule.cvt1Uv2Const(
                        empty_arg_loc_1.predIdx, empty_arg_loc_1.argIdx, constant
                );
                added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
            }

            /* Case 3 */
            for (int j = i + 1; j < empty_args.size(); j++) {
                /* Find another empty argument */
                final ArgLocation empty_arg_loc_2 = empty_args.get(j);
                final Rule new_rule = rule.clone();
                final UpdateStatus update_status = new_rule.cvt2Uvs2NewLv(
                        empty_arg_loc_1.predIdx, empty_arg_loc_1.argIdx, empty_arg_loc_2.predIdx, empty_arg_loc_2.argIdx
                );
                added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
            }

            /* Case 4 */
            for (KbRelation relation: relations) {
                for (int arg_idx = 0; arg_idx < relation.getArity(); arg_idx++) {
                    final Rule new_rule = rule.clone();
                    final UpdateStatus update_status = new_rule.cvt2Uvs2NewLv(
                            relation.getNumeration(), relation.getArity(), arg_idx, empty_arg_loc_1.predIdx, empty_arg_loc_1.argIdx
                    );
                    added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
        }
        return added_candidate_cnt;
    }

    /**
     * Find the generalizations of a basic rule. Only the specializations that have a better quality score is added to the
     * candidate list. The candidate list always keeps the best rules.
     *
     * @param rule The original rule
     * @param candidates The candidate list
     * @return The number of added candidates
     * @throws InterruptedSignal Thrown when the workflow should be interrupted
     */
    protected int findGeneralizations(final Rule rule, final Rule[] candidates) throws InterruptedSignal {
        int added_candidate_cnt = 0;
        for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.predicates(); pred_idx++) {
            /* Independent fragment may appear in a generalized rule, but this will be found by checking rule validness */
            final Predicate predicate = rule.getPredicate(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                if (Argument.isNonEmpty(predicate.args[arg_idx])) {
                    final Rule new_rule = rule.clone();
                    final UpdateStatus update_status = new_rule.rmAssignedArg(pred_idx, arg_idx);
                    added_candidate_cnt += checkThenAddRule(update_status, new_rule, rule, candidates);
                }
            }
        }
        return added_candidate_cnt;
    }

    /**
     * Check the status of updated rule and add to a candidate list if the update is successful and the evaluation score
     * of the updated rule is higher than the original one. The candidate list always keeps the best rules.
     *
     * @param updateStatus The update status
     * @param updatedRule The updated rule
     * @param originalRule The original rule
     * @param candidates The candidate list
     * @return 1 if the update is successful and the updated rule is better than the original one; 0 otherwise.
     * @throws InterruptedSignal Thrown when the workflow should be interrupted
     */
    protected int checkThenAddRule(
            UpdateStatus updateStatus, Rule updatedRule, Rule originalRule, Rule[] candidates
    ) throws InterruptedSignal {
        boolean updated_is_better = false;
        switch (updateStatus) {
            case NORMAL:
                if (updatedRule.getEval().value(evalMetric) > originalRule.getEval().value(evalMetric)) {
                    updated_is_better = true;
                    if (null == candidates[0]) {
                        candidates[0] = updatedRule;
                    } else {
                        int worst_candidate_idx = 0;
                        double lowest_score = candidates[0].getEval().value(evalMetric);
                        for (int i = 1; i < candidates.length; i++) {
                            if (null == candidates[i]) {
                                worst_candidate_idx = i;
                                break;
                            }
                            double candidate_socre = candidates[i].getEval().value(evalMetric);
                            if (lowest_score > candidate_socre) {
                                worst_candidate_idx = i;
                                lowest_score = candidate_socre;
                            }
                        }
                        candidates[worst_candidate_idx] = updatedRule;
                    }
                }
                break;
            case INVALID:
            case DUPLICATED:
            case INSUFFICIENT_COVERAGE:
            case TABU_PRUNED:
                break;
            default:
                throw new Error("Unknown Update Status of Rule: " + updateStatus.name());
        }
        if (SInC.interrupted) {
            /* Interruption triggers here */
            throw new InterruptedSignal("Interrupted");
        }
        return updated_is_better ? 1 : 0;
    }

    /**
     * Select rule r as one of the beams in the next iteration of rule mining. Shared operations for beams may be added
     * in this method, e.g., updating the cache indices.
     */
    abstract protected void selectAsBeam(Rule r);

    /**
     * Find the positive and negative entailments of the rule. Label the positive entailments and add the negative ones
     * to the counterexample set. Evidence for the positive entailments are also needed to update the dependency graph.
     *
     * @throws KbException When KB operation fails
     */
    protected void updateKbAndDependencyGraph(Rule rule) throws KbException {
        counterexamples.addAll(rule.getCounterexamples());
        EvidenceBatch evidence_batch = rule.getEvidenceAndMarkEntailment();
        for (int[][] grounding: evidence_batch.evidenceList) {
            final Predicate head_pred = new Predicate(
                    evidence_batch.relationsInRule[Rule.HEAD_PRED_IDX], grounding[Rule.HEAD_PRED_IDX]
            );
            final GraphNode<Predicate> head_node = predicate2NodeMap.computeIfAbsent(
                    head_pred, k -> new GraphNode<>(head_pred)
            );
            dependencyGraph.compute(head_node, (h, dependencies) -> {
                if (null == dependencies) {
                    dependencies = new HashSet<>();
                }
                if (1 >= grounding.length) {
                    /* dependency is the "⊥" node */
                    dependencies.add(SInC.AXIOM_NODE);
                } else {
                    for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < grounding.length; pred_idx++) {
                        final Predicate body_pred = new Predicate(
                                evidence_batch.relationsInRule[pred_idx], grounding[pred_idx]
                        );
                        final GraphNode<Predicate> body_node = predicate2NodeMap.computeIfAbsent(
                                body_pred, kk -> new GraphNode<>(body_pred)
                        );
                        dependencies.add(body_node);
                    }
                }
                return dependencies;
            });
        }
    }

    /**
     * Find rules and compress the target relation.
     *
     * @throws KbException When KB operation fails
     */
    public void run() throws KbException {
        Rule rule;
        while (!SInC.interrupted && (null != (rule = findRule()))) {
            logger.printf("Found: %s\n", rule.toDumpString(kb.getNumerationMap()));
            hypothesis.add(rule);
            updateKbAndDependencyGraph(rule);
        }
    }

    public Set<Record> getCounterexamples() {
        return counterexamples;
    }

    public List<Rule> getHypothesis() {
        return hypothesis;
    }
}
