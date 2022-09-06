package sinc.impl.pruned.symmetric;

import org.junit.jupiter.api.Test;
import sinc.SInC;
import sinc.SincConfig;
import sinc.common.Eval;
import sinc.common.Dataset;

import static org.junit.jupiter.api.Assertions.*;

class Sinc4SymmetricTest {
    @Test
    void testWN() {
        SInC sinc = new Sinc4Symmetric(new SincConfig(
                1,
                false,
                false,
                5,
                true,
                Eval.EvalMetric.InfoGain,
                0.05,
                0.25,
                0,
                1,
                false,
                -1.0,
                false,
                false
        ), Dataset.WN18.getPath(),"Sym_h_5.result", "Sym_h_5.log");
        sinc.run();
        assertTrue(sinc.recover());
    }
}