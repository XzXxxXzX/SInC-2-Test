package sinc;

import org.junit.jupiter.api.Test;
import sinc.common.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SincRecoveryTest {

    static class RuleImpl extends Rule {

        public RuleImpl(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
            super(headFunctor, arity, searchedFingerprints);
        }

        public RuleImpl(Rule another) {
            super(another);
        }

        @Override
        public Rule clone() {
            return null;
        }

        @Override
        protected double factCoverage() {
            return 1.0;
        }

        @Override
        protected Eval calculateEval() {
            return null;
        }
    }

    @Test
    void test1() {
        /* h(?, ?) :- */
        final Predicate h1 = new Predicate("h", 2);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Set<Predicate> reduced_facts = new HashSet<>(Collections.singletonList(h1));
        final Set<Predicate> counter_examples = new HashSet<>();
        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
        for (String constant: all_constants) {
            if ("d".equals(constant)) {
                continue;
            }
            final Predicate ce = new Predicate("h", 2);
            ce.args[0] = new Constant(Rule.CONSTANT_ARG_ID, constant);
            ce.args[1] = new Constant(Rule.CONSTANT_ARG_ID, constant);
            counter_examples.add(ce);
        }

        Rule r = new RuleImpl("h", 2, new HashSet<>());
        assertTrue(r.toString().contains("h(?,?):-"));

        final Set<Predicate> expected_recovery = new HashSet<>();
        for (String c1: all_constants) {
            for (String c2: all_constants) {
                if (c1.equals(c2)) {
                    continue;
                }
                final Predicate p = new Predicate("h", 2);
                p.args[0] = new Constant(Rule.CONSTANT_ARG_ID, c1);
                p.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c2);
                expected_recovery.add(p);
            }
        }
        final Predicate h2 = new Predicate("h", 2);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "d");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "d");
        expected_recovery.add(h2);

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, counter_examples, new HashSet<>(Collections.singleton("d"))
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test2() {
        /* h(X, X) :- */
        final Predicate h = new Predicate("h", 2);
        h.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(h, p1));
        final Set<Predicate> counter_examples = new HashSet<>();
        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
        for (String c1: all_constants) {
            for (String c2: all_constants) {
                if (c1.equals(c2)) {
                    continue;
                }
                final Predicate p = new Predicate("h", 2);
                p.args[0] = new Constant(Rule.CONSTANT_ARG_ID, c1);
                p.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c2);
                counter_examples.add(p);
            }
        }
        counter_examples.remove(h);

        Rule r = new RuleImpl("h", 2, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(0, 0, 0, 1));
        assertTrue(r.toString().contains("h(X0,X0):-"));

        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(h, p1));
        for (String c: all_constants) {
            final Predicate p = new Predicate("h", 2);
            p.args[0] = new Constant(Rule.CONSTANT_ARG_ID, c);
            p.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c);
            expected_recovery.add(p);
        }

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, counter_examples, new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test3() {
        /* h(X, Y) :- p(X, Y) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "c");

        final Predicate h1 = new Predicate("h", 2);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate h2 = new Predicate("h", 2);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate h3 = new Predicate("h", 2);
        h3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        final Predicate h4 = new Predicate("h", 2);
        h4.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        h4.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3, h1, h2, h3, h4
        ));

        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "c"));
        final Set<Predicate> counter_examples = new HashSet<>();
        for (String c: all_constants) {
            final Predicate ce = new Predicate("h", 2);
            ce.args[0] = new Constant(Rule.CONSTANT_ARG_ID, c);
            ce.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c);
            counter_examples.add(ce);
        }

        Rule r = new RuleImpl("h", 2, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(0, 1, 1, 1));
        assertTrue(r.toString().contains("h(X0,X1):-p(X0,X1)"));

        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3, h1, h2, h3, h4
        ));
        for (String c1: all_constants) {
            for (String c2: all_constants) {
                if (c1.equals(c2)) {
                    continue;
                }
                final Predicate h = new Predicate("h", 2);
                h.args[0] = new Constant(Rule.CONSTANT_ARG_ID, c1);
                h.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c2);
                expected_recovery.add(h);
            }
        }

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, counter_examples, new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test4() {
        /* h(X, Y) :- p(X, Z), q(Z, Y) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");

        final Predicate q1 = new Predicate("q", 2);
        q1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        q1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate q2 = new Predicate("q", 2);
        q2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        q2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate q3 = new Predicate("q", 2);
        q3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        q3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3, q1, q2, q3
        ));

        Rule r = new RuleImpl("h", 2, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(1, 1, 2, 0));
        assertTrue(r.toString().contains("h(X0,X1):-p(X0,X2),q(X2,X1)"));

        final Predicate h1 = new Predicate("h", 2);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate h2 = new Predicate("h", 2);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate h3 = new Predicate("h", 2);
        h3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate h4 = new Predicate("h", 2);
        h4.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h4.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                h1, h2, h3, h4, p1, p2, p3, q1, q2, q3
        ));

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test5() {
        /* h(X, Y, ?) :- p(X, Z), q(Z, Y) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");

        final Predicate q1 = new Predicate("q", 2);
        q1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        q1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate q2 = new Predicate("q", 2);
        q2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        q2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate q3 = new Predicate("q", 2);
        q3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        q3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3, q1, q2, q3
        ));

        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "A", "B", "c"));

        Rule r = new RuleImpl("h", 3, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(1, 1, 2, 0));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0,X2),q(X2,X1)"));

        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3, q1, q2, q3
        ));
        for (String c3: all_constants) {
            final Predicate h1 = new Predicate("h", 3);
            h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, c3);
            final Predicate h2 = new Predicate("h", 3);
            h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
            h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
            h2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, c3);
            final Predicate h3 = new Predicate("h", 3);
            h3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
            h3.args[2] = new Constant(Rule.CONSTANT_ARG_ID, c3);
            final Predicate h4 = new Predicate("h", 3);
            h4.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
            h4.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h4.args[2] = new Constant(Rule.CONSTANT_ARG_ID, c3);
            expected_recovery.add(h1);
            expected_recovery.add(h2);
            expected_recovery.add(h3);
            expected_recovery.add(h4);
        }

        final Set<String> delta_constants = new HashSet<>(Collections.singleton("c"));

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), delta_constants
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test6() {
        /* h(X, ?) :- p(X, ?) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3
        ));

        Rule r = new RuleImpl("h", 2, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertTrue(r.toString().contains("h(X0,?):-p(X0,?)"));

        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3
        ));
        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "A", "B", "c"));
        for (String c: all_constants) {
            final Predicate h1 = new Predicate("h", 2);
            h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c);
            final Predicate h2 = new Predicate("h", 2);
            h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
            h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c);
            expected_recovery.add(h1);
            expected_recovery.add(h2);
        }

        final Set<String> delta_constants = new HashSet<>(Collections.singleton("c"));

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), delta_constants
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test7() {
        /* h(X, ?, *) :- p(X, ?), q(X, +) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");

        final Predicate q1 = new Predicate("q", 2);
        q1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        q1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "+");
        final Predicate q2 = new Predicate("q", 2);
        q2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        q2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "-");
        final Predicate q3 = new Predicate("q", 2);
        q3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        q3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "-");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3, q1, q2, q3
        ));

        Rule r = new RuleImpl("h", 3, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar("q", 2, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(0, 2,"*"));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(2, 1,"+"));
        assertTrue(r.toString().contains("h(X0,?,*):-p(X0,?),q(X0,+)"));

        final Set<String> all_constants = new HashSet<>(Arrays.asList("a", "b", "A", "B", "+", "-", "*"));
        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3, q1, q2, q3
        ));
        for (String c: all_constants) {
            final Predicate h = new Predicate("h", 3);
            h.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
            h.args[1] = new Constant(Rule.CONSTANT_ARG_ID, c);
            h.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "*");
            expected_recovery.add(h);
        }

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test8() {
        /* h(X, X, Y, Y) :- p(X, Y, +) */
        final Predicate p1 = new Predicate("p", 3);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        p1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "+");
        final Predicate p2 = new Predicate("p", 3);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        p2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "+");
        final Predicate p3 = new Predicate("p", 3);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "C");
        p3.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "-");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3
        ));

        Rule r = new RuleImpl("h", 4, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 3, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(0, 2, 1, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar(0, 1, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar(0, 3, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(1, 2, "+"));
        assertTrue(r.toString().contains("h(X0,X0,X1,X1):-p(X0,X1,+)"));

        final Predicate h1 = new Predicate("h", 4);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        h1.args[3] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate h2 = new Predicate("h", 4);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        h2.args[3] = new Constant(Rule.CONSTANT_ARG_ID, "B");
        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3, h1, h2
        ));

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }

    @Test
    void test9() {
        /* h(X) :- p(X, X), q(X) */
        final Predicate p1 = new Predicate("p", 2);
        p1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        p1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate p2 = new Predicate("p", 2);
        p2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        p2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate p3 = new Predicate("p", 2);
        p3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        p3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "A");
        final Predicate p4 = new Predicate("p", 2);
        p4.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        p4.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final Predicate q1 = new Predicate("q", 1);
        q1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate q2 = new Predicate("q", 1);
        q2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final Set<Predicate> reduced_facts = new HashSet<>(Arrays.asList(
                p1, p2, p3, p4, q1, q2
        ));

        Rule r = new RuleImpl("h", 1, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar("q", 1, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar(1, 1, 0));
        assertTrue(r.toString().contains("h(X0):-p(X0,X0),q(X0)"));

        final Predicate h1 = new Predicate("h", 1);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Set<Predicate> expected_recovery = new HashSet<>(Arrays.asList(
                p1, p2, p3, p4, q1, q2, h1
        ));

        SincRecovery recovery = new SincRecovery(
                new ArrayList<>(Collections.singleton(r)), reduced_facts, new HashSet<>(), new HashSet<>()
        );
        assertEquals(expected_recovery, recovery.recover());
    }
}