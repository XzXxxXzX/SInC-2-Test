package sinc2.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.kb.NumerationMap;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PredicateTest {

    static NumerationMap map = new NumerationMap();

    @BeforeAll
    static void createNumerationMap() {
        assertEquals(1, map.mapName("family"));
        assertEquals(2, map.mapName("alice"));
        assertEquals(3, map.mapName("bob"));
        assertEquals(4, map.mapName("catherine"));
    }

    @Test
    void testEquality() {
        Predicate p1 = new Predicate(1, 3);
        p1.args[0] = Argument.variable(1);
        p1.args[2] = Argument.constant(2);

        Predicate p2 = new Predicate(1, 3);
        p2.args[0] = Argument.variable(1);
        p2.args[2] = Argument.constant(2);

        assertEquals(p2, p1);

        Set<Predicate> set = new HashSet<>();
        set.add(p2);
        assertFalse(set.add(p1));
    }

    @Test
    void testStringify() {
        Predicate p = new Predicate(1, 7);
        p.args[0] = Argument.constant(2);
        p.args[1] = Argument.constant(3);
        p.args[2] = Argument.constant(4);
        p.args[3] = Argument.variable(0);
        p.args[4] = Argument.variable(3);
        p.args[5] = Argument.EMPTY_VALUE;

        assertEquals("family(alice,bob,catherine,X0,X3,?,?)", p.toString(map));
    }
}