package sinc2.rule;

import java.util.Objects;

/**
 * The case 5 specialization operation, in the numerated form.
 *
 * @since 2.0
 */
public class SpecOprCase5 extends SpecOpr {
    public final int predIdx;
    public final int argIdx;
    public final int constant;

    public SpecOprCase5(int predIdx, int argIdx, int constant) {
        super(SpecOprCase.CASE5);
        this.predIdx = predIdx;
        this.argIdx = argIdx;
        this.constant = constant;
    }

    @Override
    public UpdateStatus specialize(Rule rule) {
        return rule.cvt1Uv2Const(predIdx, argIdx, constant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecOprCase5 that = (SpecOprCase5) o;
        return predIdx == that.predIdx && argIdx == that.argIdx && constant == that.constant;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), predIdx, argIdx, constant);
    }
}
