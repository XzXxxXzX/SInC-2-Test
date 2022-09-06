package sinc2.common;

import java.util.Objects;

/**
 * The indicator class used in the fingerprint of rules. An indicator denotes a constant or a location in a predicate.
 *
 * @since 1.0
 */
public class ArgIndicator {
    public final int functor;
    public final int idx;

    protected ArgIndicator(int functor, int idx) {
        this.functor = functor;
        this.idx = idx;
    }

    /**
     * Create an indicator of a constant
     *
     * @param constNumeration The numeration of the constant name
     * @return The constant indicator
     */
    public static ArgIndicator getConstantIndicator(int constNumeration) {
        return new ArgIndicator(constNumeration, -1);
    }

    /**
     * Create an indicator of a variable
     *
     * @param functor The numeration of the functor name
     * @param idx The argument index of the variable in the predicate
     * @return The variable indicator
     */
    public static ArgIndicator getVariableIndicator(int functor, int idx) {
        return new ArgIndicator(functor, idx);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgIndicator that = (ArgIndicator) o;
        return functor == that.functor && idx == that.idx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(functor, idx);
    }
}
