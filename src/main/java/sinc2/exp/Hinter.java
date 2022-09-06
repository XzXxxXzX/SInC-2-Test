package sinc2.exp;

import sinc2.impl.base.CachedRule;
import sinc2.kb.KbException;
import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;
import sinc2.rule.*;
import sinc2.util.ArrayOperation;
import sinc2.util.MultiSet;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class reads a hint template and find evaluation of instantiated rules.
 *
 * The structure of the template file contains n+2 lines:
 *   - The first two lines are the settings of the thresholds of "Fact Coverage" and "τ". The output rules must satisfy
 *     the restrictions (evaluations no smaller than the thresholds);
 *   - Each of the following lines is a template of rules, and the structure is:
 *     <Horn Rule Template>;[<Restrictions>]
 *     - Horn rule template complies the grammar of the following context-free-grammar (similar to Prolog):
 *
 *       line := template_rule;[restrictions]
 *       template_rule := predicate:-body
 *       body := ε | predicate | predicate,body
 *       predicate := pred_symbol(args)
 *       args := ε | variable | constant | variable,args | constant,args
 *       restrictions := ε | sym_tuple | sym_tuple,restrictions
 *       sym_tuple := (sym_list)
 *       sym_list := pred_symbol | pred_symbol,sym_list
 *
 *       A "variable" is defined by the following regular expression: [A-Z][a-zA-z0-9]*
 *       A "pred_symbol" and a "constant" are defined by the following regex: [a-z][a-zA-z0-9]*
 *     - The restrictions are a list of predicate symbol tuples. Each tuple of predicate symbols mean they cannot be all
 *       instantiated to the same predicate symbol in the same rule.
 *       For example, let "(p, q, r)" be a restriction tuple, then the following instantiation of the three predicates
 *       are valid:
 *         - p = father, q = mother, r = gender
 *         - p = parent, q = parent, r = grandparent
 *       But the following is invalid:
 *         - p = father, q = father, r = father
 *       If we need to represent that p, q, and r are mutually different from each other, then we can use three pairs
 *       instead: [(p, q), (p, r), (q, r)].
 *
 * The output is a "rules_<KB>.tsv" file containing n+1 rows and 7 columns:
 *   - The first is the title row. Other rows are in the descent order of τ(r)
 *   - The columns are:
 *     1. Rule: An instance of the template, written as r.
 *     2. |r|
 *     3. E^+_r
 *     4. E^-_r
 *     5. Fact Coverage of r
 *     6. τ(r)
 *     7. δ(r)
 *
 * The following is an example "template.hint" file:
 *
 *   0.2
 *   0.8
 *   p(X,Y):-q(Y,X);[]
 *   p(X,Y):-q(X,Z),r(Z,Y);[(p,q),(p,r)]
 *
 * The following is an example output "rules_someKB.tsv" file:
 *
 *   rule	|r|	E+	E-	FC	τ	δ
 *   friend(X,Y):-friend(Y,X)	2	10	2	0.5	0.71	6
 *   grandparent(X,Y):-parent(X,Z),parent(Z,Y)	3	150	0	0.9375	0.98	147
 *
 * Todo: Add some statistics of the result
 *
 * @since 2.0
 */
public class Hinter {
    /** A special value for the instantiation of the predicate symbols meaning it has not yet been instantiated */
    protected static final int UNDETERMINED = -1;

    /**
     * A structure that records a successfully instantiated rule and its score.
     */
    protected static class CollectedRuleInfo {
        final String rule;
        final double score;

        public CollectedRuleInfo(String rule, double score) {
            this.rule = rule;
            this.score = score;
        }
    }

    protected final String kbPath;
    protected final String kbName;
    protected final String hintFilePath;
    protected final Path outputFilePath;

    /** The target KB */
    protected NumeratedKb kb;
    /** The numerations of the relations in the KB */
    protected int[] kbRelationNums;
    /** The arities of the relations in the KB (correspond to the relation numeration) */
    protected int[] kbRelationArities;
    /** "Fact Coverage" and "τ" */
    protected double factCoverageThreshold, compRatioThreshold;
    /** The list of collected rules and the evaluation details */
    protected List<CollectedRuleInfo> collectedRuleInfos = new ArrayList<>();

    public static Path getRulesFilePath(String hintFilePath, String kbName) {
        return Paths.get(new File(hintFilePath).toPath().getParent().toString(), String.format("rules_%s.tsv", kbName));
    }

    /**
     * Create a Hinter object.
     *
     * @param kbPath         The path to the numerated KB
     * @param kbName         The name of the KB
     * @param hintFilePath   The path to the hint file
     */
    public Hinter(String kbPath, String kbName, String hintFilePath) {
        this.kbPath = kbPath;
        this.kbName = kbName;
        this.hintFilePath = hintFilePath;
        this.outputFilePath = getRulesFilePath(hintFilePath, kbName);
    }

