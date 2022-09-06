package sinc.impl.cached.spec;

import sinc.common.*;
import sinc.impl.cached.CachedQueryMonitor;
import sinc.impl.cached.CachedRule;
import sinc.impl.cached.MemKB;

import java.util.*;

public class SpecificCachedRule extends CachedRule {
    /* 记录符合条件的grounding的中间结果 */
    private static class PredicateCache {
        public final Predicate predicate;
        public Set<Predicate> inclusion;  /* 对这个Set的操作仅限于读取以及替换，不要向其中添加或删除元素，
                                             这样可以做到copy on write */

        public PredicateCache(Predicate predicate) {
            this.predicate = predicate;
            this.inclusion = new HashSet<>();
        }

        public PredicateCache(PredicateCache another) {
            this.predicate = new Predicate(another.predicate);
            this.inclusion = another.inclusion;  // copy on write
        }
    }
    private final MemKB kb;
    private List<List<PredicateCache>> groundings = new LinkedList<>();
    private List<List<PredicateCache>> groundingsBody = new LinkedList<>();

    public SpecificCachedRule(String headFunctor, Set<RuleFingerPrint> cache, MemKB kb) {
        super(headFunctor, kb.getArity(headFunctor), cache);
        this.kb = kb;

        /* 把无BV的head加入 */
        final PredicateCache head_cache = new PredicateCache(new Predicate(headFunctor, getHead().arity()));
        head_cache.inclusion = kb.getAllFacts(headFunctor);
        final List<PredicateCache> grounding = new ArrayList<>();
        grounding.add(head_cache);
        groundings.add(grounding);

        final List<PredicateCache> grounding_body = new ArrayList<>();
        grounding_body.add(null);  // 保持两种cache的index一致
        groundingsBody.add(grounding_body);

        this.eval = calculateEval();
    }

    public SpecificCachedRule(SpecificCachedRule another) {
        super(another);
        this.kb = another.kb;
        for (final List<PredicateCache> grounding: another.groundings) {
            this.groundings.add(dupGrounding(grounding, false));
        }
        for (final List<PredicateCache> grounding_body: another.groundingsBody) {
            this.groundingsBody.add(dupGrounding(grounding_body, true));
        }
    }

    @Override
    public Rule clone() {
        final long time_start = System.nanoTime();
        Rule r =  new SpecificCachedRule(this);
        final long time_done = System.nanoTime();
        cacheMonitor.cloneCostInNano += time_done - time_start;
        return r;
    }

    @Override
    protected UpdateStatus boundFreeVar2ExistingVarHandler(int predIdx, int argIdx, int varId) {
        final long time_start = System.nanoTime();
        boundFreeVar2ExistingVarUpdateCache(predIdx, argIdx, varId, false);

        if (MIN_FACT_COVERAGE >= factCoverage()) {
            final long time_done = System.nanoTime();
            cacheMonitor.boundExistVarCostInNano += time_done - time_start;
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }

        boundFreeVar2ExistingVarUpdateCache(predIdx, argIdx, varId, true);
        final long time_done = System.nanoTime();
        cacheMonitor.boundExistVarCostInNano += time_done - time_start;
        return UpdateStatus.NORMAL;
    }

