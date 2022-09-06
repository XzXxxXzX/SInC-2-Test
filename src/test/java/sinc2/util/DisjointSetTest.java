package sinc2.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisjointSetTest {


    @Test
    void test() {
        /* Test 1: {0, 1, 2}, {3, 4} */
        DisjointSet s1 = new DisjointSet(5);
        assertEquals(5, s1.totalSets());

        s1.unionSets(0, 2);
        s1.unionSets(1, 2);
        s1.unionSets(4, 4);
        s1.unionSets(4, 3);
        assertEquals(2, s1.totalSets());
        assertEquals(s1.findSet(0), s1.findSet(1));
        assertEquals(s1.findSet(0), s1.findSet(2));
        assertEquals(s1.findSet(1), s1.findSet(2));
        assertEquals(s1.findSet(3), s1.findSet(4));
        assertNotEquals(s1.findSet(0), s1.findSet(3));

        /* Test 2: {0, 1, 2, 3, 4} */
        DisjointSet s2 = new DisjointSet(5);
        assertEquals(5, s2.totalSets());

        s2.unionSets(0, 2);
        s2.unionSets(1, 2);
        s2.unionSets(1, 4);
        s2.unionSets(4, 3);
        assertEquals(1, s2.totalSets());
        assertEquals(s2.findSet(0), s2.findSet(1));
        assertEquals(s2.findSet(0), s2.findSet(2));
        assertEquals(s2.findSet(1), s2.findSet(3));
        assertEquals(s2.findSet(3), s2.findSet(4));

        /* Test 3: {0, 1}, {2}, {3, 4} */
        DisjointSet s3 = new DisjointSet(5);
        assertEquals(5, s3.totalSets());

        s3.unionSets(0, 1);
        s3.unionSets(1, 1);
        s3.unionSets(4, 4);
        s3.unionSets(4, 3);
        assertEquals(3, s3.totalSets());
        assertEquals(s3.findSet(0), s3.findSet(1));
        assertEquals(s3.findSet(3), s3.findSet(4));
        assertNotEquals(s3.findSet(0), s3.findSet(2));
        assertNotEquals(s3.findSet(3), s3.findSet(2));
        assertNotEquals(s3.findSet(1), s3.findSet(3));
    }
}