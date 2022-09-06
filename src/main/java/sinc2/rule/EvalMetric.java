package sinc2.rule;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of quality evaluation metrics.
 *
 * @since 1.0
 */
public enum EvalMetric {
    /** Compression Rate */
    CompressionRatio("τ", "Compression Rate"),

    /** Compression Capacity */
    CompressionCapacity("δ", "Compression Capacity"),

    /** Information Gain (proposed in FOIL) */
    InfoGain("h", "Information Gain");

    /** Symbol to enumeration map, used for getting values by symbol */
    private static final Map<String, EvalMetric> symbol2ValMap = new HashMap<>();
    static {
        for (EvalMetric metric: EvalMetric.values()) {
            symbol2ValMap.put(metric.symbol, metric);
        }
    }

    /** Symbol of the metric */
    private final String symbol;

    /** Description of the metric */
    private final String description;

    EvalMetric(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    static public EvalMetric getBySymbol(String name) {
        return symbol2ValMap.get(name);
    }
}
