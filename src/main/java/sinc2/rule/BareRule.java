package sinc2.rule;

import sinc2.common.Predicate;
import sinc2.kb.Record;
import sinc2.util.MultiSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A rule implementation with barely nothing but the structure and fingerprint operations. This class is used for loading
 * Horn rules from strings. It can also be used for testing basic operations in the abstract class 'Rule' or for
 * manipulating the rule structure only.
 *
 * @since 1.0
 */
public class BareRule extends Rule {

    public Eval returningEval = new Eval(null, 0, 0, length);
    public double coverage = 1.0;
    public UpdateStatus case1PreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case1PostUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case2PreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case2PostUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case3PreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case3PostUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case4PreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case4PostUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case5PreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus case5PostUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus generalizationPreUpdateStatus = UpdateStatus.NORMAL;
    public UpdateStatus generalizationPostUpdateStatus = UpdateStatus.NORMAL;
    public EvidenceBatch returningEvidence = null;
    public Set<Record> returningCounterexamples = new HashSet<>();

    public BareRule(int headFunctor, int arity, Set<Fingerprint> fingerprintCache, Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap) {
        super(headFunctor, arity, fingerprintCache, category2TabuSetMap);
        returningEvidence = new EvidenceBatch(new int[]{headFunctor});
    }

    public BareRule(List<Predicate> structure, Set<Fingerprint> fingerprintCache, Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap) {
        super(structure, fingerprintCache, category2TabuSetMap);
        final int[] relations_in_rule = new int[structure.size()];
        for (int i = 0; i < relations_in_rule.length; i++) {
            relations_in_rule[i] = structure.get(i).functor;
        }
        returningEvidence = new EvidenceBatch(relations_in_rule);
    }

    public BareRule(Rule another) {
        super(another);
    }

    @Override
    public BareRule clone() {
        return new BareRule(this);
    }

    @Override
    protected Eval calculateEval() {
        return returningEval;
    }

    @Override
    public Eval getEval() {
        return returningEval;
    }

    @Override
    protected double recordCoverage() {
        return coverage;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(int predIdx, int argIdx, int varId) {
        return case1PreUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(int predIdx, int argIdx, int varId) {
        return case1PostUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(Predicate newPredicate, int argIdx, int varId) {
        return case2PreUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(Predicate newPredicate, int argIdx, int varId) {
        return case2PostUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        return case3PreUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(int predIdx1, int argIdx1, int predIdx2, int argIdx2) {
        return case3PostUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        return case4PreUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(Predicate newPredicate, int argIdx1, int predIdx2, int argIdx2) {
        return case4PostUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPreCvg(int predIdx, int argIdx, int constant) {
        return case5PreUpdateStatus;
    }

    @Override
    protected UpdateStatus cvt1Uv2ConstHandlerPostCvg(int predIdx, int argIdx, int constant) {
        return case5PostUpdateStatus;
    }

    @Override
    protected UpdateStatus rmAssignedArgHandlerPreCvg(int predIdx, int argIdx) {
        return generalizationPreUpdateStatus;
    }

    @Override
    protected UpdateStatus rmAssignedArgHandlerPostCvg(int predIdx, int argIdx) {
        return generalizationPostUpdateStatus;
    }

    @Override
    public EvidenceBatch getEvidenceAndMarkEntailment() {
        return returningEvidence;
    }

    @Override
    public Set<Record> getCounterexamples() {
        return returningCounterexamples;
    }
}
