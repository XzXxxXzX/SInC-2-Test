package sinc.common;

import java.io.PrintWriter;

public class RuleMonitor {
    private static final int NANOS_PER_MILLI = 1000000;

    /* Rule Update Cost */
    public long updateFingerPrintTimeNano = 0;
    public long dupCheckTimeNano = 0;
    public long validCheckTimeNano = 0;
    public long updateHandlerTimeNano = 0;
    public long evalTimeNano = 0;

    public void show(PrintWriter writer) {
        writer.println("### Monitored Rule Info ###\n");
        writer.println("--- Time Cost ---");
        writer.printf(
                "(ms) %10s %10s %10s %10s %10s\n",
                "UpdFP", "DupCheck", "ValidCheck", "UpdHand", "Eval"
        );
        writer.printf(
                "     %10d %10d %10d %10d %10d\n\n",
                updateFingerPrintTimeNano / NANOS_PER_MILLI,
                dupCheckTimeNano / NANOS_PER_MILLI,
                validCheckTimeNano / NANOS_PER_MILLI,
                updateHandlerTimeNano / NANOS_PER_MILLI,
                evalTimeNano / NANOS_PER_MILLI
        );
    }
}
