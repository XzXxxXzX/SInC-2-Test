package sinc.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Eval {
    public enum EvalMetric {
        CompressionRate("τ", "Compression Rate"),
        CompressionCapacity("δ", "Compression Capacity"),
        InfoGain("h", "Information Gain"),
        CumulatedInfo("H", "Cumulated Information");
        private final String name;
        private final String description;

        private static final Map<String, EvalMetric> name2ValMap = new HashMap<>();
        static {
            for (EvalMetric metric: EvalMetric.values()) {
                name2ValMap.put(metric.name, metric);
            }
        }

        EvalMetric(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        static public EvalMetric getByName(String name) {
            return name2ValMap.get(name);
        }
    }

    private static class EvalMin extends Eval {
        private EvalMin() {
            super(null, 0, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
        }

        @Override
        public double value(EvalMetric type) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public boolean useful(EvalMetric type) {
            return false;
        }
    }

    public static final Eval MIN = new EvalMin();

    private static final double COMP_RATIO_USEFUL_THRESHOLD = 0.5;
    private static final double COMP_CAPACITY_USEFUL_THRESHOLD = 0.0;
    private static final double INFO_GAIN_USEFUL_THRESHOLD = 0.00;

    private final double posCnt;
    private final double negCnt;
    private final double allCnt;
    private final int ruleSize;

    private final double compRatio;
    private final double compCapacity;
    private final double infoGain;
    private final double cumulatedInfo;

    public Eval(Eval previousEval, double posCnt, double allCnt, int ruleSize) {
        this.posCnt = posCnt;
        this.negCnt = allCnt - posCnt;
        this.allCnt = allCnt;
        this.ruleSize = ruleSize;

        double tmp_ratio = posCnt / (allCnt + ruleSize);
        this.compRatio = Double.isNaN(tmp_ratio) ? 0 : tmp_ratio;

        this.compCapacity = posCnt - negCnt - ruleSize;

        if (0 == posCnt) {
            this.infoGain = 0;
        } else {
            if (null == previousEval || 0 == previousEval.posCnt) {
                this.infoGain = posCnt * Math.log(posCnt / allCnt);
            } else {
                this.infoGain = posCnt * (
                        Math.log(posCnt / allCnt) - Math.log(previousEval.posCnt / previousEval.allCnt)
                );
            }
        }
        this.cumulatedInfo = ((null == previousEval) ? 0 : previousEval.cumulatedInfo) + this.infoGain;
    }

    public double value(EvalMetric type) {
        switch (type) {
            case CompressionRate:
                return compRatio;
            case CompressionCapacity:
                return compCapacity;
            case InfoGain:
                return infoGain;
            case CumulatedInfo:
                return cumulatedInfo;
            default:
                return 0;
        }
    }

    public boolean useful(EvalMetric type) {
        return compCapacity > COMP_CAPACITY_USEFUL_THRESHOLD;
    }

    public double getAllCnt() {
        return allCnt;
    }

    public double getPosCnt() {
        return posCnt;
    }

    public double getNegCnt() {
        return negCnt;
    }

    @Override
    public String toString() {
        return String.format(
                "(+)%f; (-)%f; |%d|; δ=%f; τ=%f; h=%f; H=%f", posCnt, negCnt, ruleSize, compCapacity, compRatio, infoGain, cumulatedInfo
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eval eval = (Eval) o;
        return Double.compare(eval.posCnt, posCnt) == 0 &&
                Double.compare(eval.negCnt, negCnt) == 0 &&
                Double.compare(eval.allCnt, allCnt) == 0 &&
                ruleSize == eval.ruleSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posCnt, negCnt, allCnt, ruleSize);
    }
}