    /**
     * Use the hints to instantiate rules in the target KB
     *
     */
    public void run() throws ExperimentException {
        try {
            /* Load KB */
            kb = new NumeratedKb(kbName, kbPath);
            kbRelationNums = new int[kb.totalRelations()];
            kbRelationArities = new int[kb.totalRelations()];
            int idx = 0;
            for (KbRelation relation: kb.getRelations()) {
                kbRelationNums[idx] = relation.getNumeration();
                kbRelationArities[idx] = relation.getArity();
                idx++;
            }

            /* Load hint file */
            /* Read "Fact Coverage" and "τ" */
            BufferedReader reader = new BufferedReader(new FileReader(hintFilePath));
            try {
                factCoverageThreshold = Double.parseDouble(reader.readLine());
                Rule.MIN_FACT_COVERAGE = factCoverageThreshold;
            } catch (Exception e) {
                throw new ExperimentException("Missing fact coverage setting", e);
            }
            try {
                compRatioThreshold = Double.parseDouble(reader.readLine());
            } catch (Exception e) {
                throw new ExperimentException("Missing compression ratio setting", e);
            }

            /* Read templates and restrictions */
            String line;
            List<Hint> hints = new ArrayList<>();
            while (null != (line = reader.readLine())) {
                hints.add(new Hint(line, kb.getNumerationMap()));
            }

            /* Instantiate templates */
            for (int i = 0; i < kbRelationNums.length; i++) {
                /* Create the initial rule */
                Set<Fingerprint> fingerprint_cache = new HashSet<>();
                Map<MultiSet<Integer>, Set<Fingerprint>> tabu_set = new HashMap<>();
                int head_functor = kbRelationNums[i];
                int head_arity = kbRelationArities[i];
                CachedRule rule = new CachedRule(head_functor, head_arity, fingerprint_cache, tabu_set, kb);

                /* Try each template */
                for (Hint hint: hints) {
                    if (head_arity != hint.functorArities[0]) {
                        continue;
                    }
                    int totalFunctors = hint.functorRestrictionCounterLink.length;
                    int[] template_functor_instantiation = ArrayOperation.initArrayWithValue(totalFunctors, UNDETERMINED);
                    int[] restriction_counters = new int[hint.restrictions.size()];

                    /* Set head functor */
                    template_functor_instantiation[0] = head_functor;

                    /* Set restrictions */
                    specializeByOperations(
                            rule.clone(), hint.operations, 0, template_functor_instantiation, hint.functorArities,
                            hint.functorRestrictionCounterLink, hint.restrictionCounterBounds, restriction_counters
                    );
                }
            }

            /* Dump the results */
            PrintWriter writer = new PrintWriter(outputFilePath.toFile());
            writer.printf("rule\t|r|\tE+\tE-\tFC\tτ\tδ\n");
            collectedRuleInfos.sort(Comparator.comparingDouble((CollectedRuleInfo e) -> e.score).reversed());
            for (CollectedRuleInfo rule_info: collectedRuleInfos) {
                writer.println(rule_info.rule);
            }
            writer.close();
        } catch (KbException | IOException | RuleParseException e) {
            throw new ExperimentException(e);
        }
    }

