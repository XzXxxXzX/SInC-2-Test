package sinc.common;

import java.util.List;
import java.util.Set;

public class UpdateResult {
    public final List<Predicate[]> groundings;
    public final Set<Predicate> counterExamples;

    public UpdateResult(List<Predicate[]> groundings, Set<Predicate> counterExamples) {
        this.groundings = groundings;
        this.counterExamples = counterExamples;
    }
}
