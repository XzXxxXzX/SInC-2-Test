package sinc2.rule;

/**
 * Parsed case 1 specialization operation
 *
 * @since 2.0
 */
public class ParsedSpecOprCase1 extends ParsedSpecOpr {
    public final int predIdx;
    public final int argIdx;
    public final int varId;

    public ParsedSpecOprCase1(int predIdx, int argIdx, int varId) {
        specCase = SpecOprCase.CASE1;
        this.predIdx = predIdx;
        this.argIdx = argIdx;
        this.varId = varId;
    }
}
