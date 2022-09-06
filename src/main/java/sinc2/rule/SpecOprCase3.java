package sinc2.rule;

import java.util.Objects;

/**
 * The case 3 specialization operation, in the numerated form.
 *
 * @since 2.0
 */
public class SpecOprCase3 extends SpecOpr {
    public final int predIdx1;
    public final int argIdx1;
    public final int predIdx2;
    public final int argIdx2;

    public SpecOprCase3(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        super(SpecOprCase.CASE3);
        this.predIdx1 = predIdx1;
        this.argIdx1 = argIdx1;
        this.predIdx2 = predIdx2;
        this.argIdx2 = argIdx2;
    }

    @Override
    public UpdateStatus specialize(Rule rule) {
        return rule.cvt2Uvs2NewLv(predIdx1, argIdx1, predIdx2, argIdx2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecOprCase3 that = (SpecOprCase3) o;
        return predIdx1 == that.predIdx1 && argIdx1 == that.argIdx1 && predIdx2 == that.predIdx2 && argIdx2 == that.argIdx2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), predIdx1, argIdx1, predIdx2, argIdx2);
    }
}
