package sinc.exp;

import sinc.common.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class CompareDupDetectionByBruteForce {

    public static final int SAMPLE_NUM = 10000;

    static class RulePair{
        final List<Predicate> rule1, rule2;

        public RulePair(List<Predicate> rule1, List<Predicate> rule2) {
            this.rule1 = rule1;
            this.rule2 = rule2;
        }
    }

    public static void main(String[] args) throws Exception {
        if (1 != args.length) {
            System.out.println("Usage: <Compare File Path>");
        }
        final String COMP_FILE_PATH = args[0];
        BufferedReader reader = new BufferedReader(new FileReader(COMP_FILE_PATH));
        List<RulePair> rule_pairs = new ArrayList<>();
        for (int i = 0; i < SAMPLE_NUM; i++) {
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            if (null == line1 || null == line2) {
                break;
            }
            rule_pairs.add(new RulePair(parseRule(line1), parseRule(line2)));
        }

        System.out.println("--- Compare Basic ---");
        compareBasic(rule_pairs);

        System.out.println("--- Compare Cache ---");
        compareProgress(rule_pairs);
    }

    static void compareBasic(List<RulePair> rulePairs) {
        long fingerprint_time_cost_nano = 0;
        long brute_force_time_cost_nano = 0;
        int false_positive_cnt = 0;
        int positive_cnt = 0;
        for (RulePair rp: rulePairs) {
            /* Test Brute Force */
            long time_start = System.nanoTime();
            boolean match = matchRules(rp.rule1, rp.rule2);
            long time_done = System.nanoTime();
            brute_force_time_cost_nano += time_done - time_start;
            if (!match) {
                false_positive_cnt++;
            }

            /* Test Fingerprint */
            RuleFingerPrint fp1 = new RuleFingerPrint(rp.rule1);
            RuleFingerPrint fp2 = new RuleFingerPrint(rp.rule2);
            time_start = System.nanoTime();
            match = fp1.equals(fp2);
            time_done = System.nanoTime();
            if (match) {
                positive_cnt++;
            }
            fingerprint_time_cost_nano += time_done - time_start;
        }

        System.out.printf("Brute Force Time Cost (ms): %.2f\n", brute_force_time_cost_nano / 1000000.0);
        System.out.printf("Fingerprint Time Cost (ms): %.2f\n", fingerprint_time_cost_nano / 1000000.0);
        System.out.printf("Speed-up: %.2f\n", brute_force_time_cost_nano * 1.0 / fingerprint_time_cost_nano);
        System.out.printf("False Positive: %d/%d\n", false_positive_cnt, rulePairs.size());
        System.out.printf("Positive: %d/%d\n", positive_cnt, rulePairs.size());
        System.out.printf("FP Rate: %.2f\n", false_positive_cnt * 100.0 / rulePairs.size());
    }

    static void compareProgress(List<RulePair> rulePairs) {
        Set<RuleFingerPrint> fp_cache = new HashSet<>();
        Set<List<Predicate>> bf_cache = new HashSet<>();
        long fp_cache_time_nano = 0;
        long bf_cache_time_nano = 0;
        int false_positive_cnt = 0;
        int positive_cnt = 0;
        for (RulePair rp: rulePairs) {
            fp_cache.add(new RuleFingerPrint(rp.rule1));
            bf_cache.add(rp.rule1);

            /* Test Fingerprint */
            RuleFingerPrint fp2 = new RuleFingerPrint(rp.rule2);
            long time_start = System.nanoTime();
            boolean match = !fp_cache.add(fp2);
            long time_done = System.nanoTime();
            if (match) {
                positive_cnt++;
            }
            fp_cache_time_nano += time_done - time_start;

            /* Test Brute Force */
            time_start = System.nanoTime();
            match = false;
            for (List<Predicate> r: bf_cache) {
                if (matchRules(r, rp.rule2)) {
                    match = true;
                    break;
                }
            }
            time_done = System.nanoTime();
            if (!match) {
                false_positive_cnt++;
            }
            bf_cache_time_nano += time_done - time_start;
        }

        System.out.printf("Brute Force Time Cost (ms): %.2f\n", bf_cache_time_nano / 1000000.0);
        System.out.printf("Fingerprint Time Cost (ms): %.2f\n", fp_cache_time_nano / 1000000.0);
        System.out.printf("Speed-up: %.2f\n", bf_cache_time_nano * 1.0 / fp_cache_time_nano);
        System.out.printf("False Positive: %d/%d\n", false_positive_cnt, rulePairs.size());
        System.out.printf("Positive: %d/%d\n", positive_cnt, rulePairs.size());
        System.out.printf("FP Rate: %.2f\n", false_positive_cnt * 100.0 / rulePairs.size());
    }

    static List<Predicate> parseRule(String str) {
        List<Predicate> rule = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        String functor = null;
        List<Argument> arguments = new ArrayList<>();
        for (int char_idx = 0; char_idx < str.length(); char_idx++) {
            char c = str.charAt(char_idx);
            switch (c) {
                case '(':
                    // Buffer as functor
                    functor = builder.toString();
                    builder = new StringBuilder();
                    break;
                case ')':
                    // Buffer as argument, finish a predicate
                    arguments.add(parseArg(builder.toString()));
                    Predicate predicate = new Predicate(functor, arguments.size());
                    for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                        predicate.args[arg_idx] = arguments.get(arg_idx);
                    }
                    rule.add(predicate);
                    builder = new StringBuilder();
                    functor = null;
                    arguments = new ArrayList<>();
                    break;
                case ',':
                    // In Predicate: Buffer as argument; Out of predicate: nothing
                    if (null != functor) {
                        arguments.add(parseArg(builder.toString()));
                        builder = new StringBuilder();
                    }
                    break;
                case ':':
                case '-':
                    // Nothing
                    break;
                default:
                    // Append buffer
                    builder.append(c);
            }
        }
        return rule;
    }

    static Argument parseArg(String str) {
        switch (str.charAt(0)) {
            case 'X':
                /* Parse LV */
                return new Variable(Integer.parseInt(str.substring(1)));
            case '?':
                /* Parse UV */
                return null;
            default:
                /* Parse Constant */
                return new Constant(Rule.CONSTANT_ARG_ID, str);
        }
    }

    static boolean matchRules(List<Predicate> rule1, List<Predicate> rule2) {
        /* Collect rule1 LVs */
        Set<Integer> rule1_vars = new HashSet<>();
        for (Predicate predicate: rule1) {
            for (Argument argument: predicate.args) {
                if (null != argument && argument.isVar) {
                    rule1_vars.add(argument.id);
                }
            }
        }

        /* Collect and normalize rule2 LVs */
        Set<Integer> rule2_vars = new HashSet<>();
        for (Predicate predicate: rule2) {
            for (Argument argument: predicate.args) {
                if (null != argument && argument.isVar) {
                    rule2_vars.add(argument.id);
                }
            }
        }
        if (rule1_vars.size() != rule2_vars.size()) {
            return false;
        }

        /* Permute LVs in rule2 and check equality */
        Integer[] rule1_var_arr = rule1_vars.toArray(new Integer[0]);
        Integer[] rule2_var_arr = rule2_vars.toArray(new Integer[0]);
        Map<Integer, Integer> initial_var_map = new HashMap<>();
        for (int i = 0; i < rule1_var_arr.length; i++) {
            initial_var_map.put(rule2_var_arr[i], rule1_var_arr[i]);
        }
        for (Predicate predicate: rule2) {
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar) {
                    Integer var1_id = initial_var_map.get(argument.id);
                    predicate.args[arg_idx] = new Variable(var1_id);
                }
            }
        }
        return permuteVar(0, rule1_var_arr, rule2, rule1.get(0), new HashSet<>(rule1.subList(1, rule1.size())));
    }

    static boolean permuteVar(int startIdx, Integer[] rule1VarIds, List<Predicate> rule2, Predicate head1, Set<Predicate> body1) {
        if (startIdx >= rule1VarIds.length - 1) {
            /* Check */
            return head1.equals(rule2.get(0)) && body1.equals(new HashSet<>(rule2.subList(1, rule2.size())));
        }
        for (int i = startIdx; i < rule1VarIds.length; i++) {
            swap(rule1VarIds, rule2, i, startIdx);
            if (permuteVar(startIdx+1, rule1VarIds, rule2, head1, body1)) {
                return true;
            }
            swap(rule1VarIds, rule2, i, startIdx);
        }
        return false;
    }

    static void swap(Integer[] rule1VarIds, List<Predicate> rule2, int idx1, int idx2) {
        if (idx1 == idx2) {
            return;
        }
        final int id1 = rule1VarIds[idx1];
        final int id2 = rule1VarIds[idx2];
        rule1VarIds[idx1] = id2;
        rule1VarIds[idx2] = id1;
        for (Predicate predicate: rule2) {
            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
                Argument argument = predicate.args[arg_idx];
                if (null != argument && argument.isVar) {
                    if (id1 == argument.id) {
                        predicate.args[arg_idx] = new Variable(id2);
                    } else if (id2 == argument.id) {
                        predicate.args[arg_idx] = new Variable(id1);
                    }
                }
            }
        }
    }

