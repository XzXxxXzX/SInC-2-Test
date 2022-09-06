package sinc2.common;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class is for the predicates parsed from plain-text strings.
 *
 * @since 2.0
 */
public class ParsedPred {
    public final String functor;
    public final ParsedArg[] args;

    public ParsedPred(String functor, ParsedArg[] args) {
        this.functor = functor;
        this.args = args;
    }

    public ParsedPred(String functor, int arity) {
        this.functor = functor;
        this.args = new ParsedArg[arity];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedPred that = (ParsedPred) o;
        return Objects.equals(functor, that.functor) && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(functor);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
