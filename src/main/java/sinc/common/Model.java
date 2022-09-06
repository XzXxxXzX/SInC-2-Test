package sinc.common;

import sinc.SInC;
import sinc.SincConfig;
import sinc.impl.cached.recal.SincWithRecalculateCache;
import sinc.impl.cached.spec.SincWithSpecificCache;
import sinc.impl.pruned.observed.SincWithFingerprintObservation;
import sinc.impl.pruned.symmetric.Sinc4Symmetric;
import sinc.impl.pruned.tabu.SincWithTabuPruning;

import java.util.HashMap;
import java.util.Map;

public enum Model {
    CACHE_COMPACT("C", "SInC with compact cache"),
    CACHE_MATERIALIZED("M", "SInC with materialized cache"),
    TABU("T", "Model C with tabu pruning"),
    OBSERVED_TABU("To", "Model T which observes rules pruned by duplication and tabu set"),
    SYMMETRY("Y", "Model T focus on symmetric relations (for experiments only)");

    static final Map<String, Model> nameMap = new HashMap<>();
    static {
        for (Model model: Model.values()) {
            nameMap.put(model.name, model);
        }
    }

    private final String name;
    private final String description;

    Model(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Model getModelByName(String name) {
        return nameMap.get(name);
    }

    public static SInC getModel(
            String name, SincConfig config, String dataPath, String dumpPath, String logPath
    ) throws SincException {
        switch (getModelByName(name)) {
            case CACHE_COMPACT:
                return new SincWithRecalculateCache(config, dataPath, dumpPath, logPath);
            case CACHE_MATERIALIZED:
                return new SincWithSpecificCache(config, dataPath, dumpPath, logPath);
            case TABU:
                return new SincWithTabuPruning(config, dataPath, dumpPath, logPath);
            case OBSERVED_TABU:
                return new SincWithFingerprintObservation(config, dataPath, dumpPath, logPath);
            case SYMMETRY:
                return new Sinc4Symmetric(config, dataPath, dumpPath, logPath);
            default:
                throw new SincException("Unknown Model: " + name);
        }
    }
}
