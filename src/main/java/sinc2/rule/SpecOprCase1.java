package sinc2.rule;

import java.util.Objects;

/**
 * The case 1 specialization operation, in the numerated form.
 *
 * @since 2.0
 */
public class SpecOprCase1 extends SpecOpr {
    public final int predIdx;
    public final int argIdx;
    public final int varId;

    public SpecOprCase1(int predIdx, int argIdx, int varId) {
        super(SpecOprCase.CASE1);
        this.predIdx = predIdx;
        this.argIdx = argIdx;
        this.varId = varId;
    }

    @Override
    public UpdateStatus specialize(Rule rule) {
        return rule.cvt1Uv2ExtLv(predIdx, argIdx, varId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecOprCase1 that = (SpecOprCase1) o;
        return predIdx == that.predIdx && argIdx == that.argIdx && varId == that.varId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), predIdx, argIdx, varId);
    }
}
