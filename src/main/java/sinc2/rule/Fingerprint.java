package sinc2.rule;

import sinc2.common.ArgIndicator;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.util.MultiSet;

import java.util.*;

/**
 * The fingerprint class that quickly tells whether two rules are equivalent.
 *
 * Note: The fingerprint structure may incorrectly tell two rules are identical. For instance:
 *
 *   1.1 p(X,Y) :- f(X,X), f(?,Y)
 *   1.2 p(X,Y) :- f(X,Y), f(?,X)
 *
 *   2.1 p(X,Y) :- f(X,?), f(Z,Y), f(?,Z)
 *   2.2 p(X,Y) :- f(X,Z), f(?,Y), f(Z,?)
 *
 * @since 1.0
 */
public class Fingerprint {

    /**
     * The equivalence class of argument indicators with head labels
     */
    static class LabeledEquivalenceClass {
        MultiSet<ArgIndicator> equivalenceClass = new MultiSet<>();
        List<Integer> headLabels = null;

        public LabeledEquivalenceClass() {}

        /**
         * Add a label, i.e., an index in the head, to the equivalent class.
         */
        public void addLabel(int headIdx) {
            if (null == headLabels) {
                headLabels = new ArrayList<>();
            }
            headLabels.add(headIdx);
        }

        /**
         * Add an indicator to the equivalence class.
         */
        public void addArgIndicator(ArgIndicator indicator) {
            equivalenceClass.add(indicator);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabeledEquivalenceClass that = (LabeledEquivalenceClass) o;
            return Objects.equals(equivalenceClass, that.equivalenceClass) && Objects.equals(headLabels, that.headLabels);
        }

        @Override
        public int hashCode() {
            return Objects.hash(equivalenceClass, headLabels);
        }
    }

    /** The head functor of the rule */
    protected final int headFunctor;

    /** The labeled equivalence classes */
    protected final MultiSet<LabeledEquivalenceClass> labeledEquivalenceClasses = new MultiSet<>();

    /** The rule that generates the fingerprint */
    protected final List<Predicate> rule;

    /**
     * Construct the fingerprint with the structure of the rule.
     *
     * @param rule The rule structure (ordered list of predicates, where the head is the first)
     */
    public Fingerprint(List<Predicate> rule) {
        this.rule = rule;
        final Predicate head_predicate = rule.get(0);
        headFunctor = head_predicate.functor;

        /* Count the number of LVs */
        /* Assumption: The IDs for the variables start from 0 and are continuous */
        int max_lv_id = -1;
        for (Predicate predicate: rule) {
            for (int argument: predicate.args) {
                if (Argument.isVariable(argument)) {
                    max_lv_id = Math.max(max_lv_id, Argument.decode(argument));
                }
            }
        }
        LabeledEquivalenceClass[] lv_equiv_classes = new LabeledEquivalenceClass[max_lv_id+1];
        for (int i = 0; i < lv_equiv_classes.length; i++) {
            lv_equiv_classes[i] = new LabeledEquivalenceClass();
        }

        /* Add every argument to the corresponding equivalence class */
        for (int arg_idx = 0; arg_idx < head_predicate.arity(); arg_idx++) {  // Convert the head first
            int argument = head_predicate.args[arg_idx];
            if (Argument.isEmpty(argument)) {
                LabeledEquivalenceClass lec = new LabeledEquivalenceClass();
                lec.addArgIndicator(ArgIndicator.getVariableIndicator(headFunctor, arg_idx));
                lec.addLabel(arg_idx);
                labeledEquivalenceClasses.add(lec);
            } else if (Argument.isVariable(argument)) {
                int var_id = Argument.decode(argument);
                LabeledEquivalenceClass lec = lv_equiv_classes[var_id];
                lec.addArgIndicator(ArgIndicator.getVariableIndicator(headFunctor, arg_idx));
                lec.addLabel(arg_idx);
            } else {
                int constant = Argument.decode(argument);
                LabeledEquivalenceClass lec = new LabeledEquivalenceClass();
                lec.addArgIndicator(ArgIndicator.getVariableIndicator(headFunctor, arg_idx));
                lec.addArgIndicator(ArgIndicator.getConstantIndicator(constant));
                lec.addLabel(arg_idx);
                labeledEquivalenceClasses.add(lec);
            }
        }
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < rule.size(); pred_idx++) {  // Convert the body
            Predicate predicate = rule.get(pred_idx);
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                int argument = predicate.args[arg_idx];
                if (Argument.isEmpty(argument)) {
                    LabeledEquivalenceClass lec = new LabeledEquivalenceClass();
                    lec.addArgIndicator(ArgIndicator.getVariableIndicator(predicate.functor, arg_idx));
                    labeledEquivalenceClasses.add(lec);
                } else if (Argument.isVariable(argument)) {
                    int var_id = Argument.decode(argument);
                    LabeledEquivalenceClass lec = lv_equiv_classes[var_id];
                    lec.addArgIndicator(ArgIndicator.getVariableIndicator(predicate.functor, arg_idx));
                } else {
                    int constant = Argument.decode(argument);
                    LabeledEquivalenceClass lec = new LabeledEquivalenceClass();
                    lec.addArgIndicator(ArgIndicator.getVariableIndicator(predicate.functor, arg_idx));
                    lec.addArgIndicator(ArgIndicator.getConstantIndicator(constant));
                    labeledEquivalenceClasses.add(lec);
                }
            }
        }

        /* Add the equivalent classes for the LVs to the fingerprint */
        labeledEquivalenceClasses.addAll(lv_equiv_classes);
    }

    public int getHeadFunctor() {
        return headFunctor;
    }

    public MultiSet<LabeledEquivalenceClass> getLabeledEquivalenceClasses() {
        return labeledEquivalenceClasses;
    }

    /**
     * Check for generalizations.
     *
     * @param another The fingerprint of another rule
     * @return Ture if the rule of this fingerprint is the generalization of the other.
     */
    public boolean generalizationOf(Fingerprint another) {
        final Set<LabeledEquivalenceClass> this_eqv_classes = labeledEquivalenceClasses.distinctValues();
        final Set<LabeledEquivalenceClass> another_eav_classes = another.labeledEquivalenceClasses.distinctValues();
        for (LabeledEquivalenceClass this_eqv_cls: this_eqv_classes) {
            boolean found_superset = false;
            for (LabeledEquivalenceClass another_eqv_cls: another_eav_classes) {
                if (this_eqv_cls.equivalenceClass.subsetOf(another_eqv_cls.equivalenceClass)) {
                    found_superset = true;
                    break;
                }
            }
            if (!found_superset) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fingerprint that = (Fingerprint) o;
        return headFunctor == that.headFunctor && Objects.equals(labeledEquivalenceClasses, that.labeledEquivalenceClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headFunctor, labeledEquivalenceClasses);
    }
}
