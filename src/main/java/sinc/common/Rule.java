package sinc.common;

import sinc.util.DisjointSet;

import java.util.*;

public abstract class Rule {
    public static final int HEAD_PRED_IDX = 0;
    public static final int FIRST_BODY_PRED_IDX = HEAD_PRED_IDX + 1;
    public static final int CONSTANT_ARG_ID = -1;

    public static double MIN_FACT_COVERAGE = 0.0;
    public static RuleMonitor monitor = new RuleMonitor();

    public enum UpdateStatus {
        NORMAL, DUPLICATED, INVALID, INSUFFICIENT_COVERAGE, TABU_PRUNED
    }

    protected final List<Predicate> structure;
    protected final List<Variable> boundedVars;  // Bounded vars use non-negative ids(list index)
    protected final List<Integer> boundedVarCnts;
    protected RuleFingerPrint fingerPrint;
    protected int equivConds;
    protected Eval eval;
    protected final Set<RuleFingerPrint> searchedFingerprints;

    public Rule(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
        structure = new ArrayList<>();
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();

        final Predicate head = new Predicate(headFunctor, arity);
        structure.add(head);

        fingerPrint = new RuleFingerPrint(structure);
        equivConds = 0;
        eval = null;

        this.searchedFingerprints = searchedFingerprints;
        this.searchedFingerprints.add(fingerPrint);
    }

    public Rule(List<Predicate> structure, Set<RuleFingerPrint> searchedFingerprints) {
        this.structure = new ArrayList<>(structure);
        boundedVars = new ArrayList<>();
        boundedVarCnts = new ArrayList<>();

        int max_var_id = -1;
        for (Predicate p: this.structure) {
            for (Argument argument: p.args) {
                if (null != argument) {
                    if (argument.isVar) {
                        max_var_id = Math.max(max_var_id, argument.id);
                    }
                    this.equivConds++;
                }
            }
        }
        this.equivConds -= max_var_id + 1;

        for (int var_id = 0; var_id <= max_var_id; var_id++) {
            this.boundedVars.add(new Variable(var_id));
            this.boundedVarCnts.add(0);
        }
        for (Predicate p: this.structure) {
            for (int arg_idx = 0; arg_idx < p.arity(); arg_idx++) {
                final Argument argument = p.args[arg_idx];
                if (null != argument && argument.isVar) {
                    p.args[arg_idx] = this.boundedVars.get(argument.id);
                    this.boundedVarCnts.set(argument.id, this.boundedVarCnts.get(argument.id) + 1);
                }
            }
        }

        this.fingerPrint = new RuleFingerPrint(this.structure);
        this.searchedFingerprints = searchedFingerprints;
        this.searchedFingerprints.add(fingerPrint);
    }

    public Rule(Rule another) {
        this.structure = new ArrayList<>(another.structure.size());
        for (Predicate predicate: another.structure) {
            this.structure.add(new Predicate(predicate));
        }
        this.boundedVars = new ArrayList<>(another.boundedVars);
        this.boundedVarCnts = new ArrayList<>(another.boundedVarCnts);
        this.fingerPrint = another.fingerPrint;
        this.equivConds = another.equivConds;
        this.eval = another.eval;
        this.searchedFingerprints = another.searchedFingerprints;
    }

    public abstract Rule clone();

    public Predicate getPredicate(int idx) {
        return structure.get(idx);
    }

    public Predicate getHead() {
        return structure.get(HEAD_PRED_IDX);
    }

    public int length() {
        return structure.size();
    }

    public int usedBoundedVars() {
        return boundedVars.size();
    }

    public int size() {
        return equivConds;
    }

    public Eval getEval() {
        return eval;
    }

