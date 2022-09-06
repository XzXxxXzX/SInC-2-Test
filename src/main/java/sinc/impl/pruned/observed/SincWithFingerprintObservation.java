package sinc.impl.pruned.observed;

import sinc.SincConfig;
import sinc.common.*;
import sinc.impl.pruned.tabu.SincWithTabuPruning;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SincWithFingerprintObservation extends SincWithTabuPruning {

    protected PrintWriter dupRuleWriter;
    protected PrintWriter specRuleWriter;

    public SincWithFingerprintObservation(
            SincConfig config, String kbPath, String dumpPath, String logPath
    ) {
        super(config, kbPath, dumpPath, logPath);

        String dupRulePath = (null == logPath) ? null : logPath.replace(".log", ".dup");
        PrintWriter writer;
        try {
            writer = (null == dupRulePath) ? new PrintWriter(System.out, true) : new PrintWriter(dupRulePath);
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.dupRuleWriter = writer;

        String specRulePath = (null == logPath) ? null : logPath.replace(".log", ".spec");
        try {
            writer = (null == specRulePath) ? new PrintWriter(System.out, true) : new PrintWriter(specRulePath);
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.specRuleWriter = writer;
    }

    protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
        return new RuleWithDupSpecObservation(headFunctor, new HashMap<>(), kb, category2TabuSetMap, dupRuleWriter, specRuleWriter);
    }

    @Override
    protected void showMonitor() {
        super.showMonitor();
        dupRuleWriter.flush();
        specRuleWriter.flush();
    }
}
