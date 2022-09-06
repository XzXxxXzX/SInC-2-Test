package sinc.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PredicateTest {
    @Test
    public void testEquality() {
        Predicate p1 = new Predicate("functor", 3);
        p1.args[0] = new Variable(15);
        p1.args[2] = new Constant(-1, "const");

        Predicate p2 = new Predicate("functor", 3);
        p2.args[0] = new Variable(15);
        p2.args[2] = new Constant(-1, "const");

        assertEquals(p2, p1);
        assertEquals("functor(X15,?,const)", p1.toString());

        Set<Predicate> set = new HashSet<>();
        set.add(p2);
        assertFalse(set.add(p1));
    }
}