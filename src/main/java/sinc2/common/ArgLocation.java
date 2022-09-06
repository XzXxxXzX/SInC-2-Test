package sinc2.common;

import java.util.Objects;

/**
 * Objects of this class denote locations of arguments in a rule.
 *
 * @since 2.0
 */
public class ArgLocation {
    public final int predIdx;
    public final int argIdx;

    public ArgLocation(int predIdx, int argIdx) {
        this.predIdx = predIdx;
        this.argIdx = argIdx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgLocation that = (ArgLocation) o;
        return predIdx == that.predIdx && argIdx == that.argIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predIdx, argIdx);
    }
}
