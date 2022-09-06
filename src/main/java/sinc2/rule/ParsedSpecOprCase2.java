package sinc2.rule;

/**
 * Parsed case 2 specialization operation
 *
 * @since 2.0
 */
public class ParsedSpecOprCase2 extends ParsedSpecOpr {
    public final String functor;
    public final int arity;
    public final int argIdx;
    public final int varId;

    public ParsedSpecOprCase2(String functor, int arity, int argIdx, int varId) {
        specCase = SpecOprCase.CASE2;
        this.functor = functor;
        this.arity = arity;
        this.argIdx = argIdx;
        this.varId = varId;
    }
}
