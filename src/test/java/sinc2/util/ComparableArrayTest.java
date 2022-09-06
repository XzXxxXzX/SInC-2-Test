package sinc2.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparableArrayTest {

    @Test
    void testEqual() {
        final Integer[] a1 = new Integer[]{1, 2, 3};
        final Integer[] a2 = new Integer[]{1, 2, 3};
        final Integer[] a3 = a1;
        final Integer[] a4 = new Integer[]{1, 3, 2};
        final Integer[] a5 = new Integer[]{1, 2, 3, 4};

        assertNotEquals(a1, a2);
        assertEquals(a1, a3);
        assertNotEquals(a1, a4);
        assertNotEquals(a1, a5);

        final ComparableArray<Integer> ca1 = new ComparableArray<>(a1);
        final ComparableArray<Integer> ca2 = new ComparableArray<>(a2);
        final ComparableArray<Integer> ca3 = new ComparableArray<>(a3);
        final ComparableArray<Integer> ca4 = new ComparableArray<>(a4);
        final ComparableArray<Integer> ca5 = new ComparableArray<>(a5);

        assertEquals(ca1, ca2);
        assertEquals(ca1, ca3);
        assertNotEquals(ca1, ca4);
        assertNotEquals(ca1, ca5);
    }
}