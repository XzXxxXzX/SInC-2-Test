package sinc2.rule;

/**
 * Parsed case 5 specialization operation
 *
 * @since 2.0
 */
public class ParsedSpecOprCase5 extends ParsedSpecOpr {
    public final int predIdx;
    public final int argIdx;
    public final String constant;

    public ParsedSpecOprCase5(int predIdx, int argIdx, String constant) {
        specCase = SpecOprCase.CASE5;
        this.predIdx = predIdx;
        this.argIdx = argIdx;
        this.constant = constant;
    }
}
