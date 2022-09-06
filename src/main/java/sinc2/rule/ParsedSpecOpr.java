package sinc2.rule;

/**
 * The base class for representing specializations parsed from a parsed rule structure.
 *
 * @since 2.0
 */
abstract public class ParsedSpecOpr {
    /** The case of the specialization operation */
    protected SpecOprCase specCase = null;

    public SpecOprCase getSpecCase() {
        return specCase;
    }
}
