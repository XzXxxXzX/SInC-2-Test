package sinc.impl.pruned.tabu;

import sinc.SincConfig;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.impl.cached.recal.SincWithRecalculateCache;
import sinc.util.MultiSet;

import java.util.*;

public class SincWithTabuPruning extends SincWithRecalculateCache {

    /* 每次迭代只保留下次生成的长度的tabu rules */
    protected Map<MultiSet<String>, Set<RuleFingerPrint>> category2TabuSetMap = new HashMap<>();
    protected final TabuMonitor tabuMonitor = new TabuMonitor();

    public SincWithTabuPruning(SincConfig config, String kbPath, String dumpPath, String logPath) {
        super(config, kbPath, dumpPath, logPath);
        TabuAwareRule.tabuAwareMonitor = new TabuAwareRuleMonitor();
    }

    @Override
    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new TabuAwareRule(headFunctor, cache, kb, category2TabuSetMap);
    }

    @Override
    protected void targetDone(String functor) {
        /* 在每个Head变换之后都需要Change Tabu set */
        int total_tabus = 0;
        for (Set<RuleFingerPrint> tabu_set: category2TabuSetMap.values()) {
            total_tabus += tabu_set.size();
        }
        tabuMonitor.tabusInDiffHeadFunctor.add(total_tabus);
        tabuMonitor.totalTabus += total_tabus;
        tabuMonitor.categoriesInDiffHeadFunctor.add(category2TabuSetMap.size());
        tabuMonitor.totalCategories += category2TabuSetMap.size();
        category2TabuSetMap = new HashMap<>();
    }

    @Override
    protected void recordRuleStatus(Rule rule, Rule.UpdateStatus updateStatus) {
        super.recordRuleStatus(rule, updateStatus);
        if (updateStatus == Rule.UpdateStatus.INSUFFICIENT_COVERAGE) {
            final MultiSet<String> functor_mset = new MultiSet<>();
            for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
                functor_mset.add(rule.getPredicate(pred_idx).functor);
            }
            final Set<RuleFingerPrint> tabu_set = category2TabuSetMap.computeIfAbsent(
                    functor_mset, k -> new HashSet<>()
            );
            tabu_set.add(rule.getFingerPrint());
        }
    }

    @Override
    protected void showMonitor() {
        super.showMonitor();
        tabuMonitor.show(logger);
        TabuAwareRule.tabuAwareMonitor.show(logger);
    }

    @Override
    public String getModelName() {
        return "Ct";
    }
}
