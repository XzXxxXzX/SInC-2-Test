package sinc2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentTest {
    @Test
    void testStaticValues() {
        assertEquals(0x80000000, Argument.FLAG_VARIABLE);
        assertEquals(~0x80000000, Argument.FLAG_CONSTANT);
    }

    @Test
    void testEncode() {
        int[] integers = new int[]{1, 25, 0x1<<30, 0xbadbeef};
        for (int n: integers) {
            int constant = Argument.constant(n);
            int variable = Argument.variable(n);
            assertNotEquals(constant, variable);
            assertFalse(Argument.isVariable(constant));
            assertTrue(Argument.isVariable(variable));
            assertFalse(Argument.isEmpty(constant));
            assertFalse(Argument.isEmpty(variable));
            assertEquals(n, Argument.decode(constant));
            assertEquals(n, Argument.decode(variable));
        }
    }
}