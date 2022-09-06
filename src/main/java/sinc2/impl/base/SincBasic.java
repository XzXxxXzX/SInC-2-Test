package sinc2.impl.base;

import sinc2.RelationMiner;
import sinc2.SInC;
import sinc2.SincConfig;

/**
 * A basic implementation of SInC. Rule mining are with compact caching and tabu prunning.
 *
 * @since 2.0
 */
public class SincBasic extends SInC {
    /**
     * Create a SInC object with configurations.
     *
     * @param config The configurations
     */
    public SincBasic(SincConfig config) {
        super(config);
    }

    @Override
    protected RelationMiner createRelationMiner(int targetRelationNum) {
        return new RelationMinerBasic(
                kb, targetRelationNum, config.evalMetric, config.beamwidth, config.stopCompressionRatio,
                predicate2NodeMap, dependencyGraph, logger
        );
    }
}
