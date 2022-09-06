package sinc2.exp;

import sinc2.common.*;
import sinc2.kb.NumerationMap;
import sinc2.rule.*;
import sinc2.util.ArrayOperation;

import java.util.*;

/**
 * The class for a single line in the hint file
 *
 * @since 2.0
 */
public class Hint {
    /** The template rule in the hint. Functors in the template are numbered from 0 to n-1, where n is the number of
     *  different functors in the rule. */
    public final List<Predicate> template = new ArrayList<>();
    /** The restriction tuples in the hint */
    public final List<int[]> restrictions = new ArrayList<>();
    /** The ordered operations that leads to the template */
    public final List<SpecOpr> operations = new ArrayList<>();
    /** The arities of the functors in the template */
    public final int[] functorArities;
    /** Functor number (the index of the array) to the indices of the restriction tuple (implemented as a counter) */
    public final int[][] functorRestrictionCounterLink;
    /** Counters of the restriction tuples */
    public final int[] restrictionCounterBounds;

    /**
     * Parse a line in the hint file to the "Hint" structure
     *
     * @param line The line that contains the template and restrictions
     * @throws RuleParseException Thrown when parse failed
     */
    public Hint(String line, NumerationMap numMap) throws RuleParseException {
        String[] components = line.split(";");
        Map<String, Integer> functor_2_int_map = new HashMap<>();

        /* Parse template */
        List<ParsedPred> parsed_rule = Rule.parseStructure(components[0]);
        List<Integer> functor_arities = new ArrayList<>();
        for (ParsedPred parsed_pred : parsed_rule) {
            int functor = functor_2_int_map.computeIfAbsent(parsed_pred.functor, k -> {
                functor_arities.add(parsed_pred.args.length);
                return functor_2_int_map.size();
            });
            int[] args = new int[parsed_pred.args.length];
            for (int i = 0; i < args.length; i++) {
                ParsedArg parsed_arg = parsed_pred.args[i];
                args[i] = (null == parsed_arg.name) ? Argument.variable(parsed_arg.id) :
                        Argument.constant(numMap.name2Num(parsed_arg.name));
            }
            template.add(new Predicate(functor, args));
        }
        functorArities = ArrayOperation.toArray(functor_arities);

        /* Parse Restrictions */
        StringBuilder builder = null;
        Set<Integer> functor_set = new HashSet<>();
        char[] chars = components[1].toCharArray();
        int char_idx = 0;
        try {
            for (; char_idx < chars.length; char_idx++) {
                switch (chars[char_idx]) {
                    case '[':
                        break;
                    case '(':
                        /* Begin a mutual set of functors */
                        functor_set = new HashSet<>();
                        builder = new StringBuilder();
                        break;
                    case ')':
                        /* Complete a mutual set of functors */
                        functor_set.add(functor_2_int_map.get(builder.toString()));
                        builder = null;
                        restrictions.add(ArrayOperation.toArray(functor_set));
                        functor_set.clear();
                        break;
                    case ',':
                        if (null != builder) {
                            /* Separator between functors */
                            functor_set.add(functor_2_int_map.get(builder.toString()));
                            builder = new StringBuilder();
                        }   // Otherwise, separator between tuples, do nothing
                        break;
                    case ']':
                        char_idx = chars.length;
                        break;
                    default:
                        builder.append(chars[char_idx]);
                }
            }
        } catch (Exception e) {
            throw new RuleParseException(String.format("Restriction Parse Failed At: %d", char_idx), e);
        }
        List<Integer>[] functor_restriction_counter_link = new List[functor_2_int_map.size()];
        for (int i = 0; i < functor_restriction_counter_link.length; i++) {
            functor_restriction_counter_link[i] = new ArrayList<>();
        }
        restrictionCounterBounds = new int[restrictions.size()];
        for (int tuple_idx = 0; tuple_idx < restrictions.size(); tuple_idx++) {
            restrictionCounterBounds[tuple_idx] = restrictions.get(tuple_idx).length;
            for (int functor: restrictions.get(tuple_idx)) {
                functor_restriction_counter_link[functor].add(tuple_idx);
            }
        }
        functorRestrictionCounterLink = new int[functor_restriction_counter_link.length][];
        for (int i = 0; i < functorRestrictionCounterLink.length; i++) {
            functorRestrictionCounterLink[i] = ArrayOperation.toArray(functor_restriction_counter_link[i]);
        }

        /* Parse specialization operations */
        List<ParsedSpecOpr> parsed_operations = Rule.parseConstruction(parsed_rule);
        for (ParsedSpecOpr parsed_operation: parsed_operations) {
            switch (parsed_operation.getSpecCase()) {
                case CASE1:
                    ParsedSpecOprCase1 opr_case1 = (ParsedSpecOprCase1) parsed_operation;
                    operations.add(new SpecOprCase1(opr_case1.predIdx, opr_case1.argIdx, opr_case1.varId));
                    break;
                case CASE2:
                    ParsedSpecOprCase2 opr_case2 = (ParsedSpecOprCase2) parsed_operation;
                    operations.add(new SpecOprCase2(functor_2_int_map.get(opr_case2.functor), opr_case2.arity, opr_case2.argIdx, opr_case2.varId));
                    break;
                case CASE3:
                    ParsedSpecOprCase3 opr_case3 = (ParsedSpecOprCase3) parsed_operation;
                    operations.add(new SpecOprCase3(opr_case3.predIdx1, opr_case3.argIdx1, opr_case3.predIdx2, opr_case3.argIdx2));
                    break;
                case CASE4:
                    ParsedSpecOprCase4 opr_case4 = (ParsedSpecOprCase4) parsed_operation;
                    operations.add(new SpecOprCase4(functor_2_int_map.get(opr_case4.functor), opr_case4.arity, opr_case4.argIdx1, opr_case4.predIdx2, opr_case4.argIdx2));
                    break;
                case CASE5:
                    ParsedSpecOprCase5 opr_case5 = (ParsedSpecOprCase5) parsed_operation;
                    operations.add(new SpecOprCase5(opr_case5.predIdx, opr_case5.argIdx, numMap.name2Num(opr_case5.constant)));
                    break;
            }
        }
    }

}
