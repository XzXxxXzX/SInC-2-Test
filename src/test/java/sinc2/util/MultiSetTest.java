package sinc2.util;

import org.junit.jupiter.api.Test;
import sinc.common.ArgIndicator;
import sinc.common.VarIndicator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultiSetTest {

    @Test
    void add() {
        MultiSet<String> s1 = new MultiSet<>();
        s1.add("a");
        s1.add("b");
        s1.add("a");

        MultiSet<String> s2 = new MultiSet<>();
        s2.add("b");
        s2.add("a");

        assertEquals(3, s1.size());
        assertEquals(2, s2.size());
        assertNotEquals(s2, s1);
        assertNotEquals(s1, s2);
    }

    @Test
    void addAll() {
        MultiSet<String> s1 = new MultiSet<>();
        s1.add("a");
        s1.add("b");
        s1.add("a");

        MultiSet<String> s2 = new MultiSet<>();
        s2.add("b");
        s2.add("a");

        MultiSet<String> s3 = new MultiSet<>(s2);

        s2.addAll(s1);
        s1.addAll(s3);

        assertEquals(5, s1.size());
        assertEquals(5, s2.size());
        assertEquals(2, s3.size());
        assertEquals(s1, s2);
        assertEquals(s2, s1);
    }

    @Test
    void remove() {
        MultiSet<String> s1 = new MultiSet<>();
        s1.add("a");
        s1.add("b");
        s1.add("a");
        s1.remove("a");
        s1.remove("c");

        MultiSet<String> s2 = new MultiSet<>();
        s2.add("b");
        s2.add("a");
        s1.remove("d");
        s1.remove("c");

        assertEquals(2, s1.size());
        assertEquals(2, s2.size());
        assertEquals(s1, s2);
        assertEquals(s2, s1);
    }

    @Test
    void intersection() {
        MultiSet<String> s1 = new MultiSet<>();
        s1.add("a");
        s1.add("b");
        s1.add("a");
        s1.add("d");

        MultiSet<String> s2 = new MultiSet<>();
        s2.add("b");
        s2.add("a");
        s2.add("b");
        s2.add("c");

        MultiSet<String> s3 = new MultiSet<>();
        s3.add("a");
        s3.add("b");

        MultiSet<String> si1 = s1.intersection(s2);
        MultiSet<String> si2 = s2.intersection(s1);

        assertEquals(2, si1.size());
        assertEquals(2, si2.size());
        assertEquals(s3, si1);
        assertEquals(s3, si2);
        assertEquals(si1, s3);
        assertEquals(si2, s3);
    }

    @Test
    void union() {
        MultiSet<String> s1 = new MultiSet<>();
        s1.add("a");
        s1.add("b");
        s1.add("a");

        MultiSet<String> s2 = new MultiSet<>();
        s2.add("b");
        s2.add("a");
        s2.add("c");

        MultiSet<String> s3 = new MultiSet<>();
        s3.add("a");
        s3.add("a");
        s3.add("b");
        s3.add("c");

        MultiSet<String> su1 = s1.union(s2);
        MultiSet<String> su2 = s2.union(s1);

        assertEquals(4, su1.size());
        assertEquals(4, su2.size());
        assertEquals(s3, su1);
        assertEquals(s3, su2);
        assertEquals(su1, s3);
        assertEquals(su2, s3);
    }

    @Test
    void elementsAboveProportion() {
        MultiSet<String> s = new MultiSet<>();
        s.add("a");
        s.add("b");
        s.add("a");
        s.add("c");
        s.add("a");
        s.add("b");
        s.add("d");
        s.add("b");

        List<String> l = s.elementsAboveProportion(0.25);
        Set<String> e_set = new HashSet<>();
        for (String str: l) {
            assertTrue(e_set.add(str));
        }

        Set<String> e_set2 = new HashSet<>();
        e_set2.add("a");
        e_set2.add("b");

        assertEquals(e_set, e_set2);
    }

    @Test
    void testEquality() {
        final String PARENT = "parent";
        final String FATHER = "father";
        final MultiSet<ArgIndicator> set1 = new MultiSet<>();
        set1.add(new VarIndicator(PARENT, 1));
        set1.add(new VarIndicator(FATHER, 0));

        final MultiSet<ArgIndicator> set2 = new MultiSet<>();
        set2.add(new VarIndicator(PARENT, 1));
        set2.add(new VarIndicator(FATHER, 0));

        assertEquals(set1, set2);

        final MultiSet<MultiSet<ArgIndicator>> wrapper_set1 = new MultiSet<>();
        wrapper_set1.add(set1);

        final MultiSet<MultiSet<ArgIndicator>> wrapper_set2 = new MultiSet<>();
        wrapper_set2.add(set2);

        assertEquals(wrapper_set1, wrapper_set2);
    }
}