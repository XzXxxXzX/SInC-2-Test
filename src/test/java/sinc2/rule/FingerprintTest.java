package sinc2.rule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.ArgIndicator;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.kb.NumeratedKb;
import sinc2.kb.NumerationMap;
import sinc2.util.MultiSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintTest {

    static final String FUNCTOR_H = "h";
    static final String FUNCTOR_P = "p";
    static final String FUNCTOR_Q = "q";
    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandparent";
    static final String CONST_C = "c";
    static final String CONST_E = "e";
    
    static final int NUM_H = 1;
    static final int NUM_P = 2;
    static final int NUM_Q = 3;
    static final int NUM_FATHER = 4;
    static final int NUM_PARENT = 5;
    static final int NUM_GRANDPARENT = 6;
    static final int NUM_C = 7;
    static final int NUM_E = 8;
    
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    static final NumerationMap map = new NumerationMap();
    
    @BeforeAll
    static void createNumerationMap() {
        assertEquals(NUM_H, map.mapName(FUNCTOR_H));
        assertEquals(NUM_P, map.mapName(FUNCTOR_P));
        assertEquals(NUM_Q, map.mapName(FUNCTOR_Q));
        assertEquals(NUM_FATHER, map.mapName(FUNCTOR_FATHER));
        assertEquals(NUM_PARENT, map.mapName(FUNCTOR_PARENT));
        assertEquals(NUM_GRANDPARENT, map.mapName(FUNCTOR_GRANDPARENT));
        assertEquals(NUM_C, map.mapName(CONST_C));
        assertEquals(NUM_E, map.mapName(CONST_E));
    }

    @Test
    public void testConstruction() {
        /* h(X, c) <- p(X, Y), q(Y, Z, e), h(Z, ?), h(X, ?) */
        /* Equivalence Classes:
         * - In Head:
         *      X: {h[0], p[0], h[0]}   [0]
         *      c: {h[1], c}            [1]
         * - In Body:
         *      Y: {p[1], q[0]}
         *      Z: {q[1], h[0]}
         *      e: {q[2], e}
         *      ?: {h[1]}, {h[1]}
         */
        final Predicate head = new Predicate(NUM_H, 2);
        head.args[0] = Argument.variable(0);
        head.args[1] = Argument.constant(NUM_C);
        final Predicate body1 = new Predicate(NUM_P, 2);
        body1.args[0] = Argument.variable(0);
        body1.args[1] = Argument.variable(1);
        final Predicate body2 = new Predicate(NUM_Q, 3);
        body2.args[0] = Argument.variable(1);
        body2.args[1] = Argument.variable(2);
        body2.args[2] = Argument.constant(NUM_E);
        final Predicate body3 = new Predicate(NUM_H, 2);
        body3.args[0] = Argument.variable(2);
        body3.args[1] = Argument.EMPTY_VALUE;
        final Predicate body4 = new Predicate(NUM_H, 2);
        body4.args[0] = Argument.variable(0);
        body4.args[1] = Argument.EMPTY_VALUE;
        final List<Predicate> rule = List.of(head, body1, body2, body3, body4);
        assertEquals("h(X0,c):-p(X0,X1),q(X1,X2,e),h(X2,?),h(X0,?)", rule2String(rule));

        Fingerprint.LabeledEquivalenceClass lec_x = new Fingerprint.LabeledEquivalenceClass();
        lec_x.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 0));
        lec_x.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_P, 0));
        lec_x.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 0));
        lec_x.addLabel(0);
        Fingerprint.LabeledEquivalenceClass lec_c = new Fingerprint.LabeledEquivalenceClass();
        lec_c.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 1));
        lec_c.addArgIndicator(ArgIndicator.getConstantIndicator(NUM_C));
        lec_c.addLabel(1);
        Fingerprint.LabeledEquivalenceClass lec_y = new Fingerprint.LabeledEquivalenceClass();
        lec_y.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_P, 1));
        lec_y.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_Q, 0));
        Fingerprint.LabeledEquivalenceClass lec_z = new Fingerprint.LabeledEquivalenceClass();
        lec_z.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_Q, 1));
        lec_z.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 0));
        Fingerprint.LabeledEquivalenceClass lec_e = new Fingerprint.LabeledEquivalenceClass();
        lec_e.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_Q, 2));
        lec_e.addArgIndicator(ArgIndicator.getConstantIndicator(NUM_E));
        Fingerprint.LabeledEquivalenceClass lec_uv1 = new Fingerprint.LabeledEquivalenceClass();
        lec_uv1.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 1));
        Fingerprint.LabeledEquivalenceClass lec_uv2 = new Fingerprint.LabeledEquivalenceClass();
        lec_uv2.addArgIndicator(ArgIndicator.getVariableIndicator(NUM_H, 1));
        MultiSet<Fingerprint.LabeledEquivalenceClass> expected_lec_set = new MultiSet<>(
                new Fingerprint.LabeledEquivalenceClass[]{lec_x, lec_y, lec_z, lec_c, lec_e, lec_uv1, lec_uv2}
        );

        Fingerprint fingerprint = new Fingerprint(rule);
        assertEquals(NUM_H, fingerprint.getHeadFunctor());
        assertEquals(expected_lec_set, fingerprint.getLabeledEquivalenceClasses());
    }

    @Test
    public void testEquality1() {
        /* R1: h(X, Y) <- h(Y, X) */
        /* R2: h(Y, X) <- h(X, Y) */
        final Predicate head1 = new Predicate(NUM_H, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.variable(1);
        final Predicate body11 = new Predicate(NUM_H, 2);
        body11.args[0] = Argument.variable(1);
        body11.args[1] = Argument.variable(0);
        final List<Predicate> rule1 = List.of(head1, body11);
        assertEquals("h(X0,X1):-h(X1,X0)", rule2String(rule1));
        final Fingerprint finger_print1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_H, 2);
        head2.args[0] = Argument.variable(1);
        head2.args[1] = Argument.variable(0);
        final Predicate body21 = new Predicate(NUM_H, 2);
        body21.args[0] = Argument.variable(0);
        body21.args[1] = Argument.variable(1);
        final List<Predicate> rule2 = List.of(head2, body21);
        assertEquals("h(X1,X0):-h(X0,X1)", rule2String(rule2));
        final Fingerprint finger_print2 = new Fingerprint(rule2);

        assertEquals(finger_print1, finger_print2);
        assertEquals(finger_print2, finger_print1);
    }

    @Test
    void testEquality2() {
        /* R3: h(X) <- h(Y) */
        /* R4: h(X) <- h(Y), h(Z) */
        /* If no independent fragment is introduced in the search, this will not happen in real world cases */
        final Predicate head3 = new Predicate(NUM_H, 1);
        final Predicate body31 = new Predicate(NUM_H, 1);
        final List<Predicate> rule3 = List.of(head3, body31);
        assertEquals("h(?):-h(?)", rule2String(rule3));
        final Fingerprint finger_print3 = new Fingerprint(rule3);

        final Predicate head4 = new Predicate(NUM_H, 1);
        final Predicate body41 = new Predicate(NUM_H, 1);
        final Predicate body42 = new Predicate(NUM_H, 1);
        final List<Predicate> rule4 = List.of(head4, body41, body42);
        assertEquals("h(?):-h(?),h(?)", rule2String(rule4));
        final Fingerprint finger_print4 = new Fingerprint(rule4);

        assertNotEquals(finger_print3, finger_print4);
        assertNotEquals(finger_print4, finger_print3);
    }

    @Test
    void testEquality3() {
        /* R5: h(X) <- p(X, Y) , q(Y, c) */
        /* R6: h(X) <- p(X, Y) , q(c, Y) */
        final Predicate head5 = new Predicate(NUM_H, 1);
        head5.args[0] = Argument.variable(0);
        final Predicate body51 = new Predicate(NUM_P, 2);
        body51.args[0] = Argument.variable(0);
        body51.args[1] = Argument.variable(1);
        final Predicate body52 = new Predicate(NUM_Q, 2);
        body52.args[0] = Argument.variable(1);
        body52.args[1] = Argument.constant(NUM_C);
        final List<Predicate> rule5 = List.of(head5, body51, body52);
        assertEquals("h(X0):-p(X0,X1),q(X1,c)", rule2String(rule5));
        final Fingerprint finger_print5 = new Fingerprint(rule5);

        final Predicate head6 = new Predicate(NUM_H, 1);
        head6.args[0] = Argument.variable(0);
        final Predicate body61 = new Predicate(NUM_P, 2);
        body61.args[0] = Argument.variable(0);
        body61.args[1] = Argument.variable(1);
        final Predicate body62 = new Predicate(NUM_Q, 2);
        body62.args[0] = Argument.constant(NUM_C);
        body62.args[1] = Argument.variable(1);
        final List<Predicate> rule6 = List.of(head6, body61, body62);
        assertEquals("h(X0):-p(X0,X1),q(c,X1)", rule2String(rule6));
        final Fingerprint finger_print6 = new Fingerprint(rule6);

        assertNotEquals(finger_print5, finger_print6);
        assertNotEquals(finger_print6, finger_print5);
    }

    @Test
    void testFalsePositive1() {
        /* #1: p(X, Y) :- q(X, X), q(?, Y) */
        /* #2: p(X, Y) :- q(X, Y), q(?, X) */
        /* Current fingerprint structure will incorrectly report the two rules as identical */
        final Predicate head1 = new Predicate(NUM_P, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.variable(1);
        final Predicate body11 = new Predicate(NUM_Q, 2);
        body11.args[0] = Argument.variable(0);
        body11.args[1] = Argument.variable(0);
        final Predicate body12 = new Predicate(NUM_Q, 2);
        body12.args[0] = Argument.EMPTY_VALUE;
        body12.args[1] = Argument.variable(1);
        List<Predicate> rule1 = List.of(head1, body11, body12);
        assertEquals("p(X0,X1):-q(X0,X0),q(?,X1)", rule2String(rule1));
        final Fingerprint fingerprint1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_P, 2);
        head2.args[0] = Argument.variable(0);
        head2.args[1] = Argument.variable(1);
        final Predicate body21 = new Predicate(NUM_Q, 2);
        body21.args[0] = Argument.variable(0);
        body21.args[1] = Argument.variable(1);
        final Predicate body22 = new Predicate(NUM_Q, 2);
        body22.args[0] = Argument.EMPTY_VALUE;
        body22.args[1] = Argument.variable(0);
        List<Predicate> rule2 = List.of(head2, body21, body22);
        assertEquals("p(X0,X1):-q(X0,X1),q(?,X0)", rule2String(rule2));
        final Fingerprint fingerprint2 = new Fingerprint(rule2);

        assertEquals(fingerprint1, fingerprint2);
        assertEquals(fingerprint2, fingerprint1);
    }

    @Test
    void testFalsePositive2() {
        /* #1: p(X, Y) :- q(X, ?), q(Z, Y), q(?, Z) */
        /* #2: p(X, Y) :- q(X, Z), q(?, Y), q(Z, ?) */
        /* Current fingerprint structure will incorrectly report the two rules as identical */
        final Predicate head1 = new Predicate(NUM_P, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.variable(1);
        final Predicate body11 = new Predicate(NUM_Q, 2);
        body11.args[0] = Argument.variable(0);
        body11.args[1] = Argument.EMPTY_VALUE;
        final Predicate body12 = new Predicate(NUM_Q, 2);
        body12.args[0] = Argument.variable(2);
        body12.args[1] = Argument.variable(1);
        final Predicate body13 = new Predicate(NUM_Q, 2);
        body13.args[0] = Argument.EMPTY_VALUE;
        body13.args[1] = Argument.variable(2);
        List<Predicate> rule1 = List.of(head1, body11, body12, body13);
        assertEquals("p(X0,X1):-q(X0,?),q(X2,X1),q(?,X2)", rule2String(rule1));
        final Fingerprint fingerprint1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_P, 2);
        head2.args[0] = Argument.variable(0);
        head2.args[1] = Argument.variable(1);
        final Predicate body21 = new Predicate(NUM_Q, 2);
        body21.args[0] = Argument.variable(0);
        body21.args[1] = Argument.variable(2);
        final Predicate body22 = new Predicate(NUM_Q, 2);
        body22.args[0] = Argument.EMPTY_VALUE;
        body22.args[1] = Argument.variable(1);
        final Predicate body23 = new Predicate(NUM_Q, 2);
        body23.args[0] = Argument.variable(2);
        body23.args[1] = Argument.EMPTY_VALUE;
        List<Predicate> rule2 = List.of(head2, body21, body22, body23);
        assertEquals("p(X0,X1):-q(X0,X2),q(?,X1),q(X2,?)", rule2String(rule2));
        final Fingerprint fingerprint2 = new Fingerprint(rule2);

        assertEquals(fingerprint1, fingerprint2);
        assertEquals(fingerprint2, fingerprint1);
    }

    @Test
    public void test2() {
        /* #1: grandparent(X, Z) :- parent(X, Y), father(Y, Z) */
        final Predicate p11 = new Predicate(NUM_GRANDPARENT, ARITY_GRANDPARENT);
        p11.args[0] = Argument.variable(0);
        p11.args[1] = Argument.variable(2);
        final Predicate p12 = new Predicate(NUM_PARENT, ARITY_PARENT);
        p12.args[0] = Argument.variable(0);
        p12.args[1] = Argument.variable(1);
        final Predicate p13 = new Predicate(NUM_FATHER, ARITY_FATHER);
        p13.args[0] = Argument.variable(1);
        p13.args[1] = Argument.variable(2);
        final List<Predicate> rule1 = List.of(p11, p12, p13);
        assertEquals("grandparent(X0,X2):-parent(X0,X1),father(X1,X2)", rule2String(rule1));
        final Fingerprint finger_print1 = new Fingerprint(rule1);

        /* #2: grandparent(Y, X) :- father(Z, X), parent(Y, Z) */
        final Predicate p21 = new Predicate(NUM_GRANDPARENT, ARITY_GRANDPARENT);
        p21.args[0] = Argument.variable(1);
        p21.args[1] = Argument.variable(0);
        final Predicate p22 = new Predicate(NUM_FATHER, ARITY_FATHER);
        p22.args[0] = Argument.variable(2);
        p22.args[1] = Argument.variable(0);
        final Predicate p23 = new Predicate(NUM_PARENT, ARITY_PARENT);
        p23.args[0] = Argument.variable(1);
        p23.args[1] = Argument.variable(2);
        final List<Predicate> rule2 = List.of(p21, p22, p23);
        assertEquals("grandparent(X1,X0):-father(X2,X0),parent(X1,X2)", rule2String(rule2));
        final Fingerprint finger_print2 = new Fingerprint(rule2);

        /* #3: grandparent(Y, X) :- father(Z, X), parent(Z, Y) */
        final Predicate p31 = new Predicate(NUM_GRANDPARENT, ARITY_GRANDPARENT);
        p31.args[0] = Argument.variable(1);
        p31.args[1] = Argument.variable(0);
        final Predicate p32 = new Predicate(NUM_FATHER, ARITY_FATHER);
        p32.args[0] = Argument.variable(2);
        p32.args[1] = Argument.variable(0);
        final Predicate p33 = new Predicate(NUM_PARENT, ARITY_PARENT);
        p33.args[0] = Argument.variable(2);
        p33.args[1] = Argument.variable(1);
        final List<Predicate> rule3 = List.of(p31, p32, p33);
        assertEquals("grandparent(X1,X0):-father(X2,X0),parent(X2,X1)", rule2String(rule3));
        final Fingerprint finger_print3 = new Fingerprint(rule3);

        /* #4: grandparent(Y, X) :- father(?, X), parent(Y, ?) */
        final Predicate p41 = new Predicate(NUM_GRANDPARENT, ARITY_GRANDPARENT);
        p41.args[0] = Argument.variable(1);
        p41.args[1] = Argument.variable(0);
        final Predicate p42 = new Predicate(NUM_FATHER, ARITY_FATHER);
        p42.args[0] = Argument.EMPTY_VALUE;
        p42.args[1] = Argument.variable(0);
        final Predicate p43 = new Predicate(NUM_PARENT, ARITY_PARENT);
        p43.args[0] = Argument.variable(1);
        p43.args[1] = Argument.EMPTY_VALUE;
        final List<Predicate> rule4 = List.of(p41, p42, p43);
        assertEquals("grandparent(X1,X0):-father(?,X0),parent(X1,?)", rule2String(rule4));
        final Fingerprint finger_print4 = new Fingerprint(rule4);

        assertEquals(finger_print1, finger_print2);
        assertNotEquals(finger_print1, finger_print3);
        assertNotEquals(finger_print1, finger_print4);
        assertNotEquals(finger_print3, finger_print4);
    }

    @Test
    void testGeneralizationOf1() {
        /* #1: p(X, ?) :- q(?, X) */
        /* #2: p(X, Y) :- q(Y, X) */
        final Predicate head1 = new Predicate(NUM_P, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.EMPTY_VALUE;
        final Predicate body1 = new Predicate(NUM_Q, 2);
        body1.args[0] = Argument.EMPTY_VALUE;
        body1.args[1] = Argument.variable(0);
        final List<Predicate> rule1 = List.of(head1, body1);
        assertEquals("p(X0,?):-q(?,X0)", rule2String(rule1));
        final Fingerprint fingerprint1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_P, 2);
        head2.args[0] = Argument.variable(0);
        head2.args[1] = Argument.variable(1);
        final Predicate body2 = new Predicate(NUM_Q, 2);
        body2.args[0] = Argument.variable(1);
        body2.args[1] = Argument.variable(0);
        final List<Predicate> rule2 = List.of(head2, body2);
        assertEquals("p(X0,X1):-q(X1,X0)", rule2String(rule2));
        final Fingerprint fingerprint2 = new Fingerprint(rule2);

        assertTrue(fingerprint1.generalizationOf(fingerprint2));
        assertFalse(fingerprint2.generalizationOf(fingerprint1));
    }

    @Test
    void testGeneralizationOf2() {
        /* #1: p(X, ?) :- q(?, X) */
        /* #2: p(X, ?) :- q(Y, X), p(Y, ?) */
        final Predicate head1 = new Predicate(NUM_P, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.EMPTY_VALUE;
        final Predicate body1 = new Predicate(NUM_Q, 2);
        body1.args[0] = Argument.EMPTY_VALUE;
        body1.args[1] = Argument.variable(0);
        final List<Predicate> rule1 = List.of(head1, body1);
        assertEquals("p(X0,?):-q(?,X0)", rule2String(rule1));
        final Fingerprint fingerprint1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_P, 2);
        head2.args[0] = Argument.variable(0);
        head2.args[1] = Argument.EMPTY_VALUE;
        final Predicate body21 = new Predicate(NUM_Q, 2);
        body21.args[0] = Argument.variable(1);
        body21.args[1] = Argument.variable(0);
        final Predicate body22 = new Predicate(NUM_P, 2);
        body22.args[0] = Argument.variable(1);
        body22.args[1] = Argument.EMPTY_VALUE;
        final List<Predicate> rule2 = List.of(head2, body21, body22);
        assertEquals("p(X0,?):-q(X1,X0),p(X1,?)", rule2String(rule2));
        final Fingerprint fingerprint2 = new Fingerprint(rule2);

        assertTrue(fingerprint1.generalizationOf(fingerprint2));
        assertFalse(fingerprint2.generalizationOf(fingerprint1));
    }

    @Test
    void testGeneralizationOf3() {
        /* #1: p(X, ?) :- q(?, X) */
        /* #2: p(X, ?) :- q(c, X) */
        final Predicate head1 = new Predicate(NUM_P, 2);
        head1.args[0] = Argument.variable(0);
        head1.args[1] = Argument.EMPTY_VALUE;
        final Predicate body1 = new Predicate(NUM_Q, 2);
        body1.args[0] = Argument.EMPTY_VALUE;
        body1.args[1] = Argument.variable(0);
        final List<Predicate> rule1 = List.of(head1, body1);
        assertEquals("p(X0,?):-q(?,X0)", rule2String(rule1));
        final Fingerprint fingerprint1 = new Fingerprint(rule1);

        final Predicate head2 = new Predicate(NUM_P, 2);
        head2.args[0] = Argument.variable(0);
        head2.args[1] = Argument.EMPTY_VALUE;
        final Predicate body2 = new Predicate(NUM_Q, 2);
        body2.args[0] = Argument.constant(NUM_C);
        body2.args[1] = Argument.variable(0);
        final List<Predicate> rule2 = List.of(head2, body2);
        assertEquals("p(X0,?):-q(c,X0)", rule2String(rule2));
        final Fingerprint fingerprint2 = new Fingerprint(rule2);

        assertTrue(fingerprint1.generalizationOf(fingerprint2));
        assertFalse(fingerprint2.generalizationOf(fingerprint1));
    }

    @Test
    void testRules() {
        String[] rule_strs = new String[]{
                "family(?,X0,X1):-father(X0,X1),isMale(X1)",
                "family(X1,?,X0):-father(?,X0),mother(X1,?),isMale(X0)",
                "family(X1,X0,?):-father(X0,X2),mother(X1,?),isMale(X2)",
                "family(X1,X0,X2):-father(X0,?),mother(X1,?),isMale(X2)",
                "family(X1,X0,X2):-father(X0,X2),mother(X1,?)"
        };
        NumeratedKb kb = new NumeratedKb("test");
        kb.mapName("family");
        kb.mapName("father");
        kb.mapName("mother");
        kb.mapName("isMale");

        Rule rule0 = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        rule0.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        rule0.cvt2Uvs2NewLv(kb.name2Num("isMale"), 1, 0, 0, 2);
        rule0.cvt1Uv2ExtLv(1, 1, 1);
        assertEquals(rule_strs[0], rule0.toDumpString(kb.getNumerationMap()));

        Rule rule1 = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        rule1.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 1, 0, 2);
        rule1.cvt2Uvs2NewLv(kb.name2Num("mother"), 2, 0, 0, 0);
        rule1.cvt1Uv2ExtLv(kb.name2Num("isMale"), 1, 0, 0);
        assertEquals(rule_strs[1], rule1.toDumpString(kb.getNumerationMap()));

        Rule rule2 = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        rule2.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        rule2.cvt2Uvs2NewLv(kb.name2Num("mother"), 2, 0, 0, 0);
        rule2.cvt2Uvs2NewLv(kb.name2Num("isMale"), 1, 0, 1, 1);
        assertEquals(rule_strs[2], rule2.toDumpString(kb.getNumerationMap()));

        Rule rule3 = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        rule3.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        rule3.cvt2Uvs2NewLv(kb.name2Num("mother"), 2, 0, 0, 0);
        rule3.cvt2Uvs2NewLv(kb.name2Num("isMale"), 1, 0, 0, 2);
        assertEquals(rule_strs[3], rule3.toDumpString(kb.getNumerationMap()));

        Rule rule4 = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        rule4.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        rule4.cvt2Uvs2NewLv(kb.name2Num("mother"), 2, 0, 0, 0);
        rule4.cvt2Uvs2NewLv(0, 2, 1, 1);
        assertEquals(rule_strs[4], rule4.toDumpString(kb.getNumerationMap()));

        Rule[] rules = new Rule[]{rule0, rule1, rule2, rule3, rule4};
        for (int i = 0; i < rules.length; i++) {
            for (int j = i + 1; j < rules.length; j++) {
                assertNotEquals(rules[i], rules[j]);
            }
        }
    }

    private String rule2String(List<Predicate> rule) {
        StringBuilder builder = new StringBuilder();
        builder.append(rule.get(0).toString(map)).append(":-");
        if (1 < rule.size()) {
            builder.append(rule.get(1).toString(map));
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',');
                builder.append(rule.get(i).toString(map));
            }
        }
        return builder.toString();
    }
}