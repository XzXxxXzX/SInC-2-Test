package sinc.common;

import java.util.List;
import java.util.Set;

/**
 * 只用于更新rule structure
 */
public class BareRule extends Rule {
    public BareRule(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
        super(headFunctor, arity, searchedFingerprints);
    }

    public BareRule(List<Predicate> structure, Set<RuleFingerPrint> searchedFingerprints) {
        super(structure, searchedFingerprints);
    }

    public BareRule(Rule another) {
        super(another);
    }

    @Override
    public BareRule clone() {
        return new BareRule(this);
    }

    @Override
    protected double factCoverage() {
        /* 不需要 */
        return 0;
    }

    @Override
    protected Eval calculateEval() {
        /* 不需要 */
        return null;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(predIdx, argIdx, varId);
        return null;
    }

    public UpdateStatus boundFreeVar2ExistingVar(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(functor, arity, argIdx, varId);
        return null;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        fingerPrint = boundFreeVars2NewVarUpdateStructure(predIdx1, argIdx1, predIdx2, argIdx2);
        return null;
    }

    public UpdateStatus boundFreeVars2NewVar(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        fingerPrint = boundFreeVars2NewVarUpdateStructure(functor, arity, argIdx1, predIdx2, argIdx2);
        return null;
    }

    public UpdateStatus boundFreeVar2Constant(final int predIdx, final int argIdx, final String constantSymbol) {
        fingerPrint = boundFreeVar2ConstantUpdateStructure(predIdx, argIdx, constantSymbol);
        return null;
    }
}
