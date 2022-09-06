package sinc.impl.cached;

import sinc.SInC;
import sinc.SincConfig;
import sinc.common.*;
import sinc.impl.cached.recal.RecalculateCachedRule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class CachedSinc extends SInC {

    protected final MemKB kb = new MemKB();
    protected final CachedQueryMonitor cacheMonitor = new CachedQueryMonitor();

    public CachedSinc(SincConfig config, String kbPath, String dumpPath, String logPath) {
        super(
                new SincConfig(
                        config.threads,
                        config.validation,
                        config.debug,
                        config.beamWidth,
                        false,  // Rule Cache 的优化方案不支持向前搜索
                        config.evalMetric,
                        config.minFactCoverage,
                        config.minConstantCoverage,
                        config.minColumnSimilarity,
                        config.stopCompressionRate,
                        true,
                        -1.0,
                        false,
                        false
                ),
                kbPath,
                dumpPath,
                logPath
        );
    }

    @Override
    protected KbStatistics loadKb() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(kbPath));
            String line;
            while (null != (line = reader.readLine())) {
                final String[] components = line.split("\t");
                final Predicate predicate = new Predicate(components[0], components.length - 1);
                for (int i = 1; i < components.length; i++) {
                    predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
                }
                kb.addFact(predicate);
            }
            kb.calculatePromisingConstants(config.minConstantCoverage);
            kb.calculateSimilarColumnPairs(config.minColumnSimilarity);

            return new KbStatistics(
                    kb.totalFacts(),
                    kb.getFunctor2ArityMap().size(),
                    kb.totalConstants(),
                    kb.getActualConstantSubstitutions(),
                    kb.getTotalConstantSubstitutions(),
                    kb.similarColumnPairs(),
                    kb.totalColumnPairs()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new KbStatistics(-1, -1, -1, -1, -1, -1, -1);
    }

    @Override
    protected List<String> getTargetFunctors() {
        return kb.getAllFunctors();
    }

    @Override
    protected Map<String, Integer> getFunctor2ArityMap() {
        return kb.getFunctor2ArityMap();
    }

    @Override
    protected Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return kb.getFunctor2PromisingConstantMap();
    }

    @Override
    protected boolean columnSimilar(String functor1, int idx1, String functor2, int idx2) {
        return kb.columnsSimilar(functor1, idx1, functor2, idx2);
    }

    @Override
    protected Set<Predicate> getOriginalKb() {
        return kb.getOriginalKB();
    }

    @Override
    public Set<String> getAllConstants() {
        return kb.getAllConstants();
    }

    @Override
    protected void recordRuleStatus(Rule rule, Rule.UpdateStatus updateStatus) {
        CachedRule r = (CachedRule) rule;
        cacheMonitor.totalClones++;
        cacheMonitor.cloneCostInNano += r.cacheMonitor.cloneCostInNano;

        /* 下列参数只在正常Update的Rule中记录 */
        if (Rule.UpdateStatus.NORMAL != updateStatus) {
            return;
        }
        cacheMonitor.preComputingCostInNano += r.cacheMonitor.preComputingCostInNano;
        cacheMonitor.allEntailQueryCostInNano += r.cacheMonitor.allEntailQueryCostInNano;
        cacheMonitor.posEntailQueryCostInNano += r.cacheMonitor.posEntailQueryCostInNano;
        cacheMonitor.boundExistVarCostInNano += r.cacheMonitor.boundExistVarCostInNano;
        cacheMonitor.boundExistVarInNewPredCostInNano += r.cacheMonitor.boundExistVarInNewPredCostInNano;
        cacheMonitor.boundNewVarCostInNano += r.cacheMonitor.boundNewVarCostInNano;
        cacheMonitor.boundNewVarInNewPredCostInNano += r.cacheMonitor.boundNewVarInNewPredCostInNano;
        cacheMonitor.boundConstCostInNano += r.cacheMonitor.boundConstCostInNano;
        cacheMonitor.cacheStats.addAll(r.cacheMonitor.cacheStats);
        cacheMonitor.evalStats.addAll(r.cacheMonitor.evalStats);
    }

    @Override
    protected UpdateResult updateKb(Rule rule) {
        CachedRule forward_cached_rule = (CachedRule) rule;
        return forward_cached_rule.updateInKb();
    }

    @Override
    protected void showMonitor() {
        super.showMonitor();
        cacheMonitor.show(logger);
    }
}
