package sinc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalTest {

    @Test
    public void testEvalMin() {
        Eval min = Eval.MIN;
        Eval[] evs = new Eval[]{
                new Eval(null, 0, 1, 2),
                new Eval(null, 100, 100, 2),
                new Eval(null, 100, 120, 3),
                Eval.MIN
        };

//        for (Eval.EvalMetric type: Eval.EvalMetric.values()) {
        for (Eval.EvalMetric type: new Eval.EvalMetric[]{
                Eval.EvalMetric.CompressionCapacity,
                Eval.EvalMetric.CompressionRate
        }) {
            for (Eval ev: evs) {
                assertTrue(min.value(type) <= ev.value(type));
            }

            assertTrue(evs[0].value(type) < evs[1].value(type));
            assertTrue(evs[2].value(type) < evs[1].value(type));

            assertFalse(evs[0].useful(type));
            assertTrue(evs[1].useful(type));
            assertTrue(evs[2].useful(type));
        }
    }
}