package sinc2.rule;

import sinc2.common.Argument;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.common.Predicate;
import sinc2.kb.NumerationMap;
import sinc2.kb.Record;
import sinc2.util.DisjointSet;
import sinc2.util.MultiSet;

import java.util.*;

/**
 * The class of the basic rule structure. The class defines the basic structure of a rule and the basic operations that
 * manipulates the structure.
 *
 * @since 1.0
 */
public abstract class Rule {
    /** The index of the head predicate */
    public static final int HEAD_PRED_IDX = 0;

    /** The beginning index of the body predicate */
    public static final int FIRST_BODY_PRED_IDX = HEAD_PRED_IDX + 1;

    /** The threshold of the coverage value for pruning */
    public static double MIN_FACT_COVERAGE = 0.0;

    /** The cache of all used fingerprints */
    protected final Set<Fingerprint> fingerprintCache;

    /** The fingerprint tabu set of all rules that are pruned due to insufficient coverage */
    protected final Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap;

    /** The structure of the rule */
    protected final List<Predicate> structure;

    /** The number of limited variables used in the rule */
    protected final List<Integer> limitedVarCnts;   // Limited vars use non-negative ids (the list indices)

    /** The fingerprint of the rule */
    protected Fingerprint fingerprint;

    /** The rule length */
    protected int length;

    /** The evaluation of the rule */
    protected Eval eval;

