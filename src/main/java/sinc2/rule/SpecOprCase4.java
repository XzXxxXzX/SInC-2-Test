package sinc2.rule;

import java.util.Objects;

/**
 * The case 4 specialization operation, in the numerated form.
 *
 * @since 2.0
 */
public class SpecOprCase4 extends SpecOpr {
    public final int functor;
    public final int arity;
    public final int argIdx1;
    public final int predIdx2;
    public final int argIdx2;

    public SpecOprCase4(int functor, int arity, int argIdx1, int predIdx2, int argIdx2) {
        super(SpecOprCase.CASE4);
        this.functor = functor;
        this.arity = arity;
        this.argIdx1 = argIdx1;
        this.predIdx2 = predIdx2;
        this.argIdx2 = argIdx2;
    }

    @Override
    public UpdateStatus specialize(Rule rule) {
        return rule.cvt2Uvs2NewLv(functor, arity, argIdx1, predIdx2, argIdx2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecOprCase4 that = (SpecOprCase4) o;
        return functor == that.functor && arity == that.arity && argIdx1 == that.argIdx1 && predIdx2 == that.predIdx2 && argIdx2 == that.argIdx2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), functor, arity, argIdx1, predIdx2, argIdx2);
    }
}
