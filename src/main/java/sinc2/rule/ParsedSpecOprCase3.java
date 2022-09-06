package sinc2.rule;

/**
 * Parsed case 3 specialization operation
 *
 * @since 2.0
 */
public class ParsedSpecOprCase3 extends ParsedSpecOpr {
    public final int predIdx1;
    public final int argIdx1;
    public final int predIdx2;
    public final int argIdx2;

    public ParsedSpecOprCase3(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        specCase = SpecOprCase.CASE3;
        this.predIdx1 = predIdx1;
        this.argIdx1 = argIdx1;
        this.predIdx2 = predIdx2;
        this.argIdx2 = argIdx2;
    }
}
