package sinc.common;

import java.util.Objects;

public abstract class ArgIndicator {
    public final String functor;
    public final int idx;

    public ArgIndicator(String functor, int idx) {
        this.functor = functor;
        this.idx = idx;
    }

    public ArgIndicator(ArgIndicator another) {
        this.functor = another.functor;
        this.idx = another.idx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgIndicator that = (ArgIndicator) o;
        return idx == that.idx && Objects.equals(functor, that.functor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functor, idx);
    }
}