    /**
     * Parse a plain-text string into a rule structure. The allowed input can be defined by the following context-free
     * grammar (which is similar to Prolog):
     *
     * rule := predicate:-body
     * body := ε | predicate | predicate,body
     * predicate := pred_symbol(args)
     * args := ε | variable | constant | variable,args | constant,args
     *
     * A "variable" is defined by the following regular expression: [A-Z][a-zA-z0-9]*
     * A "pred_symbol" and a "constant" are defined by the following regex: [a-z][a-zA-z0-9]*
     *
     * @return The rule structure is represented by a list of ParsedPred because there is no mapping information for the
     * numerations of the names.
     */
    public static List<ParsedPred> parseStructure(String ruleStr) throws RuleParseException {
        List<ParsedPred> structure = new ArrayList<>();
        Map<String, Integer> variable_2_id_map = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        String functor = null;
        List<ParsedArg> arguments = new ArrayList<>();
        for (int char_idx = 0; char_idx < ruleStr.length(); char_idx++) {
            char c = ruleStr.charAt(char_idx);
            switch (c) {
                case '(':
                    /* Buffer as functor */
                    functor = builder.toString();
                    builder = new StringBuilder();
                    break;
                case ')':
                    /* Buffer as argument, finish a predicate */
                    arguments.add(parseArg(builder.toString(), variable_2_id_map));
                    ParsedPred predicate = new ParsedPred(functor, arguments.toArray(new ParsedArg[0]));
                    structure.add(predicate);
                    builder = new StringBuilder();
                    functor = null;
                    arguments = new ArrayList<>();
                    break;
                case ',':
                    /* In Predicate: Buffer as argument; Out of predicate: nothing */
                    if (null != functor) {
                        arguments.add(parseArg(builder.toString(), variable_2_id_map));
                        builder = new StringBuilder();
                    }
                    break;
                case ':':
                case '-':
                case ' ': case '\n': case '\t':
                    /* Nothing */
                    break;
                default:
                    /* Append buffer */
                    builder.append(c);
            }
        }

        /* Remove possible named UVs */
        int[] lv_cnts = new int[variable_2_id_map.size()];
        for (ParsedPred predicate: structure) {
            for (ParsedArg argument: predicate.args) {
                if (null != argument && null == argument.name) {
                    lv_cnts[argument.id]++;
                }
            }
        }
        int named_uv_cnt = 0;
        for (int vid = 0; vid < lv_cnts.length; vid++) {
            if (1 == lv_cnts[vid]) {
                /* Remove the UV argument */
                named_uv_cnt++;
                boolean found = false;
                for (int pred_idx = 0; pred_idx < structure.size() && !found; pred_idx++) {
                    ParsedPred predicate = structure.get(pred_idx);
                    for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                        ParsedArg argument = predicate.args[arg_idx];
                        if (null != argument && null == argument.name && argument.id == vid) {
                            found = true;
                            predicate.args[arg_idx] = null;
                            break;
                        }
                    }
                }
            }
        }
        for (int named_uv_id = 0; named_uv_id < lv_cnts.length - named_uv_cnt; named_uv_id++) {
            /* Use LV ID to replace used UV IDs */
            if (1 == lv_cnts[named_uv_id]) {
                for (int lv_id = lv_cnts.length - 1; lv_id > named_uv_id; lv_id--) {
                    if (1 < lv_cnts[lv_id]) {
                        lv_cnts[lv_id] = 0;
                        for (ParsedPred predicate: structure) {
                            for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                                ParsedArg argument = predicate.args[arg_idx];
                                if (null != argument && null == argument.name && argument.id == lv_id) {
                                    predicate.args[arg_idx] = ParsedArg.variable(named_uv_id);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        return structure;
    }

    /**
     * Parse a plain-text string into an argument.
     *
     * @return The argument is represented by an instance of ParsedArg because there is no mapping information for the
     * numerations of the names.
     */
    static ParsedArg parseArg(String str, Map<String, Integer> variable2IdMap) throws RuleParseException {
        final char first_char = str.charAt(0);
        switch (first_char) {
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K':
            case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V':
            case 'W': case 'X': case 'Y': case 'Z':
                /* Parse LV */
                return ParsedArg.variable(variable2IdMap.computeIfAbsent(str, k -> variable2IdMap.size()));
            case '?':
                /* Parse UV */
                return null;
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k':
            case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v':
            case 'w': case 'x': case 'y': case 'z':
                /* Parse Constant */
                return ParsedArg.constant(str);
            default:
                throw new RuleParseException(String.format("Character not allowed at the beginning of the argument: '%c'", first_char));
        }
    }

    /**
     * Parse the specialization routine that constructs the rule structure.
     *
     * @param structure Rule structure
     * @return An ordered specialization operation list
     */
    public static List<ParsedSpecOpr> parseConstruction(List<ParsedPred> structure) throws RuleParseException {
        List<ParsedPred> remaining_structure = new ArrayList<>(structure); // copy
        List<ParsedPred> constructed_structure = new ArrayList<>(remaining_structure.size());
        List<ParsedSpecOpr> operations = new ArrayList<>();

        /* Add head */
        constructed_structure.add(remaining_structure.get(0));
        remaining_structure.remove(0);
        addConstantSpecialization(operations, constructed_structure.get(0), 0);

        /* Specialize all variables and constants */
        /* Constants are specialized as early as possible */
        int constructed_length = -1;
        int lv_id = -1;
        while (constructed_length < operations.size()) {
            constructed_length = operations.size();
            for (int pred_idx = 0; pred_idx < constructed_structure.size(); pred_idx++) {
                ParsedPred predicate = constructed_structure.get(pred_idx);
                for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                    ParsedArg argument = predicate.args[arg_idx];
                    if (null != argument) {
                        /* Specialize the variable */
                        predicate.args[arg_idx] = null; // remove specialized arguments
                        int var_id = argument.id;   // As constants are added the moment a predicate is added to the structure, here "argument.name == null" will always be true
                        boolean found = false;
                        for (int pred_idx2 = pred_idx; pred_idx2 < constructed_structure.size() && !found; pred_idx2++) {
                            ParsedPred predicate2 = constructed_structure.get(pred_idx2);
                            for (int arg_idx2 = 0; arg_idx2 < predicate2.args.length; arg_idx2++) {
                                ParsedArg argument2 = predicate2.args[arg_idx2];
                                if (null != argument2 && null == argument2.name && var_id == argument2.id) {
                                    /* Find another occurrence in the constructed part of the rule */
                                    /* Case 3 */
                                    found = true;
                                    predicate2.args[arg_idx2] = null;
                                    operations.add(new ParsedSpecOprCase3(pred_idx, arg_idx, pred_idx2, arg_idx2));
                                    lv_id++;
                                    break;
                                }
                            }
                        }

                        if (!found) {
                            /* Find another occurrence in the un-constructed part of the rule */
                            Iterator<ParsedPred> it = remaining_structure.listIterator();
                            while (it.hasNext() && !found) {
                                ParsedPred predicate2 = it.next();
                                for (int arg_idx2 = 0; arg_idx2 < predicate2.args.length; arg_idx2++) {
                                    ParsedArg argument2 = predicate2.args[arg_idx2];
                                    if (null != argument2 && null == argument2.name && var_id == argument2.id) {
                                        /* Case 4 */
                                        found = true;
                                        predicate2.args[arg_idx2] = null;
                                        operations.add(new ParsedSpecOprCase4(
                                                predicate2.functor, predicate2.args.length, arg_idx2, pred_idx, arg_idx
                                        ));
                                        lv_id++;
                                        constructed_structure.add(predicate2);
                                        it.remove();

                                        /* Case 5 */
                                        addConstantSpecialization(operations, predicate2, constructed_structure.size() - 1);
                                        break;
                                    }
                                }
                            }
                        }

                        /* Construct the remaining occurrences of the variable */
                        for (int pred_idx2 = pred_idx; pred_idx2 < constructed_structure.size(); pred_idx2++) {
                            ParsedPred predicate2 = constructed_structure.get(pred_idx2);
                            for (int arg_idx2 = 0; arg_idx2 < predicate2.args.length; arg_idx2++) {
                                ParsedArg argument2 = predicate2.args[arg_idx2];
                                if (null != argument2 && null == argument2.name && var_id == argument2.id) {
                                    /* Find another occurrence in the constructed part of the rule */
                                    /* Case 1 */
                                    predicate2.args[arg_idx2] = null;
                                    operations.add(new ParsedSpecOprCase1(pred_idx2, arg_idx2, lv_id));
                                    break;
                                }
                            }
                        }
                        Iterator<ParsedPred> it = remaining_structure.listIterator();
                        while (it.hasNext()) {
                            ParsedPred predicate2 = it.next();
                            for (int arg_idx2 = 0; arg_idx2 < predicate2.args.length; arg_idx2++) {
                                ParsedArg argument2 = predicate2.args[arg_idx2];
                                if (null != argument2 && null == argument2.name && var_id == argument2.id) {
                                    /* Case 2 */
                                    predicate2.args[arg_idx2] = null;
                                    operations.add(new ParsedSpecOprCase2(
                                            predicate2.functor, predicate2.args.length, arg_idx2, lv_id
                                    ));
                                    constructed_structure.add(predicate2);
                                    it.remove();

                                    /* Case 5 */
                                    addConstantSpecialization(operations, predicate2, constructed_structure.size() - 1);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!remaining_structure.isEmpty()) {
            throw new RuleParseException("Independent fragment existed in the rule: " + remaining_structure);
        }

        return operations;
    }

    /**
     * Add constant specializations in a single predicate to the operation list
     *
     * @param operations Specialization operation list
     * @param predicate The predicate
     * @param predIdx The index of the predicate
     */
    static protected void addConstantSpecialization(List<ParsedSpecOpr> operations, ParsedPred predicate, int predIdx) {
        for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
            ParsedArg argument = predicate.args[arg_idx];
            if (null != argument && null != argument.name) {
                /* Case 5 */
                operations.add(new ParsedSpecOprCase5(predIdx, arg_idx, argument.name));
                predicate.args[arg_idx] = null;
            }
        }
    }

    /**
     * Initialize the most general rule of a certain target head relation.
     *
     * @param headFunctor The functor of the head predicate, i.e., the target relation.
     * @param arity The arity of the functor
     * @param fingerprintCache The cache of the used fingerprints
     * @param category2TabuSetMap The tabu set of pruned fingerprints
     */
    public Rule(
            int headFunctor, int arity, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap
    ) {
        this.fingerprintCache = fingerprintCache;
        this.category2TabuSetMap = category2TabuSetMap;
        structure = new ArrayList<>();
        limitedVarCnts = new ArrayList<>();
        length = 0;
        eval = null;

        structure.add(new Predicate(headFunctor, arity));
        fingerprint = new Fingerprint(structure);
        this.fingerprintCache.add(fingerprint);
    }

    /**
     * Construct a rule with a certain structure.
     *
     * @param structure The structure of the rule.
     * @param fingerprintCache The cache of the fingerprint
     */
    public Rule(
            List<Predicate> structure, Set<Fingerprint> fingerprintCache,
            Map<MultiSet<Integer>, Set<Fingerprint>> category2TabuSetMap
    ) {
        this.fingerprintCache = fingerprintCache;
        this.category2TabuSetMap = category2TabuSetMap;
        this.structure = new ArrayList<>(structure.size());
        limitedVarCnts = new ArrayList<>();
        eval = null;

        /* Calculate the length and find the limited variables */
        length = 0;
        Map<Integer, Integer> old_vid_2_new_vid_map = new HashMap<>();
        for (Predicate p: structure) {
            this.structure.add(new Predicate(p));
            for (int argument: p.args) {
                if (Argument.isVariable(argument)) {
                    int var_id = Argument.decode(argument);
                    old_vid_2_new_vid_map.computeIfAbsent(var_id, k -> {
                        limitedVarCnts.add(0);
                        return old_vid_2_new_vid_map.size();
                    });
                }
                this.length += Argument.isEmpty(argument) ? 0 : 1;
            }
        }
        this.length -= old_vid_2_new_vid_map.size();

        /* Normalize the variables */
        for (Predicate p: this.structure) {
            for (int arg_idx = 0; arg_idx < p.arity(); arg_idx++) {
                int argument = p.args[arg_idx];
                if (Argument.isVariable(argument)) {
                    int new_vid = old_vid_2_new_vid_map.get(Argument.decode(argument));
                    p.args[arg_idx] = Argument.variable(new_vid);
                    limitedVarCnts.set(new_vid, limitedVarCnts.get(new_vid) + 1);
                }
            }
        }

        this.fingerprint = new Fingerprint(this.structure);
        this.fingerprintCache.add(fingerprint);
    }

    /**
     * A copy constructor.
     */
    public Rule(Rule another) {
        this.fingerprintCache = another.fingerprintCache;
        this.category2TabuSetMap = another.category2TabuSetMap;
        this.structure = new ArrayList<>(another.structure.size());
        for (Predicate predicate: another.structure) {
            this.structure.add(new Predicate(predicate));
        }
        this.limitedVarCnts = new ArrayList<>(another.limitedVarCnts);
        this.length = another.length;
        this.eval = another.eval;
        this.fingerprint = another.fingerprint;
    }

    /**
     * A clone method that correctly uses the copy constructor of any sub-class. The copy constructor may not be useful,
     * as a super class copy constructor generates only the super class object even on the sub-class object.
     *
     * @return A copy of the rule instance
     */
    public abstract Rule clone();

    /**
     * Check if the rule structure is invalid. The structure is invalid if:
     *   1. It contains duplicated predicates Todo: Partial duplication with the head should not be defined as invalid.
     *   2. It contains independent Fragment
     */
    protected boolean isInvalid() {
        /* Check independent fragment (may happen when looking for generalizations) */
        /* Use the disjoint set: join limited variables if they are in the same predicate */
        /* Assumption: no body predicate contains only empty or constant argument */
        DisjointSet disjoint_set = new DisjointSet(usedLimitedVars());

        /* Check duplicated predicates */
        /* 1. Check by set */
        /* 2. A more aggressive check to prevent duplication with the head: check for body predicate with same argument
           with the head */
        Predicate head_pred = structure.get(HEAD_PRED_IDX);
        {
            /* Put the LVs in the head to the disjoint set */
            List<Integer> lv_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                int argument = head_pred.args[arg_idx];
                if (Argument.isVariable(argument)) {
                    lv_ids.add(Argument.decode(argument));
                }
            }
            if (lv_ids.isEmpty()) {
                if (structure.size() >= 2) {
                    /* The body is an independent fragment if the head contains no LV while body is not empty */
                    return true;
                }
            } else {
                /* Must check here because there may be no LV in the head */
                int first_id = lv_ids.get(0);
                for (int i = 1; i < lv_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, lv_ids.get(i));
                }
            }
        }

        Set<Predicate> predicate_set = new HashSet<>();
        for (int pred_idx = FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            Predicate body_pred = structure.get(pred_idx);
            if (head_pred.functor == body_pred.functor) {
                for (int arg_idx = 0; arg_idx < head_pred.arity(); arg_idx++) {
                    int head_arg = head_pred.args[arg_idx];
                    int body_arg = body_pred.args[arg_idx];
                    if (Argument.isNonEmpty(head_arg) && head_arg == body_arg) {
                        return true;
                    }
                }
            }

            boolean args_complete = true;
            List<Integer> lv_ids = new ArrayList<>();
            for (int arg_idx = 0; arg_idx < body_pred.arity(); arg_idx++) {
                int argument = body_pred.args[arg_idx];
                if (Argument.isEmpty(argument)) {
                    args_complete = false;
                } else if (Argument.isVariable(argument)) {
                    lv_ids.add(Argument.decode(argument));
                }
            }

            if (args_complete) {
                if (!predicate_set.add(body_pred)) {
                    return true;
                }
            }

            /* 在同一个Predicate中出现的Bounded Var合并到一个集合中 */
            /* Join the LVs in the same predicate */
            if (lv_ids.isEmpty()) {
                /* If no LV in body predicate, the predicate is certainly part of the fragment */
                return true;
            } else {
                int first_id = lv_ids.get(0);
                for (int i = 1; i < lv_ids.size(); i++) {
                    disjoint_set.unionSets(first_id, lv_ids.get(i));
                }
            }
        }

        /* Check for independent fragment */
        return 2 <= disjoint_set.totalSets();
    }

    /**
     * Check if the rule structure has already been verified and added to the cache. If not, add the rule fingerprint to
     * the cache.
     */
    protected boolean cacheHit() {
        return !fingerprintCache.add(fingerprint);
    }

    /**
     * Check if the rule structure should be pruned by the tabu set.
     */
    protected boolean tabuHit() {
        for (int subset_size = 0; subset_size < structure.size(); subset_size++) {
            for (MultiSet<Integer> category_subset : categorySubsets(subset_size)) {
                final Set<Fingerprint> tabu_set = category2TabuSetMap.get(category_subset);
                if (null == tabu_set) continue;
                for (Fingerprint rfp : tabu_set) {
                    if (rfp.generalizationOf(this.fingerprint)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Enumerate the subcategories of the rule of a certain size.
     *
     * @param subsetSize The size of the subcategories
     * @return A set of subcategories
     */
    protected Set<MultiSet<Integer>> categorySubsets(int subsetSize) {
        final Set<MultiSet<Integer>> subsets = new HashSet<>();
        if (0 == subsetSize) {
            subsets.add(new MultiSet<>());
        } else {
            templateSubsetsHandler(subsets, new Integer[subsetSize], subsetSize - 1, FIRST_BODY_PRED_IDX);
        }
        return subsets;
    }

    /**
     * Handler for enumerating the subcategories.
     */
    protected void templateSubsetsHandler(Set<MultiSet<Integer>> subsets, Integer[] template, int depth, int startIdx) {
        if (0 < depth) {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                templateSubsetsHandler(subsets, template, depth-1, pred_idx+1);
            }
        } else {
            for (int pred_idx = startIdx; pred_idx < structure.size(); pred_idx++) {
                template[depth] = structure.get(pred_idx).functor;
                subsets.add(new MultiSet<>(template));
            }
        }
    }

    /**
     * Calculate the evaluation of the rule.
     */
    protected abstract Eval calculateEval();

    /**
     * Check if the coverage of the rule is below the threshold. If so, add the fingerprint to the tabu set.
     */
    protected boolean insufficientCoverage() {
        if (MIN_FACT_COVERAGE >= recordCoverage()) {
            add2TabuSet();
            return true;
        }
        return false;
    }

    /**
     * Calculate the record coverage of the rule. Todo: reuse positive entailments in evaluation?
     */
    protected abstract double recordCoverage();

    /**
     * Add this rule to the tabu set
     */
    protected void add2TabuSet() {
        final MultiSet<Integer> functor_mset = new MultiSet<>();
        for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < structure.size(); pred_idx++) {
            functor_mset.add(structure.get(pred_idx).functor);
        }
        final Set<Fingerprint> tabu_set = category2TabuSetMap.computeIfAbsent(
                functor_mset, k -> new HashSet<>()
        );
        tabu_set.add(fingerprint);
    }

    /**
     * Specialization Case 1: Convert a UV to an existing LV.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @param varId The id of the LV
     * @return The update status
     */
    public UpdateStatus cvt1Uv2ExtLv(final int predIdx, final int argIdx, final int varId) {
        cvt1Uv2ExtLvUpdStrc(predIdx, argIdx, varId);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ExtLvHandlerPreCvg(predIdx, argIdx, varId);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ExtLvHandlerPostCvg(predIdx, argIdx, varId);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 1.
     */
    protected void cvt1Uv2ExtLvUpdStrc(final int predIdx, final int argIdx, final int varId) {
        final Predicate target_predicate = structure.get(predIdx);
        target_predicate.args[argIdx] = Argument.variable(varId);
        limitedVarCnts.set(varId, limitedVarCnts.get(varId)+1);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 1, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(final int predIdx, final int argIdx, final int varId);

    /**
     * Handler for case 1, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(final int predIdx, final int argIdx, final int varId);

    /**
     * Specialization Case 2: Add a new predicate and convert a UV in the new predicate to an existing LV.
     *
     * @param functor The functor of the new predicate
     * @param arity The arity of the new predicate
     * @param argIdx The index of the argument
     * @param varId The id of the LV
     * @return The update status
     */
    public UpdateStatus cvt1Uv2ExtLv(final int functor, final int arity, final int argIdx, final int varId) {
        cvt1Uv2ExtLvUpdStrc(functor, arity, argIdx, varId);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ExtLvHandlerPreCvg(structure.get(structure.size()-1), argIdx, varId);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ExtLvHandlerPostCvg(structure.get(structure.size()-1), argIdx, varId);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 2.
     */
    protected void cvt1Uv2ExtLvUpdStrc(final int functor, final int arity, final int argIdx, final int varId) {
        final Predicate target_predicate = new Predicate(functor, arity);
        structure.add(target_predicate);
        target_predicate.args[argIdx] = Argument.variable(varId);
        limitedVarCnts.set(varId, limitedVarCnts.get(varId)+1);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 2, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPreCvg(
            final Predicate newPredicate, final int argIdx, final int varId
    );

    /**
     * Handler for case 2, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt1Uv2ExtLvHandlerPostCvg(
            final Predicate newPredicate, final int argIdx, final int varId
    );

    /**
     * Specialization Case 3: Convert 2 UVs in the rule to a new LV.
     *
     * @param predIdx1 The index of the first predicate
     * @param argIdx1 The argument index in the first predicate
     * @param predIdx2 The index of the second predicate
     * @param argIdx2 The argument index in the second predicate
     * @return The update status
     */
    public UpdateStatus cvt2Uvs2NewLv(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        cvt2Uvs2NewLvUpdStrc(predIdx1, argIdx1, predIdx2, argIdx2);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt2Uvs2NewLvHandlerPreCvg(predIdx1, argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt2Uvs2NewLvHandlerPostCvg(predIdx1, argIdx1, predIdx2, argIdx2);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 3.
     */
    protected void cvt2Uvs2NewLvUpdStrc(final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2) {
        final Predicate target_predicate1 = structure.get(predIdx1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final int new_var = Argument.variable(limitedVarCnts.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        limitedVarCnts.add(2);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 3, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Handler for case 3, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(
            final int predIdx1, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Specialization Case 4: Add a new predicate to the rule and convert 2 UVs in the rule to a new LV. Note: Exactly one of the
     * selected arguments are from the newly added predicate. 
     *
     * @param functor The functor of the new predicate
     * @param arity The arity of the new predicate
     * @param argIdx1 The argument index in the first predicate
     * @param predIdx2 The index of the second predicate
     * @param argIdx2 The argument index in the second predicate
     * @return The update status
     */
    public UpdateStatus cvt2Uvs2NewLv(
            final int functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        cvt2Uvs2NewLvUpdStrc(functor, arity, argIdx1, predIdx2, argIdx2);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt2Uvs2NewLvHandlerPreCvg(structure.get(structure.size()-1), argIdx1, predIdx2, argIdx2);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt2Uvs2NewLvHandlerPostCvg(structure.get(structure.size()-1), argIdx1, predIdx2, argIdx2);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 4.
     */
    protected void cvt2Uvs2NewLvUpdStrc(
            final int functor, final int arity, final int argIdx1, final int predIdx2, final int argIdx2
    ) {
        final Predicate target_predicate1 = new Predicate(functor, arity);
        structure.add(target_predicate1);
        final Predicate target_predicate2 = structure.get(predIdx2);
        final int new_var = Argument.variable(limitedVarCnts.size());
        target_predicate1.args[argIdx1] = new_var;
        target_predicate2.args[argIdx2] = new_var;
        limitedVarCnts.add(2);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 4, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPreCvg(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Handler for case 4, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     *
     * @param newPredicate The newly created predicate in the updated structure
     */
    protected abstract UpdateStatus cvt2Uvs2NewLvHandlerPostCvg(
            final Predicate newPredicate, final int argIdx1, final int predIdx2, final int argIdx2
    );

    /**
     * Specialization Case 5: Convert a UV to a constant.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @param constant The numeration of the constant symbol
     * @return The update status
     */
    public UpdateStatus cvt1Uv2Const(final int predIdx, final int argIdx, final int constant) {
        cvt1Uv2ConstUpdStrc(predIdx, argIdx, constant);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = cvt1Uv2ConstHandlerPreCvg(predIdx, argIdx, constant);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = cvt1Uv2ConstHandlerPostCvg(predIdx, argIdx, constant);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for case 5.
     */
    protected void cvt1Uv2ConstUpdStrc(final int predIdx, final int argIdx, final int constant) {
        final Predicate predicate = structure.get(predIdx);
        predicate.args[argIdx] = Argument.constant(constant);
        length++;
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for case 5, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ConstHandlerPreCvg(final int predIdx, final int argIdx, final int constant);

    /**
     * Handler for case 5, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus cvt1Uv2ConstHandlerPostCvg(final int predIdx, final int argIdx, final int constant);

    /**
     * Generalization: Remove an assignment to the argument.
     *
     * @param predIdx The index of the predicate
     * @param argIdx The index of the argument
     * @return The update status
     */
    public UpdateStatus rmAssignedArg(final int predIdx, final int argIdx) {
        rmAssignedArgUpdStrc(predIdx, argIdx);

        if (cacheHit()) {
            return UpdateStatus.DUPLICATED;
        }

        if (isInvalid()) {
            return UpdateStatus.INVALID;
        }

        if (tabuHit()) {
            return UpdateStatus.TABU_PRUNED;
        }

        /* Invoke handler and check coverage */
        UpdateStatus status = rmAssignedArgHandlerPreCvg(predIdx, argIdx);
        if (UpdateStatus.NORMAL == status) {
            if (insufficientCoverage()) {
                return UpdateStatus.INSUFFICIENT_COVERAGE;
            }
            status = rmAssignedArgHandlerPostCvg(predIdx, argIdx);
            if (UpdateStatus.NORMAL == status) {
                this.eval = calculateEval();
            }
        }
        return status;
    }

    /**
     * Update rule structure for generalization.
     */
    protected void rmAssignedArgUpdStrc(final int predIdx, final int argIdx) {
        final Predicate predicate = structure.get(predIdx);
        final int removed_argument = predicate.args[argIdx];
        predicate.args[argIdx] = Argument.EMPTY_VALUE;

        /* Rearrange the var ids if the removed argument is an LV */
        if (Argument.isVariable(removed_argument)) {
            int removed_var_id = Argument.decode(removed_argument);
            final Integer var_uses_cnt = limitedVarCnts.get(removed_var_id);
            if (2 >= var_uses_cnt) {
                /* The LV should be removed */
                /* Change the id of the latest LV to the removed one */
                /* Note that the removed one may also be the latest LV */
                int latest_var_id = limitedVarCnts.size() - 1;
                limitedVarCnts.set(removed_var_id, limitedVarCnts.get(latest_var_id));
                limitedVarCnts.remove(latest_var_id);

                /* Clear the argument as well as the UV due to the remove (if applicable) */
                boolean found = false;
                for (Predicate another_predicate : structure) {
                    for (int i = 0; i < another_predicate.arity(); i++) {
                        if (removed_argument == another_predicate.args[i]) {
                            another_predicate.args[i] = Argument.EMPTY_VALUE;
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }

                int latest_var = Argument.variable(latest_var_id);
                if (removed_argument != latest_var) {
                    /* Rewrite the vars to the removed one */
                    for (Predicate another_predicate : structure) {
                        for (int i = 0; i < another_predicate.arity(); i++) {
                            another_predicate.args[i] = (latest_var == another_predicate.args[i]) ?
                                    removed_argument : another_predicate.args[i];
                        }
                    }
                }
            } else {
                /* Remove the occurrence only */
                limitedVarCnts.set(removed_var_id, var_uses_cnt - 1);
            }
        }
        length--;

        /* The removal may result in a predicate where all arguments are empty, remove the predicate if it is not the head */
        Iterator<Predicate> itr = structure.iterator();
        itr.next();   // Skip the head
        while (itr.hasNext()) {
            Predicate body_pred = itr.next();
            boolean is_empty_pred = true;
            for (int argument: body_pred.args) {
                if (Argument.isNonEmpty(argument)) {
                    is_empty_pred = false;
                    break;
                }
            }
            if (is_empty_pred) {
                itr.remove();
            }
        }
        fingerprint = new Fingerprint(structure);
    }

    /**
     * Handler for generalization, before the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus rmAssignedArgHandlerPreCvg(final int predIdx, final int argIdx);

    /**
     * Handler for generalization, after the coverage check. This function is for the implementation of more actions during
     * the rule update in the sub-classes.
     */
    protected abstract UpdateStatus rmAssignedArgHandlerPostCvg(final int predIdx, final int argIdx);

    /**
     * Calculate the evidence of positively entailed facts in the KB. Each piece of evidence is an ordered array of
     * predicates, where the head is the first.
     *
     * @return The evidence generated by the rule.
     */
    public abstract EvidenceBatch getEvidenceAndMarkEntailment();

    /**
     * Calculate the counterexamples generated by the rule.
     *
     * @return The set of counterexamples.
     */
    public abstract Set<Record> getCounterexamples();

    public Predicate getPredicate(int idx) {
        return structure.get(idx);
    }

    public Predicate getHead() {
        return structure.get(HEAD_PRED_IDX);
    }

    public int length() {
        return length;
    }

    public int usedLimitedVars() {
        return limitedVarCnts.size();
    }

    public int predicates() {
        return structure.size();
    }

    public Eval getEval() {
        return eval;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public String toString(NumerationMap map) {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString(map)).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString(map));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString(map));
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(eval).append(')');
        builder.append(structure.get(0).toString()).append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString());
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString());
            }
        }
        return builder.toString();
    }

    public String toDumpString(NumerationMap map) {
        StringBuilder builder = new StringBuilder(structure.get(0).toString(map));
        builder.append(":-");
        if (1 < structure.size()) {
            builder.append(structure.get(1).toString(map));
            for (int i = 2; i < structure.size(); i++) {
                builder.append(',');
                builder.append(structure.get(i).toString(map));
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return this.fingerprint.equals(that.fingerprint);
    }

    @Override
    public int hashCode() {
        return fingerprint.hashCode();
    }
}
