package sinc.impl.cached;

import org.junit.jupiter.api.Test;
import sinc.common.Constant;
import sinc.common.Predicate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MemKBTest {

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

    @Test
    void testKbFamily() {
        final MemKB kb = kbFamily();
        assertEquals(16, kb.totalConstants());
        assertEquals(17, kb.totalFacts());

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
        final Set<Predicate> father_facts = new HashSet<>();
        father_facts.add(father1);
        father_facts.add(father2);
        father_facts.add(father3);
        father_facts.add(father4);
        father_facts.add(father5);
        assertEquals(father_facts, kb.getAllFacts(FUNCTOR_FATHER));
        assertEquals(2, kb.getArity(FUNCTOR_FATHER));
        final Set<String> father_a1_values = new HashSet<>(Arrays.asList("f1", "f2", "f3", "f4"));
        assertEquals(father_a1_values, kb.getValueSet(FUNCTOR_FATHER, 0));
        final Set<String> father_a2_values = new HashSet<>(Arrays.asList("s1", "s2", "d2", "s3", "d4"));
        assertEquals(father_a2_values, kb.getValueSet(FUNCTOR_FATHER, 1));
        final Map<String, Set<Predicate>> father_a1_indices = new HashMap<>();
        father_a1_indices.put("f1", new HashSet<>(Arrays.asList(father1)));
        father_a1_indices.put("f2", new HashSet<>(Arrays.asList(father2, father3)));
        father_a1_indices.put("f3", new HashSet<>(Arrays.asList(father4)));
        father_a1_indices.put("f4", new HashSet<>(Arrays.asList(father5)));
        assertEquals(father_a1_indices, kb.getArgIndices(FUNCTOR_FATHER, 0));

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
        final Set<Predicate> parent_facts = new HashSet<>();
        parent_facts.add(parent1);
        parent_facts.add(parent2);
        parent_facts.add(parent3);
        parent_facts.add(parent4);
        parent_facts.add(parent5);
        parent_facts.add(parent6);
        parent_facts.add(parent7);
        parent_facts.add(parent8);
        parent_facts.add(parent9);
        assertEquals(parent_facts, kb.getAllFacts(FUNCTOR_PARENT));
        assertEquals(2, kb.getArity(FUNCTOR_PARENT));
        final Set<String> parent_a1_values = new HashSet<>(Arrays.asList("f1", "f2", "m2", "g1", "g2", "g3"));
        assertEquals(parent_a1_values, kb.getValueSet(FUNCTOR_PARENT, 0));
        final Set<String> parent_a2_values = new HashSet<>(Arrays.asList("s1", "d1", "s2", "d2", "f1", "f2", "m2", "f3"));
        assertEquals(parent_a2_values, kb.getValueSet(FUNCTOR_PARENT, 1));
        final Map<String, Set<Predicate>> parent_a2_indices = new HashMap<>();
        parent_a2_indices.put("s1", new HashSet<>(Arrays.asList(parent1)));
        parent_a2_indices.put("d1", new HashSet<>(Arrays.asList(parent2)));
        parent_a2_indices.put("s2", new HashSet<>(Arrays.asList(parent3)));
        parent_a2_indices.put("d2", new HashSet<>(Arrays.asList(parent4, parent5)));
        parent_a2_indices.put("f1", new HashSet<>(Arrays.asList(parent6)));
        parent_a2_indices.put("f2", new HashSet<>(Arrays.asList(parent7)));
        parent_a2_indices.put("m2", new HashSet<>(Arrays.asList(parent8)));
        parent_a2_indices.put("f3", new HashSet<>(Arrays.asList(parent9)));
        assertEquals(parent_a2_indices, kb.getArgIndices(FUNCTOR_PARENT, 1));

        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand3 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand3.args[0] = new Constant(CONST_ID, "g4");
        grand3.args[1] = new Constant(CONST_ID, "s4");
        final Set<Predicate> grand_facts = new HashSet<>();
        grand_facts.add(grand1);
        grand_facts.add(grand2);
        grand_facts.add(grand3);
        assertEquals(grand_facts, kb.getAllFacts(FUNCTOR_GRANDPARENT));
        assertEquals(2, kb.getArity(FUNCTOR_GRANDPARENT));
        final Set<String> grand_a1_values = new HashSet<>(Arrays.asList("g1", "g2", "g4"));
        assertEquals(grand_a1_values, kb.getValueSet(FUNCTOR_GRANDPARENT, 0));
        final Set<String> grand_a2_values = new HashSet<>(Arrays.asList("s1", "d2", "s4"));
        assertEquals(grand_a2_values, kb.getValueSet(FUNCTOR_GRANDPARENT, 1));

        for (Predicate p: father_facts) {
            assertFalse(kb.hasProved(p));
        }
        for (Predicate p: parent_facts) {
            assertFalse(kb.hasProved(p));
        }
        for (Predicate p: grand_facts) {
            assertFalse(kb.hasProved(p));
        }
        for (Predicate p: parent_facts) {
            kb.proveFact(p);
        }
        for (Predicate p: father_facts) {
            assertFalse(kb.hasProved(p));
        }
        for (Predicate p: parent_facts) {
            assertTrue(kb.hasProved(p));
        }
        for (Predicate p: grand_facts) {
            assertFalse(kb.hasProved(p));
        }
    }
}