    /**
     * ?????????????????????Invalid???
     *   1. Trivial
     *   2. Independent Fragment
     */
    protected boolean isInvalid() {
        /* Independent Fragment(????????????origin???????????????) */
        /* ?????????????????? */
        /* Assumption: ???????????????Free Var???Const???Pred(??????head)??????????????????Bounded Var???????????????Pred??????????????????????????? */
        DisjointSet disjoint_set = new DisjointSet(usedBoundedVars());

        /* Trivial(???Set??????) */
        /* 1. ???Set?????? */
        /* 2. ?????????????????????Head???????????????????????????Head??????????????????????????????????????? */
        Predicate head_pred = structure.get(HEAD_PRED_IDX);
        {
            /* ??????Head??????????????????????????????disjoint set */
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                Argument argument = head_pred.args[arg_idx];
                if (null != argument && argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }
            if (bounded_var_ids.isEmpty()) {
                if (structure.size() >= 2) {
                    /* Head?????????bounded var??????body??????????????????head?????????independent fragment */
                    return true;
                }
            } else {
                /* ???????????????????????????Head??????????????????Bounded Var */
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        Set<Predicate> predicate_set = new HashSet<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            Predicate body_pred = structure.get(pred_idx);
            if (head_pred.functor.equals(body_pred.functor)) {
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    Argument head_arg = head_pred.args[arg_idx];
                    Argument body_arg = body_pred.args[arg_idx];
                    if (null != head_arg && head_arg.equals(body_arg)) {
                        return true;
                    }
                }
            }

            boolean args_complete = true;
            List<Integer> bounded_var_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                Argument argument = body_pred.args[arg_idx];
                if (null == argument) {
                    args_complete = false;
                } else if (argument.isVar) {
                    bounded_var_ids.add(argument.id);
                }
            }

            if (args_complete) {
                if (!predicate_set.add(body_pred)) {
                    return true;
                }
            }

            /* ????????????Predicate????????????Bounded Var???????????????????????? */
            if (bounded_var_ids.isEmpty()) {
                /* ??????body???pred?????????bounded var????????????independent fragment */
                return true;
            } else {
                int first_id = bounded_var_ids.get(0);
                for (int i = 1; i < bounded_var_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, bounded_var_ids.get(i));
                }
            }
        }

        /* ??????????????????Independent Fragment */
        return 2 <= disjoint_set.totalSets();
    }

    /**
     * ????????????????????????FV????????????????????????BV
     *
     * @return ????????????????????????????????????????????????true???????????????false
     */
    public UpdateStatus boundFreeVar2ExistingVar(
            final int predIdx, final int argIdx, final int varId
    ) {
        /* ??????Rule?????? */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(predIdx, argIdx, varId);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(predIdx, argIdx, varId);
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint boundFreeVar2ExistingVarUpdateStructure(
            final int predIdx, final int argIdx, final int varId
    ) {
        final Predicate target_predicate = structure.get(predIdx);
        target_predicate.args[argIdx] = boundedVars.get(varId);
        boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
        equivConds++;
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus boundFreeVar2ExistingVarHandler(
            final int predIdx, final int argIdx, final int varId
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * ???????????????Predicate???????????????????????????FV????????????????????????BV
     *
     * @return ????????????????????????????????????????????????true???????????????false
     */
    public UpdateStatus boundFreeVar2ExistingVar(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        /* ??????Rule?????? */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ExistingVarUpdateStructure(functor, arity, argIdx, varId);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = boundFreeVar2ExistingVarHandler(structure.get(structure.size() - 1), argIdx, varId);
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint boundFreeVar2ExistingVarUpdateStructure(
            final String functor, final int arity, final int argIdx, final int varId
    ) {
        final Predicate target_predicate = new Predicate(functor, arity);
        structure.add(target_predicate);
        target_predicate.args[argIdx] = boundedVars.get(varId);
        boundedVarCnts.set(varId, boundedVarCnts.get(varId)+1);
        equivConds++;
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus boundFreeVar2ExistingVarHandler(
            final Predicate newPredicate, final int argIdx, final int varId
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * ??????????????????FV????????????????????????BV
     *
     * @return ????????????????????????????????????????????????true???????????????false
     */
    public UpdateStatus boundFreeVars2NewVar(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* ??????Rule?????? */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVars2NewVarUpdateStructure(predIdx1, argIdx1, predIdx2, argIdx2);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(predIdx1, argIdx1, predIdx2, argIdx2);
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint boundFreeVars2NewVarUpdateStructure(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        final Predicate target_predicate1 = structure.get(predIdx1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final Variable new_var = new Variable(boundedVars.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        boundedVars.add(new_var);
        boundedVarCnts.add(2);
        equivConds++;
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus boundFreeVars2NewVarHandler(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * ??????????????????Predicate???????????????????????????FV?????????????????????FV????????????????????????BV
     *
     * @return ????????????????????????????????????????????????true???????????????false
     */
    public UpdateStatus boundFreeVars2NewVar(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        /* ??????Rule?????? */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVars2NewVarUpdateStructure(functor, arity, argIdx1, predIdx2, argIdx2);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = boundFreeVars2NewVarHandler(
                structure.get(structure.size() - 1), argIdx1, predIdx2, argIdx2
        );
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint boundFreeVars2NewVarUpdateStructure(
            final String functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        final Predicate target_predicate1 = new Predicate(functor, arity);
        structure.add(target_predicate1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final Variable new_var = new Variable(boundedVars.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        boundedVars.add(new_var);
        boundedVarCnts.add(2);
        equivConds++;
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus boundFreeVars2NewVarHandler(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    /**
     * ??????????????????FV???????????????
     *
     * @return ????????????????????????????????????????????????true???????????????false
     */
    public UpdateStatus boundFreeVar2Constant(final int predIdx, final int argIdx, final String constantSymbol) {
        /* ??????Rule?????? */
        long time_start_nano = System.nanoTime();
        fingerPrint = boundFreeVar2ConstantUpdateStructure(predIdx, argIdx, constantSymbol);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = boundFreeVar2ConstantHandler(predIdx, argIdx, constantSymbol);
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint boundFreeVar2ConstantUpdateStructure(
            final int predIdx, final int argIdx, final String constantSymbol
    ) {
        final Predicate predicate = structure.get(predIdx);
        predicate.args[argIdx] = new Constant(CONSTANT_ARG_ID, constantSymbol);
        equivConds++;
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus boundFreeVar2ConstantHandler(final int predIdx, final int argIdx, final String constantSymbol) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    public UpdateStatus removeBoundedArg(final int predIdx, final int argIdx) {
        long time_start_nano = System.nanoTime();
        fingerPrint = removeBoundedArgUpdateStructure(predIdx, argIdx);
        long time_fp_updated_nano = System.nanoTime();
        monitor.updateFingerPrintTimeNano += time_fp_updated_nano - time_start_nano;

        /* ??????????????????Cache */
        boolean cache_hit = !searchedFingerprints.add(fingerPrint);
        long time_cache_checked_nano = System.nanoTime();
        monitor.dupCheckTimeNano += time_cache_checked_nano - time_fp_updated_nano;
        if (cache_hit) {
            return UpdateStatus.DUPLICATED;
        }

        /* ??????????????? */
        boolean invalid = isInvalid();
        long time_valid_checked_nano = System.nanoTime();
        monitor.validCheckTimeNano += time_valid_checked_nano - time_cache_checked_nano;
        if (invalid) {
            return UpdateStatus.INVALID;
        }

        /* ??????handler */
        final UpdateStatus status = removeBoundedArgHandler(predIdx, argIdx);
        long time_updated_nano = System.nanoTime();
        monitor.updateHandlerTimeNano += time_updated_nano - time_valid_checked_nano;
        if (UpdateStatus.NORMAL != status) {
            return status;
        }

        /* ??????Eval */
        this.eval = calculateEval();
        long time_evaluated_nano = System.nanoTime();
        monitor.evalTimeNano += time_evaluated_nano - time_updated_nano;
        return UpdateStatus.NORMAL;
    }

    protected RuleFingerPrint removeBoundedArgUpdateStructure(final int predIdx, final int argIdx) {
        final Predicate predicate = structure.get(predIdx);
        final Argument argument = predicate.args[argIdx];
        predicate.args[argIdx] = null;

        /* ????????????????????????????????????????????????????????????????????? */
        if (argument.isVar) {
            final Integer var_uses_cnt = boundedVarCnts.get(argument.id);
            if (2 >= var_uses_cnt) {
                /* ???????????????var????????????var????????? */
                /* ??????????????????????????????????????????var */
                int last_var_idx = boundedVars.size() - 1;
                Variable last_var = boundedVars.remove(last_var_idx);
                boundedVarCnts.set(argument.id, boundedVarCnts.get(last_var_idx));
                boundedVarCnts.remove(last_var_idx);

                /* ???????????????????????????????????????????????????????????????????????? */
                for (Predicate another_predicate : structure) {
                    for (int i = 0; i < another_predicate.arity(); i++) {
                        if (null != another_predicate.args[i]) {
                            if (argument.id == another_predicate.args[i].id) {
                                another_predicate.args[i] = null;
                            }
                        }
                    }
                }

                if (argument != last_var) {
                    for (Predicate another_predicate : structure) {
                        for (int i = 0; i < another_predicate.arity(); i++) {
                            if (null != another_predicate.args[i]) {
                                if (last_var.id == another_predicate.args[i].id) {
                                    another_predicate.args[i] = argument;
                                }
                            }
                        }
                    }
                }
            } else {
                /* ????????????????????? */
                boundedVarCnts.set(argument.id, var_uses_cnt - 1);
            }
        }
        equivConds--;

        /* ????????????????????????????????????predicate?????????????????????(head??????) */
        Iterator<Predicate> itr = structure.iterator();
        Predicate head_pred = itr.next();
        while (itr.hasNext()) {
            Predicate body_pred = itr.next();
            boolean is_empty_pred = true;
            for (Argument arg_info: body_pred.args) {
                if (null != arg_info) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }
        return new RuleFingerPrint(structure);
    }

    protected UpdateStatus removeBoundedArgHandler(final int predIdx, final int argIdx) {
        if (MIN_FACT_COVERAGE >= factCoverage()) {
            return UpdateStatus.INSUFFICIENT_COVERAGE;
        }
        return UpdateStatus.NORMAL;
    }

    protected abstract double factCoverage();

    /**
     * @return ???????????????Head Coverage?????????Eval.MIN
     */
    protected abstract Eval calculateEval();

    public RuleFingerPrint getFingerPrint() {
        return fingerPrint;
    }

    public List<VarIndicator> getVarLocations(int varId) {
        List<VarIndicator> result = new ArrayList<>();
        if (varId < boundedVars.size()) {
            for (Predicate predicate: structure) {
                for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                    Argument argument = predicate.args[arg_idx];
                    if (null != argument && argument.isVar && varId == argument.id) {
                        result.add(new VarIndicator(predicate.functor, arg_idx));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString()).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }

    public String toCompleteRuleString() {
        /* ??????Free vars????????? */
        /* Todo: ??????????????????FV???name???????????????????????????Y0, Y1... ???X0, X1... ????????? */
        List<Predicate> copy = new ArrayList<>(this.structure.size());
        for (Predicate predicate: structure) {
            copy.add(new Predicate(predicate));
        }
        int free_id = usedBoundedVars();
        for (Predicate predicate: copy) {
            for (int i = 0; i < predicate.arity(); i++) {
                if (null == predicate.args[i]) {
                    predicate.args[i] = new Variable(free_id);
                    free_id++;
                }
            }
        }

        /* to string without eval */
        StringBuilder builder = new StringBuilder(copy.get(0).toString());
        builder.append(":-");
        if (1 < copy.size()) {
            builder.append(copy.get(1).toString());
            for (int i = 2; i < copy.size(); i++) {
                builder.append(',').append(copy.get(i).toString());
            }
        }
        return builder.toString();
    }

    public String toDumpString() {
        StringBuilder builder = new StringBuilder(structure.get(0).toString());
        builder.append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return this.fingerPrint.equals(that.fingerPrint);
    }

    @Override
    public int hashCode() {
        return fingerPrint.hashCode();
    }
}
