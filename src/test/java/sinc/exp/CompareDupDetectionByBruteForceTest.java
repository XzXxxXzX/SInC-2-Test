package sinc.exp;

import org.junit.jupiter.api.Test;
import sinc.common.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompareDupDetectionByBruteForceTest {
    @Test
    void testRuleParser1() {
        /* p(X, ?, Y) :- */
        List<Predicate> rule_structure = new ArrayList<>();
        Predicate head = new Predicate("p", 3);
        head.args[0] = new Variable(0);
        head.args[2] = new Variable(1);
        rule_structure.add(head);
        BareRule rule = new BareRule(rule_structure, new HashSet<>());
        String str = rule.toDumpString();

        List<Predicate> recovered_structure = CompareDupDetectionByBruteForce.parseRule(str);
        assertEquals(rule_structure, recovered_structure);
    }

    @Test
    void testRuleParser2() {
        /* pred(X, tom, X) :- body(Y), another(X, ?, Y) */
        List<Predicate> rule_structure = new ArrayList<>();
        Predicate head = new Predicate("pred", 3);
        head.args[0] = new Variable(0);
        head.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        head.args[2] = new Variable(0);
        Predicate body1 = new Predicate("body", 1);
        body1.args[0] = new Variable(1);
        Predicate body2 = new Predicate("another", 3);
        body2.args[0] = new Variable(0);
        body2.args[2] = new Variable(0);
        rule_structure.add(head);
        rule_structure.add(body1);
        rule_structure.add(body2);
        BareRule rule = new BareRule(rule_structure, new HashSet<>());
        String str = rule.toDumpString();

        List<Predicate> recovered_structure = CompareDupDetectionByBruteForce.parseRule(str);
        assertEquals(rule_structure, recovered_structure);
    }

    @Test
    void testRuleMatch0() {
        /* p(a, ?, b) :- */
        /* p(a, ?, b) :- */
        Predicate head1 = new Predicate("p", 3);
        head1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        head1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("p", 3);
        head2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        head2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertTrue(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch1() {
        /* p(X, Y) :- */
        /* p(Y, X) :- */
        Predicate head1 = new Predicate("p", 2);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("p", 2);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Variable(0);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertTrue(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch2() {
        /* p(X, Y, Z) :- q(Y, Z, X) */
        /* p(Y, Z, X) :- q(Z, X, Y) */
        Predicate head1 = new Predicate("p", 3);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        head1.args[2] = new Variable(2);
        Predicate body11 = new Predicate("q", 3);
        body11.args[0] = new Variable(1);
        body11.args[1] = new Variable(2);
        body11.args[2] = new Variable(0);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("p", 3);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Variable(2);
        head2.args[2] = new Variable(0);
        Predicate body21 = new Predicate("q", 3);
        body21.args[0] = new Variable(2);
        body21.args[1] = new Variable(0);
        body21.args[2] = new Variable(1);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertTrue(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch3() {
        /* pred(X, con, Y) :- body(tom, ?) */
        /* pred(Y, con, X) :- body(tom, ?) */
        Predicate head1 = new Predicate("pred", 3);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "con");
        head1.args[2] = new Variable(1);
        Predicate body11 = new Predicate("body", 2);
        body11.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("pred", 3);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "con");
        head2.args[2] = new Variable(0);
        Predicate body21 = new Predicate("body", 2);
        body21.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "tom");
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertTrue(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch4() {
        /* p(X, Y) :- q(X, Z), q(Z, W), q(W, R), q(R, Y) */
        /* p(Y, X) :- q(Y, R), q(R, Z), q(Z, W), q(W, X) */
        Predicate head1 = new Predicate("p", 2);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        Predicate body11 = new Predicate("q", 2);
        body11.args[0] = new Variable(0);
        body11.args[1] = new Variable(2);
        Predicate body12 = new Predicate("q", 2);
        body12.args[0] = new Variable(2);
        body12.args[1] = new Variable(3);
        Predicate body13 = new Predicate("q", 2);
        body13.args[0] = new Variable(3);
        body13.args[1] = new Variable(4);
        Predicate body14 = new Predicate("q", 2);
        body14.args[0] = new Variable(4);
        body14.args[1] = new Variable(1);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        rule1.add(body12);
        rule1.add(body13);
        rule1.add(body14);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("p", 2);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Variable(0);
        Predicate body21 = new Predicate("q", 2);
        body21.args[0] = new Variable(1);
        body21.args[1] = new Variable(4);
        Predicate body22 = new Predicate("q", 2);
        body22.args[0] = new Variable(4);
        body22.args[1] = new Variable(2);
        Predicate body23 = new Predicate("q", 2);
        body23.args[0] = new Variable(2);
        body23.args[1] = new Variable(3);
        Predicate body24 = new Predicate("q", 2);
        body24.args[0] = new Variable(3);
        body24.args[1] = new Variable(0);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        rule2.add(body22);
        rule2.add(body23);
        rule2.add(body24);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertTrue(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch5() {
        /* FP */
        /* parent(X, Y) :- father(X, X), father(?, Y) */
        /* parent(X, Y) :- father(X, Y), father(?, X) */
        Predicate head1 = new Predicate("parent", 2);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        Predicate body11 = new Predicate("father", 2);
        body11.args[0] = new Variable(0);
        body11.args[1] = new Variable(0);
        Predicate body12 = new Predicate("father", 2);
        body12.args[1] = new Variable(1);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        rule1.add(body12);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("parent", 2);
        head2.args[0] = new Variable(0);
        head2.args[1] = new Variable(1);
        Predicate body21 = new Predicate("father", 2);
        body21.args[0] = new Variable(0);
        body21.args[1] = new Variable(1);
        Predicate body22 = new Predicate("father", 2);
        body22.args[1] = new Variable(0);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        rule2.add(body22);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertFalse(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch6() {
        /* FP */
        /* parent(X, Y) :- parent(X, Z), father(Z, W), father(?, R), mother(W, R) */
        /* parent(X, Y) :- parent(X, Z), father(Z, R), father(?, W), mother(W, R) */
        Predicate head1 = new Predicate("parent", 2);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        Predicate body11 = new Predicate("parent", 2);
        body11.args[0] = new Variable(0);
        body11.args[1] = new Variable(2);
        Predicate body12 = new Predicate("father", 2);
        body12.args[0] = new Variable(2);
        body12.args[1] = new Variable(3);
        Predicate body13 = new Predicate("father", 2);
        body13.args[1] = new Variable(4);
        Predicate body14 = new Predicate("mother", 2);
        body14.args[0] = new Variable(3);
        body14.args[1] = new Variable(4);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        rule1.add(body12);
        rule1.add(body13);
        rule1.add(body14);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("parent", 2);
        head2.args[0] = new Variable(0);
        head2.args[1] = new Variable(1);
        Predicate body21 = new Predicate("parent", 2);
        body21.args[0] = new Variable(0);
        body21.args[1] = new Variable(2);
        Predicate body22 = new Predicate("father", 2);
        body22.args[0] = new Variable(2);
        body22.args[1] = new Variable(4);
        Predicate body23 = new Predicate("father", 2);
        body23.args[1] = new Variable(3);
        Predicate body24 = new Predicate("mother", 2);
        body24.args[0] = new Variable(3);
        body24.args[1] = new Variable(4);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        rule2.add(body22);
        rule2.add(body23);
        rule2.add(body24);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertEquals(fp1, fp2);
        assertFalse(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }

    @Test
    void testRuleMatch7() {
        /* Negative */
        /* p(X, Y, Z) :- q(Y, Z, X) */
        /* p(Y, Z, X) :- q(X, Z, Y) */
        Predicate head1 = new Predicate("p", 3);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        head1.args[2] = new Variable(2);
        Predicate body11 = new Predicate("q", 3);
        body11.args[0] = new Variable(1);
        body11.args[1] = new Variable(2);
        body11.args[2] = new Variable(0);
        List<Predicate> rule1 = new ArrayList<>();
        rule1.add(head1);
        rule1.add(body11);
        RuleFingerPrint fp1 = new RuleFingerPrint(rule1);

        Predicate head2 = new Predicate("p", 3);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Variable(2);
        head2.args[2] = new Variable(0);
        Predicate body21 = new Predicate("q", 3);
        body21.args[0] = new Variable(0);
        body21.args[1] = new Variable(2);
        body21.args[2] = new Variable(1);
        List<Predicate> rule2 = new ArrayList<>();
        rule2.add(head2);
        rule2.add(body21);
        RuleFingerPrint fp2 = new RuleFingerPrint(rule2);

        assertNotEquals(fp1, fp2);
        assertFalse(CompareDupDetectionByBruteForce.matchRules(rule1, rule2));
    }
}