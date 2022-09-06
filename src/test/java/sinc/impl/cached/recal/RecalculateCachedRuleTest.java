package sinc.impl.cached.recal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sinc.common.*;
import sinc.impl.cached.MemKB;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RecalculateCachedRuleTest {

    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandParent";
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    static final int CONST_ID = -1;

    static MemKB kbFamily() {
        final MemKB kb = new MemKB();

        /* father(X, Y):
         *   f1, s1
         *   f2, s2
         *   f2, d2
         *   f3, s3
         *   f4, d4
         */
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate father4 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father4.args[0] = new Constant(CONST_ID, "f3");
        father4.args[1] = new Constant(CONST_ID, "s3");
        Predicate father5 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father5.args[0] = new Constant(CONST_ID, "f4");
        father5.args[1] = new Constant(CONST_ID, "d4");
        kb.addFact(father1);
        kb.addFact(father2);
        kb.addFact(father3);
        kb.addFact(father4);
        kb.addFact(father5);

        /* parent(X, Y):
         *   f1, s1
         *   f1, d1
         *   f2, s2
         *   f2, d2
         *   m2, d2
         *   g1, f1
         *   g2, f2
         *   g2, m2
         *   g3, f3
         */
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate parent9 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent9.args[0] = new Constant(CONST_ID, "g3");
        parent9.args[1] = new Constant(CONST_ID, "f3");
        kb.addFact(parent1);
        kb.addFact(parent2);
        kb.addFact(parent3);
        kb.addFact(parent4);
        kb.addFact(parent5);
        kb.addFact(parent6);
        kb.addFact(parent7);
        kb.addFact(parent8);
        kb.addFact(parent9);

        /* grandParent(X, Y):
         *   g1, s1
         *   g2, d2
         *   g4, s4
         */
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand3 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand3.args[0] = new Constant(CONST_ID, "g4");
        grand3.args[1] = new Constant(CONST_ID, "s4");
        kb.addFact(grand1);
        kb.addFact(grand2);
        kb.addFact(grand3);

        /* Constants(16):
         *   g1, g2, g3, g4
         *   f1, f2, f3, f4
         *   m2
         *   s1, s2, s3, s4
         *   d1, d2, d4
         */

        return kb;
    }

    @BeforeEach
    void setParameters() {
        Rule.MIN_FACT_COVERAGE = -1.0;
    }

    @Test
    void testFamilyRule1() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* parent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_PARENT, cache, kb);
        assertTrue(rule.toString().contains("parent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* parent(X, ?) :- father(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 0, 0));
        assertTrue(rule.toString().contains("parent(X0,?):-father(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-father(X0,X2)"));
        assertEquals(
                new Eval(null, 4, 4 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(parent1, father1)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(parent2, father1)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(parent3, father2)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(parent4, father2)));
        final Set<List<Predicate>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(parent1, father1)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(parent2, father1)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(parent3, father2)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(parent4, father3)));
        final Set<List<Predicate>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(parent1, father1)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(parent2, father1)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(parent3, father3)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(parent4, father2)));
        final Set<List<Predicate>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(parent1, father1)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(parent2, father1)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(parent3, father3)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(parent4, father3)));
        assertTrue(
                expected_grounding_set1.equals(actual_grounding_set) ||
                        expected_grounding_set2.equals(actual_grounding_set) ||
                        expected_grounding_set3.equals(actual_grounding_set) ||
                        expected_grounding_set4.equals(actual_grounding_set)
        );
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg1: new String[]{"f1", "f2", "f3", "f4"}) {
            for (String arg2: kb.allConstants()) {
                final Predicate counter_example = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(parent1);
        expected_counter_examples.remove(parent2);
        expected_counter_examples.remove(parent3);
        expected_counter_examples.remove(parent4);
        assertEquals(expected_counter_examples, update_result.counterExamples);
        assertEquals(2, cache.size());

        /* parent(X, Y) :- father(X, Y) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 1, 1, 1));
        assertTrue(rule.toString().contains("parent(X0,X1):-father(X0,X1)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-father(X0,X1)"));
        assertEquals(
                new Eval(null, 0, 2, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        Predicate counter1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        counter1.args[0] = new Constant(CONST_ID, "f3");
        counter1.args[1] = new Constant(CONST_ID, "s3");
        Predicate counter2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        counter2.args[0] = new Constant(CONST_ID, "f4");
        counter2.args[1] = new Constant(CONST_ID, "d4");
        expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
        assertEquals(3, cache.size());
    }

    @Test
    void testFamilyRule2() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* parent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_PARENT, cache, kb);
        assertTrue(rule.toString().contains("parent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* parent(?, X) :- father(?, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 1));
        assertTrue(rule.toString().contains("parent(?,X0):-father(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X1,X0):-father(X2,X0)"));
        assertEquals(
                new Eval(null, 4, 5 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(parent1, father1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(parent3, father2)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(parent4, father3)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(parent5, father3)));
        assertEquals(expected_grounding_set, actual_grounding_set);
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg2: new String[]{"s1", "s2", "d2", "s3", "d4"}) {
            for (String arg1: kb.allConstants()) {
                final Predicate counter_example = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(parent1);
        expected_counter_examples.remove(parent3);
        expected_counter_examples.remove(parent4);
        expected_counter_examples.remove(parent5);
        assertEquals(expected_counter_examples, update_result.counterExamples);
        assertEquals(2, cache.size());

        /* parent(Y, X) :- father(Y, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 0, 1, 0));
        assertTrue(rule.toString().contains("parent(X1,X0):-father(X1,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X1,X0):-father(X1,X0)"));
        assertEquals(
                new Eval(null, 0, 2, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        Predicate counter1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        counter1.args[0] = new Constant(CONST_ID, "f3");
        counter1.args[1] = new Constant(CONST_ID, "s3");
        Predicate counter2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        counter2.args[0] = new Constant(CONST_ID, "f4");
        counter2.args[1] = new Constant(CONST_ID, "d4");
        expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
        assertEquals(3, cache.size());
    }

    @Test
    void testFamilyRule3() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* grandParent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_GRANDPARENT, cache, kb);
        assertTrue(rule.toString().contains("grandParent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertTrue(rule.toString().contains("grandParent(X0,?):-parent(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2)"));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(2, cache.size());

        /* grandParent(X, Y) :- parent(X, ?), parent(?, Y) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 1, 0, 1));
        assertTrue(rule.toString().contains("grandParent(X0,X1):-parent(X0,?),parent(?,X1)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2),parent(X3,X1)"));
        assertEquals(
                new Eval(null, 2, 6 * 8, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(3, cache.size());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(grand2, parent7, parent4)));
        final Set<List<Predicate>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(grand2, parent7, parent5)));
        final Set<List<Predicate>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(grand2, parent8, parent4)));
        final Set<List<Predicate>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(grand2, parent8, parent5)));
        assertTrue(
                expected_grounding_set1.equals(actual_grounding_set) ||
                        expected_grounding_set2.equals(actual_grounding_set) ||
                        expected_grounding_set3.equals(actual_grounding_set) ||
                        expected_grounding_set4.equals(actual_grounding_set)
        );
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg1: new String[]{"f1", "f2", "m2", "g1", "g2", "g3"}) {
            for (String arg2: new String[]{"s1", "d1", "s2", "d2", "f1", "f2", "m2", "f3"}) {
                final Predicate counter_example = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(grand1);
        expected_counter_examples.remove(grand2);
        assertEquals(expected_counter_examples, update_result.counterExamples);

        /* grandParent(X, Y) :- parent(X, Z), parent(Z, Y) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(1, 1, 2, 0));
        assertTrue(rule.toString().contains("grandParent(X0,X1):-parent(X0,X2),parent(X2,X1)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2),parent(X2,X1)"));
        assertEquals(
                new Eval(null, 0, 2, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(4, cache.size());
        update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        Predicate counter1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter1.args[0] = new Constant(CONST_ID, "g1");
        counter1.args[1] = new Constant(CONST_ID, "d1");
        Predicate counter2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter2.args[0] = new Constant(CONST_ID, "g2");
        counter2.args[1] = new Constant(CONST_ID, "s2");
        expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule4() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* grandParent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_GRANDPARENT, cache, kb);
        assertTrue(rule.toString().contains("grandParent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertTrue(rule.toString().contains("grandParent(X0,?):-parent(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2)"));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(2, cache.size());

        /* grandParent(X, ?) :- parent(X, Y), parent(Y, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 1, 1));
        assertTrue(rule.toString().contains("grandParent(X0,?):-parent(X0,X1),parent(X1,?)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),parent(X1,X3)"));
        assertEquals(
                new Eval(null, 2, 2 * 16, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(3, cache.size());

        /* grandParent(X, Z) :- parent(X, Y), parent(Y, Z) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(2, 1, 0, 1));
        assertTrue(rule.toString().contains("grandParent(X0,X2):-parent(X0,X1),parent(X1,X2)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),parent(X1,X2)"));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(4, cache.size());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(grand2, parent7, parent4)));
        final Set<List<Predicate>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(grand2, parent7, parent5)));
        final Set<List<Predicate>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(grand2, parent8, parent4)));
        final Set<List<Predicate>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(grand1, parent6, parent1)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(grand2, parent8, parent5)));
        assertTrue(
                expected_grounding_set1.equals(actual_grounding_set) ||
                        expected_grounding_set2.equals(actual_grounding_set) ||
                        expected_grounding_set3.equals(actual_grounding_set) ||
                        expected_grounding_set4.equals(actual_grounding_set)
        );
        Predicate counter1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter1.args[0] = new Constant(CONST_ID, "g1");
        counter1.args[1] = new Constant(CONST_ID, "d1");
        Predicate counter2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter2.args[0] = new Constant(CONST_ID, "g2");
        counter2.args[1] = new Constant(CONST_ID, "s2");
        Set<Predicate> expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule5() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* grandParent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_GRANDPARENT, cache, kb);
        assertTrue(rule.toString().contains("grandParent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertTrue(rule.toString().contains("grandParent(X0,?):-parent(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2)"));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(2, cache.size());

        /* grandParent(X, ?) :- parent(X, Y), father(Y, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 1, 1));
        assertTrue(rule.toString().contains("grandParent(X0,?):-parent(X0,X1),father(X1,?)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X3)"));
        assertEquals(
                new Eval(null, 2, 3 * 16, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(3, cache.size());

        /* grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(2, 1, 0, 1));
        assertTrue(rule.toString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)"));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(4, cache.size());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(grand1, parent6, father1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(grand2, parent7, father3)));
        assertEquals(expected_grounding_set, actual_grounding_set);
        Predicate counter1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter1.args[0] = new Constant(CONST_ID, "g2");
        counter1.args[1] = new Constant(CONST_ID, "s2");
        Predicate counter2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter2.args[0] = new Constant(CONST_ID, "g3");
        counter2.args[1] = new Constant(CONST_ID, "s3");
        Set<Predicate> expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule6() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* grandParent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_GRANDPARENT, cache, kb);
        assertTrue(rule.toString().contains("grandParent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* grandParent(?, X) :- father(?, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 1));
        assertTrue(rule.toString().contains("grandParent(?,X0):-father(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X1,X0):-father(X2,X0)"));
        assertEquals(
                new Eval(null, 2, 5 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(2, cache.size());

        /* grandParent(g1, X) :- father(?, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2Constant(0, 0, "g1"));
        assertTrue(rule.toString().contains("grandParent(g1,X0):-father(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(g1,X0):-father(X1,X0)"));
        assertEquals(
                new Eval(null, 1, 5, 2),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(3, cache.size());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(grand1, father1)));
        assertEquals(expected_grounding_set, actual_grounding_set);
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg2: new String[]{"s2", "d2", "s3", "d4"}) {
            final Predicate counter_example = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
            counter_example.args[0] = new Constant(CONST_ID, "g1");
            counter_example.args[1] = new Constant(CONST_ID, arg2);
            expected_counter_examples.add(counter_example);
        }
        assertEquals(expected_counter_examples, update_result.counterExamples);

        /* grandParent(g1, X) :- father(f2, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2Constant(1, 0, "f2"));
        assertTrue(rule.toString().contains("grandParent(g1,X0):-father(f2,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(g1,X0):-father(f2,X0)"));
        assertEquals(
                new Eval(null, 0, 2, 3),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(4, cache.size());
        update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        Predicate counter1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter1.args[0] = new Constant(CONST_ID, "g1");
        counter1.args[1] = new Constant(CONST_ID, "s2");
        Predicate counter2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter2.args[0] = new Constant(CONST_ID, "g1");
        counter2.args[1] = new Constant(CONST_ID, "d2");
        expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule7() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* parent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_PARENT, cache, kb);
        assertTrue(rule.toString().contains("parent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* parent(X, X) :- */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 0, 0, 1));
        assertTrue(rule.toString().contains("parent(X0,X0):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X0):-"));
        assertEquals(
                new Eval(null, 0, 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        UpdateResult update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg: kb.allConstants()) {
            final Predicate counter_example = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
            counter_example.args[0] = new Constant(CONST_ID, arg);
            counter_example.args[1] = new Constant(CONST_ID, arg);
            expected_counter_examples.add(counter_example);
        }
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule8() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* father(?, ?):- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_FATHER, cache, kb);
        assertTrue(rule.toString().contains("father(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-"));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* father(X, ?):- parent(?, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 1, 0, 0));
        assertTrue(rule.toString().contains("father(X0,?):-parent(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-parent(X2,X0)"));
        assertEquals(
                new Eval(null, 4, 8 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        assertEquals(2, cache.size());

        /* father(X, ?):- parent(?, X), parent(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0));
        assertTrue(rule.toString().contains("father(X0,?):-parent(?,X0),parent(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-parent(X2,X0),parent(X0,X3)"));
        assertEquals(
                new Eval(null, 3, 3 * 16, 2),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        assertEquals(3, cache.size());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        final Set<List<Predicate>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(father1, parent6, parent1)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(father2, parent7, parent3)));
        expected_grounding_set1.add(new ArrayList<>(Arrays.asList(father3, parent7, parent3)));
        final Set<List<Predicate>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(father1, parent6, parent1)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(father2, parent7, parent3)));
        expected_grounding_set2.add(new ArrayList<>(Arrays.asList(father3, parent7, parent4)));
        final Set<List<Predicate>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(father1, parent6, parent1)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(father2, parent7, parent4)));
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(father3, parent7, parent3)));
        final Set<List<Predicate>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(father1, parent6, parent1)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(father2, parent7, parent4)));
        expected_grounding_set4.add(new ArrayList<>(Arrays.asList(father3, parent7, parent4)));
        final Set<List<Predicate>> expected_grounding_set5 = new HashSet<>();
        expected_grounding_set5.add(new ArrayList<>(Arrays.asList(father1, parent6, parent2)));
        expected_grounding_set5.add(new ArrayList<>(Arrays.asList(father2, parent7, parent3)));
        expected_grounding_set5.add(new ArrayList<>(Arrays.asList(father3, parent7, parent3)));
        final Set<List<Predicate>> expected_grounding_set6 = new HashSet<>();
        expected_grounding_set6.add(new ArrayList<>(Arrays.asList(father1, parent6, parent2)));
        expected_grounding_set6.add(new ArrayList<>(Arrays.asList(father2, parent7, parent3)));
        expected_grounding_set6.add(new ArrayList<>(Arrays.asList(father3, parent7, parent4)));
        final Set<List<Predicate>> expected_grounding_set7 = new HashSet<>();
        expected_grounding_set7.add(new ArrayList<>(Arrays.asList(father1, parent6, parent2)));
        expected_grounding_set7.add(new ArrayList<>(Arrays.asList(father2, parent7, parent4)));
        expected_grounding_set7.add(new ArrayList<>(Arrays.asList(father3, parent7, parent3)));
        final Set<List<Predicate>> expected_grounding_set8 = new HashSet<>();
        expected_grounding_set8.add(new ArrayList<>(Arrays.asList(father1, parent6, parent2)));
        expected_grounding_set8.add(new ArrayList<>(Arrays.asList(father2, parent7, parent4)));
        expected_grounding_set8.add(new ArrayList<>(Arrays.asList(father3, parent7, parent4)));
        assertTrue(expected_grounding_set1.equals(actual_grounding_set) ||
                expected_grounding_set2.equals(actual_grounding_set) ||
                expected_grounding_set3.equals(actual_grounding_set) ||
                expected_grounding_set4.equals(actual_grounding_set) ||
                expected_grounding_set5.equals(actual_grounding_set) ||
                expected_grounding_set6.equals(actual_grounding_set) ||
                expected_grounding_set7.equals(actual_grounding_set) ||
                expected_grounding_set8.equals(actual_grounding_set)
        );
        Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg1: new String[]{"f1", "f2", "m2"}) {
            for (String arg2: kb.allConstants()) {
                final Predicate counter_example = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(father1);
        expected_counter_examples.remove(father2);
        expected_counter_examples.remove(father3);
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule9() {
        final MemKB kb = kbFamily();
        /* #1: father(?, ?):- */
        final RecalculateCachedRule rule1 = new RecalculateCachedRule(FUNCTOR_FATHER, new HashSet<>(), kb);
        assertTrue(rule1.toString().contains("father(?,?):-"));
        assertTrue(rule1.toCompleteRuleString().contains("father(X0,X1):-"));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedBoundedVars());
        assertEquals(1, rule1.length());

        /* #1: father(f2,?):- */
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVar2Constant(0, 0, "f2"));
        assertTrue(rule1.toString().contains("father(f2,?):-"));
        assertTrue(rule1.toCompleteRuleString().contains("father(f2,X0):-"));
        assertEquals(
                new Eval(null, 2, 16, 1),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedBoundedVars());
        assertEquals(1, rule1.length());
        rule1.updateInKb();

        /* #2: father(?, ?):- */
        final RecalculateCachedRule rule2 = new RecalculateCachedRule(FUNCTOR_FATHER, new HashSet<>(), kb);
        assertTrue(rule2.toString().contains("father(?,?):-"));
        assertTrue(rule2.toCompleteRuleString().contains("father(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16 - 2, 0),
                rule2.getEval()
        );
        assertEquals(0, rule2.usedBoundedVars());
        assertEquals(1, rule2.length());
        UpdateResult update_result = rule2.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate father4 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father4.args[0] = new Constant(CONST_ID, "f3");
        father4.args[1] = new Constant(CONST_ID, "s3");
        Predicate father5 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father5.args[0] = new Constant(CONST_ID, "f4");
        father5.args[1] = new Constant(CONST_ID, "d4");
        final Set<List<Predicate>> expected_groundings = new HashSet<>();
        expected_groundings.add(new ArrayList<>(Collections.singleton(father1)));
        expected_groundings.add(new ArrayList<>(Collections.singleton(father4)));
        expected_groundings.add(new ArrayList<>(Collections.singleton(father5)));
        assertEquals(expected_groundings, actual_grounding_set);
        final Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg1: kb.allConstants()) {
            for (String arg2: kb.allConstants()) {
                final Predicate counter_example = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(father1);
        expected_counter_examples.remove(father2);
        expected_counter_examples.remove(father3);
        expected_counter_examples.remove(father4);
        expected_counter_examples.remove(father5);
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyRule10() {
        final MemKB kb = kbFamily();
        /* #1: father(?, ?):- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_FATHER, new HashSet<>(), kb);
        assertTrue(rule.toString().contains("father(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-"));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());

        /* father(X, ?):- father(X, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertTrue(rule.toString().contains("father(X0,?):-parent(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-parent(X0,X2)"));
        assertEquals(
                new Eval(null, 3, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());

        /* father(X, ?):- father(X, X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(1, 1, 0));
        assertTrue(rule.toString().contains("father(X0,?):-parent(X0,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-parent(X0,X0)"));
        assertEquals(
                new Eval(null, 0, 0, 2),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        UpdateResult update_result = rule.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        assertTrue(update_result.counterExamples.isEmpty());
    }

    @Test
    void testCounterExample1() {
        final MemKB kb = kbFamily();
        /* father(?, ?):- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_FATHER, new HashSet<>(), kb);
        assertTrue(rule.toString().contains("father(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-"));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule.getEval()
        );
        UpdateResult update_result = rule.updateInKb();
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        final Set<Predicate> expected_counter_examples = new HashSet<>();
        for (String arg1: kb.allConstants()) {
            for (String arg2: kb.allConstants()) {
                final Predicate counter_example = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.removeAll(kb.getAllFacts(FUNCTOR_FATHER));
        assertEquals(expected_counter_examples, update_result.counterExamples);
    }

    @Test
    void testFamilyWithCopy1() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* grandParent(?, ?) :- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_GRANDPARENT, cache, kb);
        assertTrue(rule.toString().contains("grandParent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("grandParent(X0,X1):-"));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        assertEquals(1, cache.size());

        /* #1: grandParent(X, ?) :- parent(X, ?) */
        final RecalculateCachedRule rule1 = new RecalculateCachedRule(rule);
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertTrue(rule1.toString().contains("grandParent(X0,?):-parent(X0,?)"));
        assertTrue(rule1.toCompleteRuleString().contains("grandParent(X0,X1):-parent(X0,X2)"));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule1.getEval()
        );
        assertEquals(1, rule1.usedBoundedVars());
        assertEquals(2, rule1.length());
        assertEquals(2, cache.size());

        /* #1: grandParent(X, ?) :- parent(X, Y), father(Y, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 1, 1));
        assertTrue(rule1.toString().contains("grandParent(X0,?):-parent(X0,X1),father(X1,?)"));
        assertTrue(rule1.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X3)"));
        assertEquals(
                new Eval(null, 2, 3 * 16, 2),
                rule1.getEval()
        );
        assertEquals(2, rule1.usedBoundedVars());
        assertEquals(3, rule1.length());
        assertEquals(3, cache.size());

        /* #1: grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVars2NewVar(2, 1, 0, 1));
        assertTrue(rule1.toString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)"));
        assertTrue(rule1.toCompleteRuleString().contains("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)"));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule1.getEval()
        );
        assertEquals(3, rule1.usedBoundedVars());
        assertEquals(3, rule1.length());
        assertEquals(4, cache.size());
        UpdateResult update_result = rule1.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(grand1, parent6, father1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(grand2, parent7, father3)));
        assertEquals(expected_grounding_set, actual_grounding_set);
        Predicate counter1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter1.args[0] = new Constant(CONST_ID, "g2");
        counter1.args[1] = new Constant(CONST_ID, "s2");
        Predicate counter2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        counter2.args[0] = new Constant(CONST_ID, "g3");
        counter2.args[1] = new Constant(CONST_ID, "s3");
        Set<Predicate> expected_counter_examples = new HashSet<>(Arrays.asList(counter1, counter2));
        assertEquals(expected_counter_examples, update_result.counterExamples);

        /* #2: grandParent(X, ?) :- parent(X, ?) */
        final RecalculateCachedRule rule2 = new RecalculateCachedRule(rule);
        assertNotEquals(Rule.UpdateStatus.NORMAL, rule2.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_PARENT, 0, 0, 0));
        assertEquals(4, cache.size());

        /* #3: grandParent(?, X) :- father(?, X) */
        final RecalculateCachedRule rule3 = new RecalculateCachedRule(rule);
        assertEquals(Rule.UpdateStatus.NORMAL, rule3.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 1));
        assertTrue(rule3.toString().contains("grandParent(?,X0):-father(?,X0)"));
        assertTrue(rule3.toCompleteRuleString().contains("grandParent(X1,X0):-father(X2,X0)"));
        assertEquals(
                new Eval(null, 0, 5 * 16 - 2, 1),
                rule3.getEval()
        );
        assertEquals(1, rule3.usedBoundedVars());
        assertEquals(2, rule3.length());
        assertEquals(5, cache.size());

        /* #3: grandParent(Y, X) :- father(?, X), parent(Y, ?) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule3.boundFreeVars2NewVar(FUNCTOR_PARENT, ARITY_FATHER, 0, 0, 0));
        assertTrue(rule3.toString().contains("grandParent(X1,X0):-father(?,X0),parent(X1,?)"));
        assertTrue(rule3.toCompleteRuleString().contains("grandParent(X1,X0):-father(X2,X0),parent(X1,X3)"));
        assertEquals(
                new Eval(null, 0, 5 * 6 - 2, 2),
                rule3.getEval()
        );
        assertEquals(2, rule3.usedBoundedVars());
        assertEquals(3, rule3.length());
        assertEquals(6, cache.size());
        update_result = rule3.updateInKb();
        assertTrue(update_result.groundings.isEmpty());
        expected_counter_examples = new HashSet<>();
        for (String arg1: new String[]{"f1", "f2", "m2", "g1", "g2", "g3"}) {
            for (String arg2: new String[]{"s1", "s2", "d2", "s3", "d4"}) {
                final Predicate counter_example = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
                counter_example.args[0] = new Constant(CONST_ID, arg1);
                counter_example.args[1] = new Constant(CONST_ID, arg2);
                expected_counter_examples.add(counter_example);
            }
        }
        expected_counter_examples.remove(grand1);
        expected_counter_examples.remove(grand2);
        assertEquals(expected_counter_examples, update_result.counterExamples);

        /* #3: grandParent(Y, X) :- father(Z, X), parent(Y, Z) */
        assertNotEquals(Rule.UpdateStatus.NORMAL, rule3.boundFreeVars2NewVar(1, 0, 2, 1));
        assertEquals(6, cache.size());
    }

    @Test
    void testFamilyWithCopy2() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* #1: parent(?, ?) :- */
        final RecalculateCachedRule rule1 = new RecalculateCachedRule(FUNCTOR_PARENT, cache, kb);
        assertTrue(rule1.toString().contains("parent(?,?):-"));
        assertTrue(rule1.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedBoundedVars());
        assertEquals(1, rule1.length());
        assertEquals(1, cache.size());

        /* #1: parent(f2, ?) :- */
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVar2Constant(0, 0, "f2"));
        assertTrue(rule1.toString().contains("parent(f2,?):-"));
        assertTrue(rule1.toCompleteRuleString().contains("parent(f2,X0):-"));
        assertEquals(
                new Eval(null, 2, 16, 1),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedBoundedVars());
        assertEquals(1, rule1.length());
        assertEquals(2, cache.size());

        /* #2: parent(f2, d2) :- */
        final RecalculateCachedRule rule2 = new RecalculateCachedRule(rule1);
        assertEquals(Rule.UpdateStatus.NORMAL, rule2.boundFreeVar2Constant(0, 1, "d2"));
        assertTrue(rule2.toString().contains("parent(f2,d2):-"));
        assertTrue(rule2.toCompleteRuleString().contains("parent(f2,d2):-"));
        assertEquals(
                new Eval(null, 1, 1, 2),
                rule2.getEval()
        );
        assertEquals(0, rule2.usedBoundedVars());
        assertEquals(1, rule2.length());
        assertEquals(3, cache.size());
        UpdateResult update_result2 = rule2.updateInKb();
        final Set<List<Predicate>> actual_grounding_set2 = new HashSet<>();
        for (Predicate[] grounding: update_result2.groundings) {
            actual_grounding_set2.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        final Set<List<Predicate>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ArrayList<>(Collections.singletonList(parent4)));
        assertEquals(expected_grounding_set2, actual_grounding_set2);
        assertTrue(update_result2.counterExamples.isEmpty());

        /* #3: parent(f2, X) :- father(?, X) */
        final RecalculateCachedRule rule3 = new RecalculateCachedRule(rule1);
        assertEquals(Rule.UpdateStatus.NORMAL, rule3.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 1));
        assertTrue(rule3.toString().contains("parent(f2,X0):-father(?,X0)"));
        assertTrue(rule3.toCompleteRuleString().contains("parent(f2,X0):-father(X1,X0)"));
        assertEquals(
                new Eval(null, 1, 4, 2),
                rule3.getEval()
        );
        assertEquals(1, rule3.usedBoundedVars());
        assertEquals(2, rule3.length());
        assertEquals(4, cache.size());
        UpdateResult update_result3 = rule3.updateInKb();
        final Set<List<Predicate>> actual_grounding_set3 = new HashSet<>();
        for (Predicate[] grounding: update_result3.groundings) {
            actual_grounding_set3.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        final Set<List<Predicate>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ArrayList<>(Arrays.asList(parent3, father2)));
        assertEquals(expected_grounding_set3, actual_grounding_set3);
        Set<Predicate> expected_counter_examples3 = new HashSet<>();
        for (String arg: new String[]{"s1", "s3", "d4"}) {
            final Predicate counter_example = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
            counter_example.args[0] = new Constant(CONST_ID, "f2");
            counter_example.args[1] = new Constant(CONST_ID, arg);
            expected_counter_examples3.add(counter_example);
        }
        assertEquals(expected_counter_examples3, update_result3.counterExamples);
    }

    @Test
    void testValidity1() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* father(?,?):- */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_FATHER, cache, kb);
        assertTrue(rule.toString().contains("father(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-"));

        /* #1: father(X,?) :- father(?,X) */
        final RecalculateCachedRule rule1 = new RecalculateCachedRule(rule);
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 0));
        assertTrue(rule1.toString().contains("father(X0,?):-father(?,X0)"));
        assertTrue(rule1.toCompleteRuleString().contains("father(X0,X1):-father(X2,X0)"));

        /* #1: father(X,Y) :- father(Y,X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule1.boundFreeVars2NewVar(0, 1, 1, 0));
        assertTrue(rule1.toString().contains("father(X0,X1):-father(X1,X0)"));
        assertTrue(rule1.toCompleteRuleString().contains("father(X0,X1):-father(X1,X0)"));

        /* #2: father(X,?) :- father(X,?) [invalid] */
        final RecalculateCachedRule rule2 = new RecalculateCachedRule(rule);
        assertNotEquals(Rule.UpdateStatus.NORMAL, rule2.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 0, 0));
    }

    @Test
    void testValidity2() {
        final MemKB kb = kbFamily();
        final Set<RuleFingerPrint> cache = new HashSet<>();

        /* father(X,?) :- father(?,X) */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_FATHER, cache, kb);
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0, 0));
        assertTrue(rule.toString().contains("father(X0,?):-father(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-father(X2,X0)"));

        /* father(X,?) :- father(?,X), father(?,X) */
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(FUNCTOR_FATHER, ARITY_FATHER, 1, 0));
        assertTrue(rule.toString().contains("father(X0,?):-father(?,X0),father(?,X0)"));
        assertTrue(rule.toCompleteRuleString().contains("father(X0,X1):-father(X2,X0),father(X3,X0)"));

        /* father(X,?) :- father(Y,X), father(Y,X) [invalid] */
        assertNotEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(1, 0, 2, 0));
    }

    @Test
    void testFcFiltering1() {
        Rule.MIN_FACT_COVERAGE = 0.44;
        final MemKB kb = kbFamily();
        /* parent(X, ?) :- father(X, ?) */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_PARENT, new HashSet<>(), kb);
        assertTrue(rule.toString().contains("parent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());

        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 0, 0));
        assertTrue(rule.toString().contains("parent(X0,?):-father(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-father(X0,X2)"));
        assertEquals(
                new Eval(null, 4, 4 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        UpdateResult update_result = rule.updateInKb();
    }

    @Test
    void testFcFiltering2() {
        Rule.MIN_FACT_COVERAGE = 0.45;
        final MemKB kb = kbFamily();
        /* parent(X, ?) :- father(X, ?) */
        final RecalculateCachedRule rule = new RecalculateCachedRule(FUNCTOR_PARENT, new HashSet<>(), kb);
        assertTrue(rule.toString().contains("parent(?,?):-"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-"));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedBoundedVars());
        assertEquals(1, rule.length());

        assertNotEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(FUNCTOR_FATHER, ARITY_FATHER, 0, 0, 0));
        assertTrue(rule.toString().contains("parent(X0,?):-father(X0,?)"));
        assertTrue(rule.toCompleteRuleString().contains("parent(X0,X1):-father(X0,X2)"));
    }

    @Test
    void testAnyRule1() {
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

        final MemKB kb = new MemKB();
        kb.addFact(p1);
        kb.addFact(p2);
        kb.addFact(p3);
        kb.addFact(h1);
        kb.addFact(h2);

        RecalculateCachedRule rule = new RecalculateCachedRule("h", new HashSet<>(), kb);
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar("p", 3, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 2, 1, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(0, 1, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(0, 3, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2Constant(1, 2, "+"));
        assertTrue(rule.toString().contains("h(X0,X0,X1,X1):-p(X0,X1,+)"));
        assertEquals(
                new Eval(null, 2, 2, 5),
                rule.getEval()
        );
        assertEquals(2, rule.usedBoundedVars());
        assertEquals(2, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h1, p1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h2, p2)));
        assertTrue(update_result.counterExamples.isEmpty());
    }

    @Test
    void testAnyRule2() {
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

        final Predicate h1 = new Predicate("h", 1);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");

        final MemKB kb = new MemKB();
        kb.addFact(p1);
        kb.addFact(p2);
        kb.addFact(p3);
        kb.addFact(p4);
        kb.addFact(q1);
        kb.addFact(q2);
        kb.addFact(h1);

        final RecalculateCachedRule rule = new RecalculateCachedRule("h", new HashSet<>(), kb);
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar("p", 2, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("q", 1, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar(1, 1, 0));
        assertTrue(rule.toString().contains("h(X0):-p(X0,X0),q(X0)"));
        assertEquals(
                new Eval(null, 1, 1, 3),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(3, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h1, p2, q2)));
        assertTrue(update_result.counterExamples.isEmpty());
    }

    @Test
    void testAnyRule3() {
        /* h(X, X) :- */
        final Predicate h1 = new Predicate("h", 2);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate h2 = new Predicate("h", 2);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate h3 = new Predicate("h", 2);
        h3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "c");

        final MemKB kb = new MemKB();
        kb.addFact(h1);
        kb.addFact(h2);
        kb.addFact(h3);

        RecalculateCachedRule rule = new RecalculateCachedRule("h", new HashSet<>(), kb);
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 0, 0, 1));
        assertTrue(rule.toString().contains("h(X0,X0):-"));
        assertEquals(
                new Eval(null, 2, 3, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h2)));

        final Predicate c1 = new Predicate("h", 2);
        c1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        c1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        assertEquals(new HashSet<>(List.of(c1)), update_result.counterExamples);

        assertTrue(kb.hasProved(h1));
        assertTrue(kb.hasProved(h2));
        assertFalse(kb.hasProved(h3));
    }

    @Test
    void testAnyRule4() {
        /* h(X, X, ?) :- */
        final Predicate h1 = new Predicate("h", 3);
        h1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        final Predicate h2 = new Predicate("h", 3);
        h2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate h3 = new Predicate("h", 3);
        h3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        h3.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        h3.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "b");

        final MemKB kb = new MemKB();
        kb.addFact(h1);
        kb.addFact(h2);
        kb.addFact(h3);

        RecalculateCachedRule rule = new RecalculateCachedRule("h", new HashSet<>(), kb);
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar(0, 0, 0, 1));
        assertTrue(rule.toString().contains("h(X0,X0,?):-"));
        assertEquals(
                new Eval(null, 2, 4, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedBoundedVars());
        assertEquals(1, rule.length());
        UpdateResult update_result = rule.updateInKb();
        final Set<List<Predicate>> actual_grounding_set = new HashSet<>();
        for (Predicate[] grounding: update_result.groundings) {
            actual_grounding_set.add(new ArrayList<>(Arrays.asList(grounding)));
        }
        final Set<List<Predicate>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h1)));
        expected_grounding_set.add(new ArrayList<>(Arrays.asList(h2)));

        final Predicate c1 = new Predicate("h", 3);
        c1.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        c1.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        c1.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "a");
        final Predicate c2 = new Predicate("h", 3);
        c2.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        c2.args[1] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        c2.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "b");
        assertEquals(new HashSet<>(List.of(c1, c2)), update_result.counterExamples);

        assertTrue(kb.hasProved(h1));
        assertTrue(kb.hasProved(h2));
        assertFalse(kb.hasProved(h3));
    }
}