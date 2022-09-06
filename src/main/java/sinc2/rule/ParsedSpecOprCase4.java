package sinc2.rule;

/**
 * Parsed case 4 specialization operation
 *
 * @since 2.0
 */
public class ParsedSpecOprCase4 extends ParsedSpecOpr {
    public final String functor;
    public final int arity;
    public final int argIdx1;
    public final int predIdx2;
    public final int argIdx2;

    public ParsedSpecOprCase4(String functor, int arity, int argIdx1, int predIdx2, int argIdx2) {
        specCase = SpecOprCase.CASE4;
        this.functor = functor;
        this.arity = arity;
        this.argIdx1 = argIdx1;
        this.predIdx2 = predIdx2;
        this.argIdx2 = argIdx2;
    }
}
