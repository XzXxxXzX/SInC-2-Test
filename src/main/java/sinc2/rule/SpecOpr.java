package sinc2.rule;

import java.util.Objects;

/**
 * The base class of the five specialization operations, in the numerated form.
 *
 * @since 2.0
 */
abstract public class SpecOpr {
    /** The case of the specialization operation */
    protected final SpecOprCase specCase;

    public SpecOpr(SpecOprCase specCase) {
        this.specCase = specCase;
    }

    public SpecOprCase getSpecCase() {
        return specCase;
    }

    /**
     * Apply the specialization operation on the rule
     *
     * @param rule The rule that is going to be specialized
     */
    abstract public UpdateStatus specialize(Rule rule);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecOpr specOpr = (SpecOpr) o;
        return specCase == specOpr.specCase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(specCase);
    }
}
