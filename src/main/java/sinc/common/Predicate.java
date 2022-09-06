package sinc.common;

import java.util.Arrays;
import java.util.Objects;

public class Predicate {
    public final String functor;
    public final Argument[] args;

    public Predicate(String functor, int arity) {
        this.functor = functor;
        args = new Argument[arity];
    }

    public Predicate(Predicate another) {
        this.functor = another.functor;
        this.args = another.args.clone();
    }

    public int arity() {
        return args.length;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(functor);
        builder.append('(');
        if (0 < args.length) {
            builder.append((null == args[0]) ? "?" : args[0].name);
            for (int i = 1; i < args.length; i++) {
                builder.append(',').append((null == args[i]) ? "?" : args[i].name);
            }
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return Objects.equals(functor, predicate.functor) && Arrays.equals(args, predicate.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(functor);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
