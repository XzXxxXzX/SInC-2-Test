package sinc.exp;

import org.junit.jupiter.api.Test;
import sinc.common.Predicate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompareSpecDetectionByBruteForceTest {
    @Test
    void testMatch() throws Exception {
        CompareSpecDetectionByBruteForce.kb = CompareSpecDetectionByBruteForce.loadKb("datasets/family_medium.tsv");
        List<Predicate> original_rule = CompareDupDetectionByBruteForce.parseRule(
                "sister(?,X0):-gender(?,X0)"
        );
        List<Predicate> extended_rule = CompareDupDetectionByBruteForce.parseRule(
                "sister(X1,X0):-uncle(X0,?),aunt(X1,?),gender(?,X0)"
        );
        assertTrue(CompareSpecDetectionByBruteForce.matchRules(original_rule, extended_rule));
    }
}