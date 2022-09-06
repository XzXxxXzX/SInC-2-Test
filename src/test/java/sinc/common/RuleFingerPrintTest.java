package sinc.common;

import org.junit.jupiter.api.Test;
import sinc.util.MultiSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleFingerPrintTest {

    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandParent";
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    final int CONST_ID = -1;

    @Test
    public void testConstruction() {
        /* h(X, c) <- p(X, Y), q(Y, Z, e), h(Z, ?), h(X, ?) */
        /* Equivalence Classes:
         * - In Head:
         *      X: {h[0], p[0], h[0]}
         *      c: {h[1], c}
         * - In Body:
         *      Y: {p[1], q[0]}
         *      Z: {q[1], h[0]}
         *      e: {q[2], e}
         *      ?: {h[1]}, {h[1]}
         */
        final Predicate head = new Predicate("h", 2);
        head.args[0] = new Variable(0);
        head.args[1] = new Constant(CONST_ID, "c");
        final Predicate body1 = new Predicate("p", 2);
        body1.args[0] = new Variable(0);
        body1.args[1] = new Variable(1);
        final Predicate body2 = new Predicate("q", 3);
        body2.args[0] = new Variable(1);
        body2.args[1] = new Variable(2);
        body2.args[2] = new Constant(CONST_ID, "e");
        final Predicate body3 = new Predicate("h", 2);
        body3.args[0] = new Variable(2);
        body3.args[1] = null;
        final Predicate body4 = new Predicate("h", 2);
        body4.args[0] = new Variable(0);
        body4.args[1] = null;
        final String actual_string = String.format(
                "%s:-%s,%s,%s,%s",
                head, body1, body2, body3, body4
        );
        final List<Predicate> rule = new ArrayList<>(Arrays.asList(
                head, body1, body2, body3, body4
        ));
        assertEquals(rule2String(rule), actual_string);

        try {
            RuleFingerPrint finger_print = new RuleFingerPrint(rule);

            String head_functor = finger_print.getHeadFunctor();
            MultiSet<ArgIndicator>[] head_equiv_classes = finger_print.getHeadEquivClasses();
            MultiSet<MultiSet<ArgIndicator>> other_equiv_classes = finger_print.getOtherEquivClasses();

            assertEquals("h", head_functor);

            MultiSet<ArgIndicator> ms_h0 = new MultiSet<>();
            ms_h0.add(new VarIndicator("h", 0));
            ms_h0.add(new VarIndicator("p", 0));
            ms_h0.add(new VarIndicator("h", 0));
            MultiSet<ArgIndicator> ms_h1 = new MultiSet<>();
            ms_h1.add(new VarIndicator("h", 1));
            ms_h1.add(new ConstIndicator("c"));
            assertArrayEquals(new MultiSet[]{ms_h0, ms_h1}, head_equiv_classes);

            MultiSet<ArgIndicator> ms_y = new MultiSet<>();
            ms_y.add(new VarIndicator("p", 1));
            ms_y.add(new VarIndicator("q", 0));
            MultiSet<ArgIndicator> ms_z = new MultiSet<>();
            ms_z.add(new VarIndicator("q", 1));
            ms_z.add(new VarIndicator("h", 0));
            MultiSet<ArgIndicator> ms_e = new MultiSet<>();
            ms_e.add(new VarIndicator("q", 2));
            ms_e.add(new ConstIndicator("e"));
            MultiSet<ArgIndicator> ms_u1 = new MultiSet<>();
            ms_u1.add(new VarIndicator("h", 1));
            MultiSet<ArgIndicator> ms_u2 = new MultiSet<>();
            ms_u2.add(new VarIndicator("h", 1));
            MultiSet<MultiSet<ArgIndicator>> expected_multi_set = new MultiSet<>();
            expected_multi_set.add(ms_y);
            expected_multi_set.add(ms_z);
            expected_multi_set.add(ms_e);
            expected_multi_set.add(ms_u1);
            expected_multi_set.add(ms_u2);
            assertEquals(expected_multi_set, other_equiv_classes);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testEquality() {
        /* R1: h(X, Y) <- h(Y, X) */
        /* R2: h(Y, X) <- h(X, Y) */
        final Predicate head1 = new Predicate("h", 2);
        head1.args[0] = new Variable(0);
        head1.args[1] = new Variable(1);
        final Predicate body11 = new Predicate("h", 2);
        body11.args[0] = new Variable(1);
        body11.args[1] = new Variable(0);
        final List<Predicate> rule1 = new ArrayList<>(Arrays.asList(head1, body11));
        assertEquals(rule2String(rule1), "h(X0,X1):-h(X1,X0)");
        final RuleFingerPrint finger_print1 = new RuleFingerPrint(rule1);

        final Predicate head2 = new Predicate("h", 2);
        head2.args[0] = new Variable(1);
        head2.args[1] = new Variable(0);
        final Predicate body21 = new Predicate("h", 2);
        body21.args[0] = new Variable(0);
        body21.args[1] = new Variable(1);
        final List<Predicate> rule2 = new ArrayList<>(Arrays.asList(head2, body21));
        assertEquals(rule2String(rule2), "h(X1,X0):-h(X0,X1)");
        final RuleFingerPrint finger_print2 = new RuleFingerPrint(rule2);

        assertEquals(finger_print1, finger_print2);
        assertEquals(finger_print2, finger_print1);

        /* R3: h(X) <- h(Y) */
        /* R4: h(X) <- h(Y), h(Z) */
        /* 当保证了没有Independent Fragment之后，这种情况实际不会发生 */
        final Predicate head3 = new Predicate("h", 1);
        final Predicate body31 = new Predicate("h", 1);
        final List<Predicate> rule3 = new ArrayList<>(Arrays.asList(head3, body31));
        assertEquals(rule2String(rule3), "h(?):-h(?)");
        final RuleFingerPrint finger_print3 = new RuleFingerPrint(rule3);

        final Predicate head4 = new Predicate("h", 1);
        final Predicate body41 = new Predicate("h", 1);
        final Predicate body42 = new Predicate("h", 1);
        final List<Predicate> rule4 = new ArrayList<>(Arrays.asList(head4, body41, body42));
        assertEquals(rule2String(rule4), "h(?):-h(?),h(?)");
        final RuleFingerPrint finger_print4 = new RuleFingerPrint(rule4);

        assertNotEquals(finger_print3, finger_print4);
        assertNotEquals(finger_print4, finger_print3);

        /* R5: h(X) <- p(X, Y) , q(Y, c) */
        /* R6: h(X) <- p(X, Y) , q(c, Y) */
        final Predicate head5 = new Predicate("h", 1);
        head5.args[0] = new Variable(0);
        final Predicate body51 = new Predicate("p", 2);
        body51.args[0] = new Variable(0);
        body51.args[1] = new Variable(1);
        final Predicate body52 = new Predicate("q", 2);
        body52.args[0] = new Variable(1);
        body52.args[1] = new Constant(CONST_ID, "c");
        final List<Predicate> rule5 = new ArrayList<>(Arrays.asList(head5, body51, body52));
        assertEquals(rule2String(rule5), "h(X0):-p(X0,X1),q(X1,c)");
        final RuleFingerPrint finger_print5 = new RuleFingerPrint(rule5);

        final Predicate head6 = new Predicate("h", 1);
        head6.args[0] = new Variable(0);
        final Predicate body61 = new Predicate("p", 2);
        body61.args[0] = new Variable(0);
        body61.args[1] = new Variable(1);
        final Predicate body62 = new Predicate("q", 2);
        body62.args[0] = new Constant(CONST_ID, "c");
        body62.args[1] = new Variable(1);
        final List<Predicate> rule6 = new ArrayList<>(Arrays.asList(head6, body61, body62));
        assertEquals(rule2String(rule6), "h(X0):-p(X0,X1),q(c,X1)");
        final RuleFingerPrint finger_print6 = new RuleFingerPrint(rule6);

        assertNotEquals(finger_print5, finger_print6);
        assertNotEquals(finger_print6, finger_print5);
    }

    @Test
    public void test2() {
        /* #1: grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        final Predicate p11 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p11.args[0] = new Variable(0);
        p11.args[1] = new Variable(2);
        final Predicate p12 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p12.args[0] = new Variable(0);
        p12.args[1] = new Variable(1);
        final Predicate p13 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p13.args[0] = new Variable(1);
        p13.args[1] = new Variable(2);
        final List<Predicate> rule1 = new ArrayList<>(Arrays.asList(p11, p12, p13));
        final RuleFingerPrint finger_print1 = new RuleFingerPrint(rule1);

        /* #2: grandParent(Y, X) :- father(Z, X), parent(Y, Z) */
        final Predicate p21 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p21.args[0] = new Variable(1);
        p21.args[1] = new Variable(0);
        final Predicate p22 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p22.args[0] = new Variable(2);
        p22.args[1] = new Variable(0);
        final Predicate p23 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p23.args[0] = new Variable(1);
        p23.args[1] = new Variable(2);
        final List<Predicate> rule2 = new ArrayList<>(Arrays.asList(p21, p22, p23));
        final RuleFingerPrint finger_print2 = new RuleFingerPrint(rule2);

        /* #3: grandParent(Y, X) :- father(Z, X), parent(Z, Y) */
        final Predicate p31 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p31.args[0] = new Variable(1);
        p31.args[1] = new Variable(0);
        final Predicate p32 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p32.args[0] = new Variable(2);
        p32.args[1] = new Variable(0);
        final Predicate p33 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p33.args[0] = new Variable(2);
        p33.args[1] = new Variable(1);
        final List<Predicate> rule3 = new ArrayList<>(Arrays.asList(p31, p32, p33));
        final RuleFingerPrint finger_print3 = new RuleFingerPrint(rule3);

        /* #4: grandParent(Y, X) :- father(?, X), parent(Y, ?) */
        final Predicate p41 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        p41.args[0] = new Variable(1);
        p41.args[1] = new Variable(0);
        final Predicate p42 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        p42.args[0] = null;
        p42.args[1] = new Variable(0);
        final Predicate p43 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        p43.args[0] = new Variable(1);
        p43.args[1] = null;
        final List<Predicate> rule4 = new ArrayList<>(Arrays.asList(p41, p42, p43));
        final RuleFingerPrint finger_print4 = new RuleFingerPrint(rule4);

        assertEquals(finger_print1, finger_print2);
        assertNotEquals(finger_print1, finger_print3);
        assertNotEquals(finger_print1, finger_print4);
        assertNotEquals(finger_print3, finger_print4);
    }

    private String rule2String(List<Predicate> rule) {
        StringBuilder builder = new StringBuilder();
        builder.append(rule.get(0).toString()).append(":-");
        if (1 < rule.size()) {
            builder.append(rule.get(1));
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',');
                builder.append(rule.get(i).toString());
            }
        }
        return builder.toString();
    }
}