    /**
     * 在对应的情况下更新Cache
     *
     * @param bodyOnly 更新的目标是否仅对于body
     */
    private void boundFreeVar2ExistingVarUpdateCache(
            final int predIdx, final int argIdx, final int varId, boolean bodyOnly
    ) {
        final int pred_idx_start;
        final List<List<PredicateCache>> grounding_list;
        if (bodyOnly) {
            if (HEAD_PRED_IDX == predIdx) {
                /* 修改不涉及body的时候，body的cache不需要更新 */
                return;
            }
            pred_idx_start = FIRST_BODY_PRED_IDX;
            grounding_list = groundingsBody;
        } else {
            pred_idx_start = HEAD_PRED_IDX;
            grounding_list = groundings;
        }

        boolean found = false;
        final ListIterator<List<PredicateCache>> grounding_itr = grounding_list.listIterator();
        for (int pred_idx = pred_idx_start; pred_idx < structure.size() && !found; pred_idx++) {
            final Predicate predicate = structure.get(pred_idx);

            /* 找到一个predicate对应当前BV的一个arg的位置 */
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar && varId == argument.id &&
                        (pred_idx != predIdx || arg_idx != argIdx)) {  // 不要和刚设置的变量比较
                    found = true;

                    /* 根据当前pred和新绑定的参数列过滤grounding */
                    while (grounding_itr.hasNext()) {
                        final List<PredicateCache> grounding = grounding_itr.next();
                        final PredicateCache compared_pred_cache = grounding.get(pred_idx);
                        final Argument compared_argument = compared_pred_cache.predicate.args[arg_idx];
                        final PredicateCache target_pred_cache = grounding.get(predIdx);
                        final Set<Predicate> filtered_predicates = new HashSet<>();
                        for (Predicate fv_pred: target_pred_cache.inclusion) {
                            final Argument fv_arg = fv_pred.args[argIdx];
                            if (compared_argument.name.equals(fv_arg.name)) {
                                filtered_predicates.add(fv_pred);
                            }
                        }

                        if (filtered_predicates.isEmpty()) {
                            /* 如果过滤之后FV集合为空，那么说明当前的grounding不能用 */
                            grounding_itr.remove();
                        } else {
                            /* 如果当前grounding仍然满足要求，则更新对应参数 */
                            target_pred_cache.predicate.args[argIdx] = compared_argument;
                            target_pred_cache.inclusion = filtered_predicates;  // copy on write
                        }
                    }
                    break;
                }
            }
        }

        if (bodyOnly && !found) {
            /* 如果body中没有找到其他相同的BV，那么目标参数对应的列应该按照所有值展开inclusion并设置对应的值 */
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();
                final PredicateCache target_pred_cache = grounding.get(predIdx);

                /* 按目标列的值划分inclusion */
                final Map<String, Set<Predicate>> inclusion_map = new HashMap<>();
                for (Predicate predicate: target_pred_cache.inclusion) {
                    inclusion_map.compute(predicate.args[argIdx].name, (constant, set) -> {
                        if (null == set) {
                            set = new HashSet<>();
                        }
                        set.add(predicate);
                        return set;
                    });
                }

                /* 展开grounding */
                if (1 == inclusion_map.size()) {
                    /* 目标参数处只有一个值，直接修改grounding中对应参数即可 */
                    target_pred_cache.predicate.args[argIdx] = new Constant(
                            CONSTANT_ARG_ID, inclusion_map.keySet().iterator().next()
                    );
                } else {
                    /* 用多个grounding替代原有grounding */
                    grounding_itr.remove();
                    for (Map.Entry<String, Set<Predicate>> entry: inclusion_map.entrySet()) {
                        final List<PredicateCache> new_grounding = dupGrounding(grounding, true);
                        final PredicateCache new_target_pred_cache = new_grounding.get(predIdx);
                        new_target_pred_cache.predicate.args[argIdx] = new Constant(CONSTANT_ARG_ID, entry.getKey());
                        new_target_pred_cache.inclusion = entry.getValue();
                        grounding_itr.add(new_grounding);
                    }
                }
            }
        }
    }

    @Override
    protected UpdateStatus boundFreeVar2ExistingVarHandler(Predicate newPredicate, int argIdx, int varId) {
        final long time_start = System.nanoTime();
        boundFreeVar2ExistingVarUpdateCache(newPredicate, argIdx, varId, false);

        if (MIN_FACT_COVERAGE >= factCoverage()) {
            final long time_done = System.nanoTime();
            cacheMonitor.boundExistVarInNewPredCostInNano += time_done - time_start;
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }

        boundFreeVar2ExistingVarUpdateCache(newPredicate, argIdx, varId, true);
        final long time_done = System.nanoTime();
        cacheMonitor.boundExistVarInNewPredCostInNano += time_done - time_start;
        return UpdateStatus.NORMAL;
    }

    /**
     * 在对应的情况下更新Cache
     *
     * @param bodyOnly 更新的目标是否仅对于body
     */
    private void boundFreeVar2ExistingVarUpdateCache(
            final Predicate newPredicate, final int argIdx, final int varId, boolean bodyOnly
    ) {
        final int pred_idx_start;
        final List<List<PredicateCache>> grounding_list;
        if (bodyOnly) {
            /* 修改肯定在body内，不需要额外判断 */
            pred_idx_start = FIRST_BODY_PRED_IDX;
            grounding_list = groundingsBody;
        } else {
            pred_idx_start = HEAD_PRED_IDX;
            grounding_list = groundings;
        }

        final Map<String, Set<Predicate>> arg_indices_map = kb.getArgIndices(newPredicate.functor, argIdx);
        boolean found = false;
        final ListIterator<List<PredicateCache>> grounding_itr = grounding_list.listIterator();
        for (int pred_idx = pred_idx_start; pred_idx < structure.size() - 1 && !found; pred_idx++) {  // 不要和刚设置的变量比较
            final Predicate predicate = structure.get(pred_idx);

            /* 找到一个predicate对应当前BV的一个arg的位置 */
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                final Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar && varId == argument.id) {
                    found = true;

                    /* 根据当前pred过滤grounding */
                    while (grounding_itr.hasNext()) {
                        final List<PredicateCache> grounding = grounding_itr.next();
                        final PredicateCache compared_pred_cache = grounding.get(pred_idx);
                        final Argument compared_argument = compared_pred_cache.predicate.args[arg_idx];
                        final Set<Predicate> inclusion = arg_indices_map.get(compared_argument.name);

                        if (null == inclusion) {
                            /* 对应变量在新参数中没有，删除grounding */
                            grounding_itr.remove();
                        } else {
                            /* 将对应的值添加在grounding末尾 */
                            final PredicateCache new_pred_cache = new PredicateCache(
                                    new Predicate(newPredicate.functor, newPredicate.arity())
                            );
                            new_pred_cache.predicate.args[argIdx] = compared_argument;
                            new_pred_cache.inclusion = inclusion;  // copy on write
                            grounding.add(new_pred_cache);
                        }
                    }
                    break;
                }
            }
        }

        if (bodyOnly && !found) {
            /* 如果在body中没有找到其他相同的BV，那么就根据所有值展开grounding */
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();
                grounding_itr.remove();
                for (Map.Entry<String, Set<Predicate>> entry: arg_indices_map.entrySet()) {
                    final List<PredicateCache> new_grounding = dupGrounding(grounding, true);
                    final PredicateCache new_pred_cache = new PredicateCache(
                            new Predicate(newPredicate.functor, newPredicate.arity())
                    );
                    new_pred_cache.predicate.args[argIdx] = new Constant(CONSTANT_ARG_ID, entry.getKey());
                    new_pred_cache.inclusion.addAll(entry.getValue());
                    new_grounding.add(new_pred_cache);
                    grounding_itr.add(new_grounding);
                }
            }
        }
    }

    @Override
    protected UpdateStatus boundFreeVars2NewVarHandler(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        final long time_start = System.nanoTime();
        boundFreeVars2NewVarUpdateCache(predIdx1, argIdx1, predIdx2, argIdx2, false);

        if (MIN_FACT_COVERAGE >= factCoverage()) {
            final long time_done = System.nanoTime();
            cacheMonitor.boundNewVarCostInNano += time_done - time_start;
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }

        boundFreeVars2NewVarUpdateCache(predIdx1, argIdx1, predIdx2, argIdx2, true);
        final long time_done = System.nanoTime();
        cacheMonitor.boundNewVarCostInNano += time_done - time_start;
        return UpdateStatus.NORMAL;
    }

    /**
     * 在对应的情况下更新Cache
     *
     * @param bodyOnly 更新的目标是否仅对于body
     */
    private void boundFreeVars2NewVarUpdateCache(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2, final boolean bodyOnly
    ) {
        final List<List<PredicateCache>> grounding_list;
        if (bodyOnly) {
            if (HEAD_PRED_IDX == predIdx1 && HEAD_PRED_IDX == predIdx2) {
                /* 修改不涉及body的时候，body的cache不需要更新 */
                return;
            }
            grounding_list = groundingsBody;
        } else {
            grounding_list = groundings;
        }

        ListIterator<List<PredicateCache>> grounding_itr = grounding_list.listIterator();
        if (predIdx1 == predIdx2) {
            /* 在一张表内进行过滤 */
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();
                final PredicateCache target_pred_cache = grounding.get(predIdx1);
                final Map<String, Set<Predicate>> inclusion_map = new HashMap<>();
                for (Predicate predicate: target_pred_cache.inclusion) {
                    final Argument argument1 = predicate.args[argIdx1];
                    final Argument argument2 = predicate.args[argIdx2];
                    if (argument1.name.equals(argument2.name)) {
                        inclusion_map.compute(argument1.name, (constant, set) -> {
                            if (null == set) {
                                set = new HashSet<>();
                            }
                            set.add(predicate);
                            return set;
                        });
                    }
                }

                /* 展开原有grounding */
                grounding_itr.remove();
                for (Map.Entry<String, Set<Predicate>> entry: inclusion_map.entrySet()) {
                    final List<PredicateCache> new_grounding = dupGrounding(grounding, bodyOnly);
                    final PredicateCache new_target_pred_cache = new_grounding.get(predIdx1);
                    final Constant constant = new Constant(CONSTANT_ARG_ID, entry.getKey());
                    new_target_pred_cache.predicate.args[argIdx1] = constant;
                    new_target_pred_cache.predicate.args[argIdx2] = constant;
                    grounding_itr.add(new_grounding);
                }
            }
        } else {
            if (!bodyOnly || (HEAD_PRED_IDX != predIdx1 && HEAD_PRED_IDX != predIdx2)) {
                /* 两张表一起过滤 */
                while (grounding_itr.hasNext()) {
                    final List<PredicateCache> grounding = grounding_itr.next();

                    /* 分别找出参数常量值范围 */
                    final PredicateCache target_pred_cache1 = grounding.get(predIdx1);
                    final Map<String, Set<Predicate>> inclusion_map1 = new HashMap<>();
                    for (Predicate predicate : target_pred_cache1.inclusion) {
                        final Argument argument1 = predicate.args[argIdx1];
                        inclusion_map1.compute(argument1.name, (constant, set) -> {
                            if (null == set) {
                                set = new HashSet<>();
                            }
                            set.add(predicate);
                            return set;
                        });
                    }

                    final PredicateCache target_pred_cache2 = grounding.get(predIdx2);
                    final Map<String, Set<Predicate>> inclusion_map2 = new HashMap<>();
                    for (Predicate predicate : target_pred_cache2.inclusion) {
                        final Argument argument2 = predicate.args[argIdx2];
                        inclusion_map2.compute(argument2.name, (constant, set) -> {
                            if (null == set) {
                                set = new HashSet<>();
                            }
                            set.add(predicate);
                            return set;
                        });
                    }

                    /* 做交叉 */
                    final int comparing_pred_idx;
                    final int comparing_arg_idx;
                    final Map<String, Set<Predicate>> comparing_map;
                    final int compared_pred_idx;
                    final int compared_arg_idx;
                    final Map<String, Set<Predicate>> compared_map;
                    if (inclusion_map1.size() <= inclusion_map2.size()) {
                        comparing_pred_idx = predIdx1;
                        comparing_arg_idx = argIdx1;
                        comparing_map = inclusion_map1;
                        compared_pred_idx = predIdx2;
                        compared_arg_idx = argIdx2;
                        compared_map = inclusion_map2;
                    } else {
                        comparing_pred_idx = predIdx2;
                        comparing_arg_idx = argIdx2;
                        comparing_map = inclusion_map2;
                        compared_pred_idx = predIdx1;
                        compared_arg_idx = argIdx1;
                        compared_map = inclusion_map1;
                    }
                    grounding_itr.remove();
                    for (Map.Entry<String, Set<Predicate>> entry : comparing_map.entrySet()) {
                        final String constant_symbol = entry.getKey();
                        final Set<Predicate> compared_inclusion = compared_map.get(constant_symbol);
                        if (null != compared_inclusion) {
                            final Set<Predicate> comparing_inclusion = entry.getValue();
                            final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);

                            final List<PredicateCache> new_grounding = dupGrounding(grounding, bodyOnly);
                            final PredicateCache comparing_pred_cache = new_grounding.get(comparing_pred_idx);
                            comparing_pred_cache.predicate.args[comparing_arg_idx] = constant;
                            comparing_pred_cache.inclusion = comparing_inclusion;

                            final PredicateCache compared_pred_cache = new_grounding.get(compared_pred_idx);
                            compared_pred_cache.predicate.args[compared_arg_idx] = constant;
                            compared_pred_cache.inclusion = compared_inclusion;

                            grounding_itr.add(new_grounding);
                        }
                    }
                }
            } else {
                /* bodyOnly且只有一个predIdx在body中 */
                /* 展开 */
                final int pred_idx;
                final int arg_idx;
                if (HEAD_PRED_IDX == predIdx1) {
                    pred_idx = predIdx2;
                    arg_idx = argIdx2;
                } else {
                    pred_idx = predIdx1;
                    arg_idx = argIdx1;
                }
                while (grounding_itr.hasNext()) {
                    final List<PredicateCache> grounding = grounding_itr.next();
                    final PredicateCache target_pred_cache = grounding.get(pred_idx);

                    final Map<String, Set<Predicate>> inclusion_map = new HashMap<>();
                    for (Predicate predicate: target_pred_cache.inclusion) {
                        final Argument argument = predicate.args[arg_idx];
                        inclusion_map.compute(argument.name, (constant, set) -> {
                            if (null == set) {
                                set = new HashSet<>();
                            }
                            set.add(predicate);
                            return set;
                        });
                    }

                    if (1 == inclusion_map.size()) {
                        target_pred_cache.predicate.args[arg_idx] = new Constant(
                                CONSTANT_ARG_ID, inclusion_map.keySet().iterator().next()
                        );
                    } else {
                        grounding_itr.remove();
                        for (Map.Entry<String, Set<Predicate>> entry : inclusion_map.entrySet()) {
                            final List<PredicateCache> new_grounding = dupGrounding(grounding, true);
                            final PredicateCache new_target_pred_cache = new_grounding.get(pred_idx);
                            new_target_pred_cache.predicate.args[arg_idx] = new Constant(CONSTANT_ARG_ID, entry.getKey());
                            new_target_pred_cache.inclusion = entry.getValue();
                            grounding_itr.add(new_grounding);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected UpdateStatus boundFreeVars2NewVarHandler(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        final long time_start = System.nanoTime();
        boundFreeVars2NewVarUpdateCache(newPredicate, argIdx1, predIdx2, argIdx2, false);

        if (MIN_FACT_COVERAGE >= factCoverage()) {
            final long time_done = System.nanoTime();
            cacheMonitor.boundNewVarInNewPredCostInNano += time_done - time_start;
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }

        boundFreeVars2NewVarUpdateCache(newPredicate, argIdx1, predIdx2, argIdx2, true);
        final long time_done = System.nanoTime();
        cacheMonitor.boundNewVarInNewPredCostInNano += time_done - time_start;
        return UpdateStatus.NORMAL;
    }

    /**
     * 在对应的情况下更新Cache
     *
     * @param bodyOnly 更新的目标是否仅对于body
     */
    private void boundFreeVars2NewVarUpdateCache(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2, final boolean bodyOnly
    ) {
        final int predIdx1 = structure.size() - 1;
        final List<List<PredicateCache>> grounding_list;
        if (bodyOnly) {
            /* 修改一定涉及body，predIdx1一定是在body里 */
            grounding_list = groundingsBody;
        } else {
            grounding_list = groundings;
        }

        /* 而且在这种情况下，predIdx1 != predIdx2 */
        final ListIterator<List<PredicateCache>> grounding_itr = grounding_list.listIterator();
        final Map<String, Set<Predicate>> inclusion_map1 = kb.getArgIndices(newPredicate.functor, argIdx1);
        if (bodyOnly && HEAD_PRED_IDX == predIdx2) {
            /* 按值直接扩展 */
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();
                grounding_itr.remove();
                for (Map.Entry<String, Set<Predicate>> entry: inclusion_map1.entrySet()) {
                    final List<PredicateCache> new_grounding = dupGrounding(grounding, true);
                    final PredicateCache new_pred_cache = new PredicateCache(
                            new Predicate(newPredicate.functor, newPredicate.arity())
                    );
                    new_pred_cache.predicate.args[argIdx1] = new Constant(CONSTANT_ARG_ID, entry.getKey());
                    new_pred_cache.inclusion = entry.getValue();  // copy on write
                    new_grounding.add(new_pred_cache);
                    grounding_itr.add(new_grounding);
                }
            }
        } else {
            /* 两张表一起过滤 */
            while (grounding_itr.hasNext()) {
                final List<PredicateCache> grounding = grounding_itr.next();

                /* 找出参数常量值范围 */
                final PredicateCache target_pred_cache2 = grounding.get(predIdx2);
                final Map<String, Set<Predicate>> inclusion_map2 = new HashMap<>();
                for (Predicate predicate : target_pred_cache2.inclusion) {
                    final Argument argument2 = predicate.args[argIdx2];
                    inclusion_map2.compute(argument2.name, (constant, set) -> {
                        if (null == set) {
                            set = new HashSet<>();
                        }
                        set.add(predicate);
                        return set;
                    });
                }

                /* 做交叉 */
                final int comparing_pred_idx;
                final int comparing_arg_idx;
                final Map<String, Set<Predicate>> comparing_map;
                final int compared_pred_idx;
                final int compared_arg_idx;
                final Map<String, Set<Predicate>> compared_map;
                if (inclusion_map1.size() <= inclusion_map2.size()) {
                    comparing_pred_idx = predIdx1;
                    comparing_arg_idx = argIdx1;
                    comparing_map = inclusion_map1;
                    compared_pred_idx = predIdx2;
                    compared_arg_idx = argIdx2;
                    compared_map = inclusion_map2;
                } else {
                    comparing_pred_idx = predIdx2;
                    comparing_arg_idx = argIdx2;
                    comparing_map = inclusion_map2;
                    compared_pred_idx = predIdx1;
                    compared_arg_idx = argIdx1;
                    compared_map = inclusion_map1;
                }
                grounding_itr.remove();
                for (Map.Entry<String, Set<Predicate>> entry : comparing_map.entrySet()) {
                    final String constant_symbol = entry.getKey();
                    final Set<Predicate> compared_inclusion = compared_map.get(constant_symbol);
                    if (null != compared_inclusion) {
                        final Set<Predicate> comparing_inclusion = entry.getValue();
                        final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);

                        final List<PredicateCache> new_grounding = dupGrounding(grounding, bodyOnly);
                        new_grounding.add(new PredicateCache(
                                new Predicate(newPredicate.functor, newPredicate.arity())
                        ));

                        final PredicateCache comparing_pred_cache = new_grounding.get(comparing_pred_idx);
                        comparing_pred_cache.predicate.args[comparing_arg_idx] = constant;
                        comparing_pred_cache.inclusion = comparing_inclusion;

                        final PredicateCache compared_pred_cache = new_grounding.get(compared_pred_idx);
                        compared_pred_cache.predicate.args[compared_arg_idx] = constant;
                        compared_pred_cache.inclusion = compared_inclusion;

                        grounding_itr.add(new_grounding);
                    }
                }
            }
        }
    }

    @Override
    protected UpdateStatus boundFreeVar2ConstantHandler(int predIdx, int argIdx, String constantSymbol) {
        final long time_start = System.nanoTime();
        boundFreeVar2ConstantUpdateCache(predIdx, argIdx, constantSymbol, false);

        if (MIN_FACT_COVERAGE >= factCoverage()) {
            final long time_done = System.nanoTime();
            cacheMonitor.boundConstCostInNano += time_done - time_start;
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }

        boundFreeVar2ConstantUpdateCache(predIdx, argIdx, constantSymbol, true);
        final long time_done = System.nanoTime();
        cacheMonitor.boundConstCostInNano += time_done - time_start;
        return UpdateStatus.NORMAL;
    }

    /**
     * 在对应的情况下更新Cache
     *
     * @param bodyOnly 更新的目标是否仅对于body
     */
    private void boundFreeVar2ConstantUpdateCache(
            final int predIdx, final int argIdx, final String constantSymbol, boolean bodyOnly
    ) {
        final List<List<PredicateCache>> grounding_list;
        if (bodyOnly) {
            if (HEAD_PRED_IDX == predIdx) {
                /* 修改不涉及body的时候，body的cache不需要更新 */
                return;
            }
            grounding_list = groundingsBody;
        } else {
            grounding_list = groundings;
        }

        /* 过滤所有grounding */
        final Constant constant = new Constant(CONSTANT_ARG_ID, constantSymbol);
        final Iterator<List<PredicateCache>> grounding_itr = grounding_list.iterator();
        while (grounding_itr.hasNext()) {
            final List<PredicateCache> grounding = grounding_itr.next();
            final PredicateCache target_pred_cache = grounding.get(predIdx);
            final Set<Predicate> filtered_inclusion = new HashSet<>();
            for (Predicate predicate: target_pred_cache.inclusion) {
                final Argument argument = predicate.args[argIdx];
                if (constantSymbol.equals(argument.name)) {
                    filtered_inclusion.add(predicate);
                }
            }
            if (filtered_inclusion.isEmpty()) {
                grounding_itr.remove();
            } else {
                target_pred_cache.predicate.args[argIdx] = constant;
                target_pred_cache.inclusion = filtered_inclusion;  // copy on write
            }
        }
    }

    @Override
    protected double factCoverage() {
        final Set<Predicate> entailed_head = new HashSet<>();
        for (final List<PredicateCache> grounding_cache: groundings) {
            final PredicateCache head_pred_cache = grounding_cache.get(HEAD_PRED_IDX);
            for (Predicate head_pred: head_pred_cache.inclusion) {
                if (!kb.hasProved(head_pred)) {
                    entailed_head.add(head_pred);
                }
            }
        }
        return ((double) entailed_head.size()) /
                kb.getAllFacts(structure.get(HEAD_PRED_IDX).functor).size();
    }

    @Override
    protected Eval calculateEval() {
        /* 统计head中的变量信息 */
        final long time_query_begin = System.nanoTime();
        final Set<Integer> head_vars = new HashSet<>();
        int head_fv_cnt = 0;
        final Predicate head_pred = getHead();
        for (Argument argument: head_pred.args) {
            if (null == argument) {
                head_fv_cnt++;
            } else {
                if (argument.isVar) {
                    head_vars.add(argument.id);
                }
            }
        }

        /* 在body中找出所有generative var 第一次出现的变量位置 */
        class PredArgPos {
            final int predIdx;
            final int argIdx;

            public PredArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        final List<PredArgPos> body_gv_pos = new ArrayList<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            final Predicate body_pred = structure.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final Argument argument = body_pred.args[arg_idx];
                if (null != argument && argument.isVar && head_vars.remove(argument.id)) {
                    body_gv_pos.add(new PredArgPos(pred_idx, arg_idx));
                }
            }
        }
        final long time_pre_done = System.nanoTime();
        cacheMonitor.preComputingCostInNano += time_pre_done - time_query_begin;

        /* 计算all entail的数量 */
        final Set<ArrayList<String>> body_bv_bindings = new HashSet<>();
        for (final List<PredicateCache> grounding_body: groundingsBody) {
            final ArrayList<String> binding = new ArrayList<>(body_gv_pos.size());
            for (final PredArgPos pos: body_gv_pos) {
                final Predicate body_pred = grounding_body.get(pos.predIdx).predicate;
                final Argument argument = body_pred.args[pos.argIdx];
                binding.add(argument.name);
            }
            body_bv_bindings.add(binding);
        }
        final double all_entails = body_bv_bindings.size() * Math.pow(
                kb.totalConstants(), head_fv_cnt + head_vars.size()
        );
        final long time_all_entail_done = System.nanoTime();
        cacheMonitor.allEntailQueryCostInNano += time_all_entail_done - time_pre_done;

        /* 计算new pos entail的数量 */
        final Set<Predicate> newly_proved = new HashSet<>();
        final Set<Predicate> already_proved = new HashSet<>();
        if (0 == head_fv_cnt) {
            for (final List<PredicateCache> grounding : groundings) {
                final Predicate predicate = grounding.get(HEAD_PRED_IDX).predicate;
                if (!kb.hasProved(predicate)) {
                    newly_proved.add(predicate);
                } else {
                    already_proved.add(predicate);
                }
            }
        } else {
            for (final List<PredicateCache> grounding: groundings) {
                for (Predicate predicate: grounding.get(HEAD_PRED_IDX).inclusion) {
                    if (!kb.hasProved(predicate)) {
                        newly_proved.add(predicate);
                    } else {
                        already_proved.add(predicate);
                    }
                }
            }
        }
        final long time_pos_entail_done = System.nanoTime();
        cacheMonitor.posEntailQueryCostInNano += time_pos_entail_done - time_all_entail_done;

        cacheMonitor.cacheStats.add(new CachedQueryMonitor.CacheStat(
                groundings.size(), groundingsBody.size(), 0
        ));
        cacheMonitor.evalStats.add(new Eval(
                eval, newly_proved.size(), all_entails - already_proved.size(), size()
        ));

//        /* 用HC剪枝 */
//        double head_coverage = ((double) newly_proved.size()) / kb.getAllFacts(head_pred.functor).size();
//        if (Rule.MIN_FACT_COVERAGE >= head_coverage) {
//            return Eval.MIN;
//        }

        /* 更新eval */
        /* all entailments中需要刨除已经被证明的，否则这些默认被算作了counter examples的数量 */
        return new Eval(
                eval, newly_proved.size(), all_entails - already_proved.size(), size()
        );
    }

    protected Set<Predicate> findCounterExamples() {
        class GVBindingInfo {
            final int bodyPredIdx;
            final int bodyArgIdx;
            final Integer[] headVarLocs;

            public GVBindingInfo(int bodyPredIdx, int bodyArgIdx, Integer[] headVarLocs) {
                this.bodyPredIdx = bodyPredIdx;
                this.bodyArgIdx = bodyArgIdx;
                this.headVarLocs = headVarLocs;
            }
        }
        final Set<Predicate> counter_example_set = new HashSet<>();

        /* 统计head中的变量信息 */
        /* 如果是FV，则创建具体变量，方便替换 */
        final long time_query_start = System.nanoTime();
        final Map<Integer, List<Integer>> head_var_2_loc_map = new HashMap<>();
        int fv_id = boundedVars.size();
        final Predicate head_pred = new Predicate(getHead());
        for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
            final Argument argument = head_pred.args[arg_idx];
            if (null == argument) {
                head_var_2_loc_map.put(fv_id, new ArrayList<>(Collections.singleton(arg_idx)));
                fv_id++;
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

        /* 在body中找出所有generative var 第一次出现的变量位置 */
        final List<GVBindingInfo> body_gv_pos = new ArrayList<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            final Predicate body_pred = structure.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                final Argument argument = body_pred.args[arg_idx];
                if (null != argument && argument.isVar) {
                    List<Integer> head_var_locs = head_var_2_loc_map.remove(argument.id);
                    if (null != head_var_locs) {
                        body_gv_pos.add(new GVBindingInfo(
                                pred_idx, arg_idx, head_var_locs.toArray(new Integer[0])
                        ));
                    }
                }
            }
        }
        final Integer[][] head_only_var_locs = new Integer[head_var_2_loc_map.size()][];
        {
            int i = 0;
            for (List<Integer> loc_list: head_var_2_loc_map.values()) {
                head_only_var_locs[i] = loc_list.toArray(new Integer[0]);
                i++;
            }
        }
        final long time_pre_done = System.nanoTime();
        cacheMonitor.preComputingCostInNano += time_pre_done - time_query_start;

        /* 根据Rule结构找Counter Example */
        if (1 == structure.size()) {
            /* 没有body */
            if (0 == head_only_var_locs.length) {
                /* head中全是常量 */
                if (!kb.containsFact(head_pred)) {
                    counter_example_set.add(head_pred);
                }
            } else {
                /* head中有变量，而且全部当做自由变量处理 */
                iterate4CounterExamples(counter_example_set, head_pred, 0, head_only_var_locs);
            }
        } else {
            /* 找到所有head template */
            final Set<Predicate> head_templates = new HashSet<>();
            for (final List<PredicateCache> grounding_body : groundingsBody) {
                final Predicate head_template = new Predicate(head_pred);
                for (final GVBindingInfo pos : body_gv_pos) {
                    final Predicate body_pred = grounding_body.get(pos.bodyPredIdx).predicate;
                    final Argument argument = body_pred.args[pos.bodyArgIdx];
                    for (int loc: pos.headVarLocs) {
                        head_template.args[loc] = argument;
                    }
                }
                head_templates.add(head_template);
            }

            /* 遍历head template 找反例 */
            if (0 == head_only_var_locs.length) {
                /* 不需要替换变量 */
                for (Predicate head_template : head_templates) {
                    if (!kb.containsFact(head_template)) {
                        counter_example_set.add(head_template);
                    }
                }
            } else {
                /* 需要替换head中的变量 */
                for (Predicate head_template: head_templates) {
                    iterate4CounterExamples(counter_example_set, head_template, 0, head_only_var_locs);
                }
            }
        }
        final long time_all_entail_done = System.nanoTime();
        cacheMonitor.allEntailQueryCostInNano += time_all_entail_done - time_pre_done;

        cacheMonitor.cacheStats.add(new CachedQueryMonitor.CacheStat(
                groundings.size(), groundingsBody.size(), 0
        ));

        return counter_example_set;
    }

    protected List<Predicate[]> findGroundings() {
        final long pos_entail_begin = System.nanoTime();
        final List<Predicate[]> grounding_list = new ArrayList<>();
        final Set<Predicate> entailed_head = new HashSet<>();
        for (final List<PredicateCache> grounding_cache: groundings) {
            /* 找出grounding body */
            final Predicate[] grounding_body = new Predicate[structure.size()];
            for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
                final PredicateCache pred_cache = grounding_cache.get(pred_idx);
                grounding_body[pred_idx] = pred_cache.inclusion.iterator().next();
            }

            /* 构造其所有的head */
            final PredicateCache head_pred_cache = grounding_cache.get(HEAD_PRED_IDX);
            for (Predicate head_pred: head_pred_cache.inclusion) {
                if (!kb.hasProved(head_pred) && entailed_head.add(head_pred)) {
                    final Predicate[] grounding = dupGrounding(grounding_body, true);
                    grounding[HEAD_PRED_IDX] = head_pred;
                    grounding_list.add(grounding);
                    kb.proveFact(head_pred);
                }
            }
        }
        final long pos_entail_done = System.nanoTime();
        cacheMonitor.posEntailQueryCostInNano += pos_entail_done - pos_entail_begin;
        return grounding_list;
    }

    @Override
    protected void releaseCache() {
        groundings = null;
        groundingsBody = null;
    }

    private List<PredicateCache> dupGrounding(List<PredicateCache> grounding, boolean bodyOnly) {
        List<PredicateCache> new_grounding = new ArrayList<>(grounding.size());
        if (bodyOnly) {
            new_grounding.add(null);
            for (int i = FIRST_BODY_PRED_IDX; i < grounding.size(); i++) {
                new_grounding.add(new PredicateCache(grounding.get(i)));
            }
        } else {
            for (PredicateCache pred_cache: grounding) {
                new_grounding.add(new PredicateCache(pred_cache));
            }
        }
        return new_grounding;
    }

    private Predicate[] dupGrounding(final Predicate[] grounding, boolean bodyOnly) {
        final Predicate[] new_grounding = new Predicate[grounding.length];
        for (int pred_idx = bodyOnly ? FIRST_BODY_PRED_IDX : HEAD_PRED_IDX; pred_idx < grounding.length; pred_idx++) {
            new_grounding[pred_idx] = new Predicate(grounding[pred_idx]);
        }
        return new_grounding;
    }

    private void iterate4CounterExamples(
            final Set<Predicate> counterExamples, final Predicate template, final int idx,
            final Integer[][] varLocs
    ) {
        final Integer[] locations = varLocs[idx];
        if (idx < varLocs.length - 1) {
            /* 递归 */
            for (String constant_symbol: kb.allConstants()) {
                final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                iterate4CounterExamples(
                        counterExamples, template, idx + 1, varLocs
                );
            }
        } else {
            /* 已经到了最后的位置，不递归，完成后检查是否是Counter Example */
            for (String constant_symbol: kb.allConstants()) {
                final Constant constant = new Constant(CONSTANT_ARG_ID, constant_symbol);
                for (int loc: locations) {
                    template.args[loc] = constant;
                }
                if (!kb.containsFact(template)) {
                    counterExamples.add(new Predicate(template));
                }
            }
        }
    }
}