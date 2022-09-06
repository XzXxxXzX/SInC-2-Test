package sinc.impl.pruned.observed;

import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.impl.cached.MemKB;
import sinc.impl.pruned.tabu.TabuAwareRule;
import sinc.util.MultiSet;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleWithDupSpecObservation extends TabuAwareRule {

    static protected Set<RuleFingerPrint> uselessCache = new HashSet<>();

    protected Map<RuleFingerPrint, Rule> searchedFingerprints;
    protected PrintWriter dupRuleWriter;
    protected PrintWriter specRuleWriter;

    public RuleWithDupSpecObservation(
            String headFunctor, Map<RuleFingerPrint, Rule> cache, MemKB kb, Map<MultiSet<String>,
            Set<RuleFingerPrint>> category2TabuSetMap, PrintWriter dupRuleWriter, PrintWriter specRuleWriter
    ) {
        super(headFunctor, uselessCache, kb, category2TabuSetMap);
        this.searchedFingerprints = cache;
        this.dupRuleWriter = dupRuleWriter;
        this.specRuleWriter = specRuleWriter;
    }

    public RuleWithDupSpecObservation(RuleWithDupSpecObservation another) {
        super(another);
        this.searchedFingerprints = another.searchedFingerprints;
        this.dupRuleWriter = another.dupRuleWriter;
        this.specRuleWriter = another.specRuleWriter;
    }

    @Override
    public RuleWithDupSpecObservation clone() {
        final long time_start = System.nanoTime();
        RuleWithDupSpecObservation r =  new RuleWithDupSpecObservation(this);
        final long time_done = System.nanoTime();
        cacheMonitor.cloneCostInNano += time_done - time_start;
        return r;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(predIdx, argIdx, varId);
        long time_fp_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* 检查是否命中Cache */
        Rule dup_rule = searchedFingerprints.get(fingerPrint);
        if (null != dup_rule) {
            dupRuleWriter.println(this.toDumpString());
            dupRuleWriter.println(dup_rule.toDumpString());
        } else {
            searchedFingerprints.put(fingerPrint, this);
        }
        long time_cache_checked_nano = System.nanoTime();
        tabuAwareMonitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (null != dup_rule) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        tabuAwareMonitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否被tabu剪枝 */
        boolean tabu_hit = tabuHit();
        long time_tabu_checked_nano = System.nanoTime();
        tabuAwareMonitor.tabuCheckCostInNano += time_tabu_checked_nano - time_valid_checked_nano;
        if (tabu_hit) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(predIdx, argIdx, varId);
        long time_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateHandlerTimeNano += time_updated_nano - time_tabu_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        tabuAwareMonitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        /* 改变Rule结构 */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(functor, arity, argIdx, varId);
        long time_fp_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* 检查是否命中Cache */
        Rule dup_rule = searchedFingerprints.get(fingerPrint);
        if (null != dup_rule) {
            dupRuleWriter.println(this.toDumpString());
            dupRuleWriter.println(dup_rule.toDumpString());
        } else {
            searchedFingerprints.put(fingerPrint, this);
        }
        long time_cache_checked_nano = System.nanoTime();
        tabuAwareMonitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (null != dup_rule) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        tabuAwareMonitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否被tabu剪枝 */
        boolean tabu_hit = tabuHit();
        long time_tabu_checked_nano = System.nanoTime();
        tabuAwareMonitor.tabuCheckCostInNano += time_tabu_checked_nano - time_valid_checked_nano;
        if (tabu_hit) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(structure.get(structure.size() - 1), argIdx, varId);
        long time_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateHandlerTimeNano += time_updated_nano - time_tabu_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        tabuAwareMonitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVars2NewVarUpdateStructure(predIdx1, argIdx1, predIdx2, argIdx2);
        long time_fp_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* 检查是否命中Cache */
        Rule dup_rule = searchedFingerprints.get(fingerPrint);
        if (null != dup_rule) {
            dupRuleWriter.println(this.toDumpString());
            dupRuleWriter.println(dup_rule.toDumpString());
        } else {
            searchedFingerprints.put(fingerPrint, this);
        }
        long time_cache_checked_nano = System.nanoTime();
        tabuAwareMonitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (null != dup_rule) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        tabuAwareMonitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否被tabu剪枝 */
        boolean tabu_hit = tabuHit();
        long time_tabu_checked_nano = System.nanoTime();
        tabuAwareMonitor.tabuCheckCostInNano += time_tabu_checked_nano - time_valid_checked_nano;
        if (tabu_hit) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(predIdx1, argIdx1, predIdx2, argIdx2);
        long time_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateHandlerTimeNano += time_updated_nano - time_tabu_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        tabuAwareMonitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* 改变Rule结构 */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVars2NewVarUpdateStructure(functor, arity, argIdx1, predIdx2, argIdx2);
        long time_fp_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* 检查是否命中Cache */
        Rule dup_rule = searchedFingerprints.get(fingerPrint);
        if (null != dup_rule) {
            dupRuleWriter.println(this.toDumpString());
            dupRuleWriter.println(dup_rule.toDumpString());
        } else {
            searchedFingerprints.put(fingerPrint, this);
        }
        long time_cache_checked_nano = System.nanoTime();
        tabuAwareMonitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (null != dup_rule) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        tabuAwareMonitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否被tabu剪枝 */
        boolean tabu_hit = tabuHit();
        long time_tabu_checked_nano = System.nanoTime();
        tabuAwareMonitor.tabuCheckCostInNano += time_tabu_checked_nano - time_valid_checked_nano;
        if (tabu_hit) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(
                structure.get(structure.size() - 1), argIdx1, predIdx2, argIdx2
        );
        long time_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateHandlerTimeNano += time_updated_nano - time_tabu_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        tabuAwareMonitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus boundFreeVar2Constant(final int predIdx, final int argIdx, final String constantSymbol) {
        /* 改变Rule结构 */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ConstantUpdateStructure(predIdx, argIdx, constantSymbol);
        long time_fp_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* 检查是否命中Cache */
        Rule dup_rule = searchedFingerprints.get(fingerPrint);
        if (null != dup_rule) {
            dupRuleWriter.println(this.toDumpString());
            dupRuleWriter.println(dup_rule.toDumpString());
        } else {
            searchedFingerprints.put(fingerPrint, this);
        }
        long time_cache_checked_nano = System.nanoTime();
        tabuAwareMonitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (null != dup_rule) {
            return UpdateStatus.DUPLICATED;
        }

        /* 检查合法性 */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        tabuAwareMonitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* 检查是否被tabu剪枝 */
        boolean tabu_hit = tabuHit();
        long time_tabu_checked_nano = System.nanoTime();
        tabuAwareMonitor.tabuCheckCostInNano += time_tabu_checked_nano - time_valid_checked_nano;
        if (tabu_hit) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* 执行handler */
        final UpdateStatus status = boundFreeVar2ConstantHandler(predIdx, argIdx, constantSymbol);
        long time_updated_nano = System.nanoTime();
        tabuAwareMonitor.updateHandlerTimeNano += time_updated_nano - time_tabu_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* 更新Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        tabuAwareMonitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected boolean tabuHit() {
        boolean hit = false;
        for (int subset_size = 0; subset_size < structure.size(); subset_size++) {
            for (MultiSet<String> category_subset : categorySubsets(subset_size)) {
                final Set<RuleFingerPrint> tabu_set = category2TabuSetMap.get(category_subset);
                if (null == tabu_set) continue;
                for (RuleFingerPrint rfp : tabu_set) {
                    tabuAwareMonitor.tabuCompares++;
                    if (rfp.predecessorOf(this.fingerPrint)) {
                        specRuleWriter.println(this.toDumpString());
                        specRuleWriter.println(toDumpString(rfp.rule));
                        hit = true;
                        break;
                    }
                }
                if (hit) break;
            }
        }
        return hit;
    }

    public String toDumpString(List<Predicate> structure) {
        StringBuilder builder = new StringBuilder(structure.get(0).toString());
        builder.append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }
}