//    static boolean matchRules1(List<Predicate> rule1, List<Predicate> rule2) {
//        /* Make copies of the two rules */
//        List<Predicate> tmp1 = new ArrayList<>(rule1.size());
//        List<Predicate> tmp2 = new ArrayList<>(rule2.size());
//        for (Predicate predicate: rule1) {
//            tmp1.add(new Predicate(predicate));
//        }
//        for (Predicate predicate: rule2) {
//            tmp2.add(new Predicate(predicate));
//        }
//        rule1 = tmp1;
//        rule2 = tmp2;
//
//        /* Turn vars in rule2 to negative ID */
//        for (Predicate predicate: rule2) {
//            for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
//                Argument argument = predicate.args[arg_idx];
//                if (null != argument && argument.isVar) {
//                    predicate.args[arg_idx] = new Variable(-argument.id-1);
//                }
//            }
//        }
//
//        /* Align the head */
//        Predicate head1 = rule1.get(0);
//        Predicate head2 = rule2.get(0);
//        if (!head1.functor.equals(head2.functor) || head1.arity() != head2.arity()) {
//            return false;
//        }
//        Map<Integer, Integer> var2_to_var1_map = new HashMap<>();
//        for (int arg_idx = 0; arg_idx < head1.args.length; arg_idx++) {
//            Argument argument1 = head1.args[arg_idx];
//            Argument argument2 = head2.args[arg_idx];
//            if (null == argument1) {
//                if (null != argument2) {
//                    return false;
//                }
//            } else if (argument1.isVar) {
//                if (!argument2.isVar) {
//                    return false;
//                }
//                Integer old_mapped_id = var2_to_var1_map.get(argument2.id);
//                if (null == old_mapped_id) {
//                    var2_to_var1_map.put(argument2.id, argument1.id);
//                } else {
//                    if (old_mapped_id != argument1.id) {
//                        return false;
//                    }
//                }
//            } else {
//                if (argument2.isVar || !argument1.name.equals(argument2.name)) {
//                    return false;
//                }
//            }
//        }
//        for (Predicate predicate2: rule2) {
//            for (int arg_idx = 0; arg_idx < predicate2.arity(); arg_idx++) {
//                Argument argument = predicate2.args[arg_idx];
//                if (null != argument && argument.isVar) {
//                    Integer mapped_id = var2_to_var1_map.get(argument.id);
//                    if (null != mapped_id) {
//                        predicate2.args[arg_idx] = new Variable(mapped_id);
//                    }
//                }
//            }
//        }
//
//        /* Match the body variables */
//        return matchBody1(rule1, rule2);
//    }
//
//    static boolean matchBody1(List<Predicate> rule1, List<Predicate> rule2) {
//        List<Predicate> body1 = rule1.subList(1, rule1.size());
//        List<Predicate> body2 = rule2.subList(1, rule2.size());
//
//        /* Collect variables in body1 */
//        Set<Integer> body1_vars = new HashSet<>();
//        for (Predicate predicate: body1) {
//            for (Argument argument: predicate.args) {
//                if (null != argument && argument.isVar) {
//                    body1_vars.add(argument.id);
//                }
//            }
//        }
//        for (Argument argument: rule1.get(0).args) {
//            if (null != argument && argument.isVar) {
//                body1_vars.remove(argument.id);
//            }
//        }
//
//        /* Normalize & collect variables in body2 */
//        Set<Integer> body2_vars = new HashSet<>();
//        for (Predicate predicate: body2) {
//            for (Argument argument: predicate.args) {
//                if (null != argument && argument.isVar && argument.id < 0) {
//                    body2_vars.add(argument.id);
//                }
//            }
//        }
//        if (body1_vars.size() != body2_vars.size()) {
//            return false;
//        }
//        Map<Integer, Integer> normalize_map = new HashMap<>();
//        for (Integer original_id: body2_vars) {
//            normalize_map.put(original_id, -normalize_map.size()-1);
//        }
//        for (Predicate predicate: body2) {
//            for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
//                Argument argument = predicate.args[arg_idx];
//                if (null != argument && argument.isVar) {
//                    predicate.args[arg_idx] = new Variable(normalize_map.get(argument.id));
//                }
//            }
//        }
//
//        /* For each permutation of body1 variables, remap body2 variables and check whether the bodies are equal */
//        Integer[] body1_var_ids = body1_vars.toArray(new Integer[0]);
//        for (Predicate predicate: body2) {
//            for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
//                Argument argument = predicate.args[arg_idx];
//                if (null != argument && argument.isVar && argument.id < 0) {
//                    predicate.args[arg_idx] = new Variable(body1_var_ids[-argument.id - 1]);
//                }
//            }
//        }
//        return permuteId1(0, body1_var_ids, body2, new HashSet<>(body1));
//    }
//
//    static boolean permuteId1(int startIdx, Integer[] body1VarIds, List<Predicate> body2, Set<Predicate> body1Set) {
//        if (startIdx >= body1VarIds.length - 1) {
//            /* Check */
//            return body1Set.equals(new HashSet<>(body2));
//        }
//        for (int i = startIdx; i < body1VarIds.length; i++) {
//            swap1(body1VarIds, body2, i, startIdx);
//            if (permuteId1(startIdx+1, body1VarIds, body2, body1Set)) {
//                return true;
//            }
//            swap1(body1VarIds, body2, i, startIdx);
//        }
//        return false;
//    }
//
//    static void swap1(Integer[] body1VarIds, List<Predicate> body2, int idx1, int idx2) {
//        if (idx1 == idx2) {
//            return;
//        }
//        final int id1 = body1VarIds[idx1];
//        final int id2 = body1VarIds[idx2];
//        body1VarIds[idx1] = id2;
//        body1VarIds[idx2] = id1;
//        for (Predicate predicate: body2) {
//            for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
//                Argument argument = predicate.args[arg_idx];
//                if (null != argument && argument.isVar) {
//                    if (id1 == argument.id) {
//                        predicate.args[arg_idx] = new Variable(id2);
//                    } else if (id2 == argument.id) {
//                        predicate.args[arg_idx] = new Variable(id1);
//                    }
//                }
//            }
//        }
//    }
}