    /**
     * DFS search for specializing a rule according to a list of specialization operations. Functors in the templates
     * should be instantiated by real functors in the KB. Record all template instances that satisfies the requirements
     * of the hint file.
     *
     * @param rule                           The rule to be specialized
     * @param operations                     The list of specialization operations
     * @param oprStartIdx                    The start index of the operation list
     * @param templateFunctorInstantiation   The instantiation of the template functors
     * @param templateFunctorArities         The arities of the template functors
     * @param functorRestrictionCounterLinks Template functor to restriction counter index
     * @param restrictionCounterBounds       The bounds for the restriction counters
     * @param restrictionCounters            The restriction counters
     */
    protected void specializeByOperations(
            CachedRule rule, List<SpecOpr> operations, int oprStartIdx,
            int[] templateFunctorInstantiation, int[] templateFunctorArities,
            int[][] functorRestrictionCounterLinks, int[] restrictionCounterBounds, int[] restrictionCounters
    ) {
        for (int opr_idx = oprStartIdx; opr_idx < operations.size(); opr_idx++) {
            SpecOpr opr = operations.get(opr_idx);
            rule.updateCacheIndices();
            switch (opr.getSpecCase()) {
                case CASE1:
                case CASE3:
                case CASE5:
                    if (UpdateStatus.NORMAL != opr.specialize(rule)) {
                        return;
                    }
                    break;
                case CASE2:
                    SpecOprCase2 opr_case2 = (SpecOprCase2) opr;
                    if (UNDETERMINED == templateFunctorInstantiation[opr_case2.functor]) {
                        for (int i = 0; i < kbRelationNums.length; i++) {
                            /* Try every possible functor with the same arity of the template one */
                            if (templateFunctorArities[opr_case2.functor] != kbRelationArities[i]) {  // Check whether arity matches
                                continue;
                            }
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case2.functor]) {  // Add counters
                                restrictionCounters[counter_idx]++;
                            }
                            boolean valid = true;
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case2.functor]) {  // Check whether any counter is violated
                                if (restrictionCounterBounds[counter_idx] == restrictionCounters[counter_idx]) {
                                    valid = false;
                                    break;
                                }
                            }
                            if (valid) {
                                /* DFS to the next step */
                                templateFunctorInstantiation[opr_case2.functor] = kbRelationNums[i];
                                CachedRule specialized_rule = rule.clone();
                                if (UpdateStatus.NORMAL == specialized_rule.cvt1Uv2ExtLv(   // Can't use the method "specialize" of "SpecOpr",
                                        templateFunctorInstantiation[opr_case2.functor],    // because the functor in the objects denotes the
                                        templateFunctorArities[opr_case2.functor],          // template index instead of the real numeration of the functors
                                        opr_case2.argIdx, opr_case2.varId)
                                ) {
                                    specializeByOperations(
                                            specialized_rule, operations, opr_idx + 1,
                                            templateFunctorInstantiation, templateFunctorArities,
                                            functorRestrictionCounterLinks, restrictionCounterBounds, restrictionCounters
                                    );
                                }
                                templateFunctorInstantiation[opr_case2.functor] = UNDETERMINED;   // Restore the instantiation
                            }
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case2.functor]) {  // Restore the counters
                                restrictionCounterBounds[counter_idx]--;
                            }
                        }

                        /* If it goes here, all following operations are done in the recursive call above, just return */
                        return;
                    } else if (UpdateStatus.NORMAL == rule.cvt1Uv2ExtLv(
                            templateFunctorInstantiation[opr_case2.functor], templateFunctorArities[opr_case2.functor],
                            opr_case2.argIdx, opr_case2.varId
                    )) {
                        return;
                    }
                    break;
                case CASE4:
                    SpecOprCase4 opr_case4 = (SpecOprCase4) opr;
                    if (UNDETERMINED == templateFunctorInstantiation[opr_case4.functor]) {
                        for (int i = 0; i < kbRelationNums.length; i++) {
                            /* Try every possible functor with the same arity of the template one */
                            if (templateFunctorArities[opr_case4.functor] != kbRelationArities[i]) {  // Check whether arity matches
                                continue;
                            }
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case4.functor]) {  // Add counters
                                restrictionCounters[counter_idx]++;
                            }
                            boolean valid = true;
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case4.functor]) {  // Check whether any counter is violated
                                if (restrictionCounterBounds[counter_idx] == restrictionCounters[counter_idx]) {
                                    valid = false;
                                    break;
                                }
                            }
                            if (valid) {
                                /* DFS to the next step */
                                templateFunctorInstantiation[opr_case4.functor] = kbRelationNums[i];
                                CachedRule specialized_rule = rule.clone();
                                if (UpdateStatus.NORMAL == specialized_rule.cvt2Uvs2NewLv(  // Can't use the method "specialize" of "SpecOpr",
                                        templateFunctorInstantiation[opr_case4.functor],    // because the functor in the objects denotes the
                                        templateFunctorArities[opr_case4.functor],          // template index instead of the real numeration of the functors
                                        opr_case4.argIdx1, opr_case4.predIdx2, opr_case4.argIdx2)
                                ) {
                                    specializeByOperations(
                                            specialized_rule, operations, opr_idx + 1,
                                            templateFunctorInstantiation, templateFunctorArities,
                                            functorRestrictionCounterLinks, restrictionCounterBounds, restrictionCounters
                                    );
                                }
                                templateFunctorInstantiation[opr_case4.functor] = UNDETERMINED;   // Restore the instantiation
                            }
                            for (int counter_idx: functorRestrictionCounterLinks[opr_case4.functor]) {  // Restore the counters
                                restrictionCounterBounds[counter_idx]--;
                            }
                        }

                        /* If it goes here, all following operations are done in the recursive call above, just return */
                        return;
                    } else if (UpdateStatus.NORMAL == rule.cvt2Uvs2NewLv(
                            templateFunctorInstantiation[opr_case4.functor], templateFunctorArities[opr_case4.functor],
                            opr_case4.argIdx1, opr_case4.predIdx2, opr_case4.argIdx2
                    )) {
                        return;
                    }
                    break;
            }
        }

        /* Specialization finished, check rule quality */
        Eval eval = rule.getEval();
        if (compRatioThreshold <= eval.value(EvalMetric.CompressionRatio)) {
            String rule_info = String.format("%s\t%d\t%d\t%d\t%.2f\t%.2f\t%d",
                    rule.toDumpString(kb.getNumerationMap()), rule.length(), (int) eval.getPosEtls(), (int) eval.getNegEtls(),
                    eval.getPosEtls()/kb.getRelation(templateFunctorInstantiation[0]).totalRecords()*100,
                    eval.value(EvalMetric.CompressionRatio), (int) eval.value(EvalMetric.CompressionCapacity)
            );
            collectedRuleInfos.add(new CollectedRuleInfo(rule_info, eval.value(EvalMetric.CompressionRatio)));
        }
    }
}
