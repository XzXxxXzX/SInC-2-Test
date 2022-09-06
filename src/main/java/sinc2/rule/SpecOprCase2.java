package sinc2.rule;

import java.util.Objects;

/**
 * The case 2 specialization operation, in the numerated form.
 *
 * @since 2.0
 */
public class SpecOprCase2 extends SpecOpr {
    public final int functor;
    public final int arity;
    public final int argIdx;
    public final int varId;

    public SpecOprCase2(int functor, int arity, int argIdx, int varId) {
        super(SpecOprCase.CASE2);
        this.functor = functor;
        this.arity = arity;
        this.argIdx = argIdx;
        this.varId = varId;
    }

    @Override
    public UpdateStatus specialize(Rule rule) {
        return rule.cvt1Uv2ExtLv(functor, arity, argIdx, varId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecOprCase2 that = (SpecOprCase2) o;
        return functor == that.functor && arity == that.arity && argIdx == that.argIdx && varId == that.varId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), functor, arity, argIdx, varId);
    }
}
