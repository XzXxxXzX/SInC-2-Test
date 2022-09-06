package sinc.common;

public class KbStatistics {
    public final int facts;
    public final int functors;
    public final int constants;
    public final int actualConstantSubstitutions;
    public final int totalConstantSubstitutions;
    public final long similarColumnPairs;
    public final long totalColumnPairs;

    public KbStatistics(
            int facts, int functors, int constants, int actualConstantSubstitutions, int totalConstantSubstitutions,
            long similarColumnPairs, long totalColumnPairs
    ) {
        this.facts = facts;
        this.functors = functors;
        this.constants = constants;
        this.actualConstantSubstitutions = actualConstantSubstitutions;
        this.totalConstantSubstitutions = totalConstantSubstitutions;
        this.similarColumnPairs = similarColumnPairs;
        this.totalColumnPairs = totalColumnPairs;
    }
}
