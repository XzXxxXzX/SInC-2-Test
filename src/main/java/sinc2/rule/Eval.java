package sinc2.rule;

import java.util.Objects;

/**
 * The quality evaluation of rules for compressing knowledge bases.
 * 
 * @since 1.0
 */
public class Eval {

    /**
     * A stand-alone class for minimum evaluation value.
     */
    private static class EvalMin extends Eval {
        private EvalMin() {
            super(null, 0, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
        }

        @Override
        public double value(EvalMetric type) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public boolean useful() {
            return false;
        }
    }

    /** The minimum value of any metric */
    public static final Eval MIN = new EvalMin();

    /** The threshold of the compression ratio of a useful rule */
    public static final double COMP_RATIO_USEFUL_THRESHOLD = 0.5;

    /** The number of positive entailments (double type to be the same as 'negCnt') */
    protected final double posEtls;

    /** The number of negative entailments (double type because values may be extremely large) */
    protected final double negEtls;

    /** The number of total entailments (double type because values may be extremely large) */
    protected final double allEtls;

    /** The length of a rule */
    protected final int ruleLength;

    /** The score of compression ratio */
    protected final double compRatio;

    /** The score of compression capacity */
    protected final double compCapacity;

    /** The score of information gain */
    protected final double infoGain;

    /**
     * Initialize a evaluation score.
     *
     * @param previousEval The evaluation of the rule that is specialized to the rule of this evaluation Todo: this should be removed from the calculation
     * @param posEtls The total number of positive entailments
     * @param allEtls The total number of entailments
     * @param ruleLength The length of the rule
     */
    public Eval(Eval previousEval, double posEtls, double allEtls, int ruleLength) {
        this.posEtls = posEtls;
        this.negEtls = allEtls - posEtls;
        this.allEtls = allEtls;
        this.ruleLength = ruleLength;

        double tmp_ratio = posEtls / (allEtls + ruleLength);
        this.compRatio = Double.isNaN(tmp_ratio) ? 0 : tmp_ratio;

        this.compCapacity = posEtls - negEtls - ruleLength;

        double delta_info = (0 == posEtls) ? 0 : ((null == previousEval || 0 == previousEval.posEtls) ?
                posEtls * Math.log(posEtls / allEtls) :
                posEtls * (Math.log(posEtls / allEtls) - Math.log(previousEval.posEtls / previousEval.allEtls))
        );
        this.infoGain = ((null == previousEval) ? 0 : previousEval.infoGain) + delta_info;
    }

    /**
     * Get the score value of certain metric type.
     *
     * @param type The type of the evaluation metric
     * @return The evaluation score
     */
    public double value(EvalMetric type) {
        switch (type) {
            case CompressionRatio:
                return compRatio;
            case CompressionCapacity:
                return compCapacity;
            case InfoGain:
                return infoGain;
            default:
                return 0;
        }
    }

    /**
     * Check whether the evaluation score indicates a useful rule for compression.
     */
    public boolean useful() {
        return 0 < compCapacity;
    }

    public double getAllEtls() {
        return allEtls;
    }

    public double getPosEtls() {
        return posEtls;
    }

    public double getNegEtls() {
        return negEtls;
    }

    @Override
    public String toString() {
        return String.format(
                "(+)%f; (-)%f; |%d|; δ=%f; τ=%f; h=%f", posEtls, negEtls, ruleLength, compCapacity, compRatio, infoGain
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eval eval = (Eval) o;
        return Double.compare(eval.posEtls, posEtls) == 0 &&
                Double.compare(eval.negEtls, negEtls) == 0 &&
                Double.compare(eval.allEtls, allEtls) == 0 &&
                ruleLength == eval.ruleLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posEtls, negEtls, ruleLength);
    }
}
