package sinc2.common;

/**
 * Specify conventions of argument structures and operations.
 *
 * Integer value 0 is a special value denoting empty argument, other values using the lowest 31 bits are numerations
 * mapped to constant symbols and variables. The highest bit is a flag distinguishing variables from constants. If the
 * highest bit is 0, the argument value is a constant symbol; otherwise, it is a variable ID.
 *
 * The range of applicable constant is [1, 2^31 - 1]; [0, 2^31 - 1] for variable IDs.
 *
 * @since 2.0
 */
public class Argument {
    /** Zero is a special value denoting an empty argument, which is neither constant nor variable. */
    public static final int EMPTY_VALUE = 0;

    /** The highest bit of an integer flags a variable if it is 1. */
    public static final int FLAG_VARIABLE = 1 << 31;

    /** The highest bit of an integer flags a constant if it is 0. */
    public static final int FLAG_CONSTANT = ~FLAG_VARIABLE;

    /**
     * Encode a constant argument by its numeration value. The encoding discards the highest bit.
     *
     * @param numeration Numeration value of the constant symbol
     * @return The encoded argument
     */
    public static int constant(int numeration) {
        return FLAG_CONSTANT & numeration;
    }

    /**
     * Encode a variable argument by the variable ID. The encoding discards the highest bit.
     *
     * @param id Variable ID
     * @return The encoded argument
     */
    public static int variable(int id) {
        return FLAG_VARIABLE | id;
    }

    /**
     * Check if the argument is empty.
     */
    public static boolean isEmpty(int argument) {
        return 0 == argument;
    }

    /**
     * Check if the argument is non-empty.
     */
    public static boolean isNonEmpty(int argument) {
        return 0 != argument;
    }

    /**
     * Check if the argument is a variable (specifically, a limited variable; an unlimited variable is denoted by empty
     * value).
     */
    public static boolean isVariable(int argument) {
        return 0 != (FLAG_VARIABLE & argument);
    }

    /**
     * Check if the argument is a constant.
     */
    public static boolean isConstant(int argument) {
        return 0 != argument && 0 == (FLAG_VARIABLE & argument);
    }

    /**
     * Get the encoded value in the argument.
     */
    public static int decode(int argument) {
        return FLAG_CONSTANT & argument;
    }
}
