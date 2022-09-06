package sinc.exp;

import sinc.common.*;
import sinc.impl.cached.MemKB;
import sinc.util.MultiSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CompareSpecDetectionByBruteForce {
    public static int SAMPLE_NUM = 400;

    static class RulePair{
        final List<Predicate> original, extended;

        public RulePair(List<Predicate> original, List<Predicate> extended) {
            this.original = original;
            this.extended = extended;
        }
    }

    static MemKB kb;

    public static void main(String[] args) throws Exception {
        if (3 != args.length) {
            System.out.println("Usage: <Compare File Path> <KB File Path> <#Samples>");
        }
        final String COMP_FILE_PATH = args[0];
        final String KB_FILE_PATH = args[1];
        SAMPLE_NUM = Integer.parseInt(args[2]);

        BufferedReader reader = new BufferedReader(new FileReader(COMP_FILE_PATH));
        List<RulePair> rule_pairs = new ArrayList<>();
        for (int i = 0; i < SAMPLE_NUM; i++) {
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            if (null == line1 || null == line2) {
                break;
            }

            // Exclude Rules with constant?
            boolean contains_constant = false;
            List<Predicate> extended_rule = CompareDupDetectionByBruteForce.parseRule(line1);
            for (Predicate predicate: extended_rule) {
                for (Argument argument: predicate.args) {
                    if (null != argument && !argument.isVar) {
                        contains_constant = true;
                        break;
                    }
                }
            }
            if (contains_constant) {
                continue;
            }
            List<Predicate> original_rule = CompareDupDetectionByBruteForce.parseRule(line2);
            for (Predicate predicate: original_rule) {
                for (Argument argument: predicate.args) {
                    if (null != argument && !argument.isVar) {
                        contains_constant = true;
                        break;
                    }
                }
            }
            if (contains_constant) {
                continue;
            }
            rule_pairs.add(new RulePair(original_rule, extended_rule));
        }

        kb = loadKb(KB_FILE_PATH);

        System.out.println("--- Compare Basic ---");
        compareBasic(rule_pairs);
        System.out.flush();

        System.out.println("--- Compare Cache ---");
        compareProgress(rule_pairs);
    }

    static MemKB loadKb(String kbPath) throws IOException {
        MemKB kb = new MemKB();
        BufferedReader reader = new BufferedReader(new FileReader(kbPath));
        String line;
        while (null != (line = reader.readLine())) {
            final String[] components = line.split("\t");
            final Predicate predicate = new Predicate(components[0], components.length - 1);
            for (int i = 1; i < components.length; i++) {
                predicate.args[i - 1] = new Constant(Rule.CONSTANT_ARG_ID, components[i]);
            }
            kb.addFact(predicate);
        }
        return kb;
    }

    static void compareBasic(List<RulePair> rulePairs) {
        long fingerprint_time_cost_nano = 0;
        long brute_force_time_cost_nano = 0;
        int false_positive_cnt = 0;
        int positive_cnt = 0;
        int i = 0;
        for (RulePair rp: rulePairs) {
//            System.out.printf("TEST %d\n", i++);
//            System.out.println(toDumpString(rp.original));
//            System.out.println(toDumpString(rp.extended));
            /* Test Brute Force */
            long time_start = System.nanoTime();
            boolean match = matchRules(rp.original, rp.extended);
            long time_done = System.nanoTime();
            brute_force_time_cost_nano += time_done - time_start;
            if (!match) {
                false_positive_cnt++;
            }

            /* Test Fingerprint */
            RuleFingerPrint fp_original = new RuleFingerPrint(rp.original);
            RuleFingerPrint fp_extended = new RuleFingerPrint(rp.extended);
            time_start = System.nanoTime();
            match = fp_original.predecessorOf(fp_extended);
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
        Map<MultiSet<String>, Set<RuleFingerPrint>> fp_cache = new HashMap<>();
        Set<List<Predicate>> bf_cache = new HashSet<>();
        long fp_cache_time_nano = 0;
        long bf_cache_time_nano = 0;
        int false_positive_cnt = 0;
        int positive_cnt = 0;
        for (RulePair rp: rulePairs) {
            final MultiSet<String> functor_mset = new MultiSet<>();
            for (int pred_idx = Rule.FIRST_BODY_PRED_IDX; pred_idx < rp.original.size(); pred_idx++) {
                functor_mset.add(rp.original.get(pred_idx).functor);
            }
            final Set<RuleFingerPrint> tabu_set = fp_cache.computeIfAbsent(
                    functor_mset, k -> new HashSet<>()
            );
            tabu_set.add(new RuleFingerPrint(rp.original));
            bf_cache.add(rp.original);

            /* Test Fingerprint */
            long time_start = System.nanoTime();
            boolean match = tabuHit(fp_cache, rp.extended);
            long time_done = System.nanoTime();
            if (match) {
                positive_cnt++;
            }
            fp_cache_time_nano += time_done - time_start;

            /* Test Brute Force */
            time_start = System.nanoTime();
            match = false;
            for (List<Predicate> r: bf_cache) {
                if (matchRules(r, rp.extended)) {
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

    static boolean tabuHit(Map<MultiSet<String>, Set<RuleFingerPrint>> category2TabuSetMap, List<Predicate> rule) {
        boolean hit = false;
        RuleFingerPrint fingerprint = new RuleFingerPrint(rule);
        for (int subset_size = 0; subset_size < rule.size(); subset_size++) {
            for (MultiSet<String> category_subset : categorySubsets(rule, subset_size)) {
                final Set<RuleFingerPrint> tabu_set = category2TabuSetMap.get(category_subset);
                if (null == tabu_set) continue;
                for (RuleFingerPrint rfp : tabu_set) {
                    if (rfp.predecessorOf(fingerprint)) {
                        hit = true;
                        break;
                    }
                }
                if (hit) break;
            }
        }
        return hit;
    }

    static Set<MultiSet<String>> categorySubsets(List<Predicate> rule, int subsetSize) {
        final Set<MultiSet<String>> subsets = new HashSet<>();
        if (0 == subsetSize) {
            subsets.add(new MultiSet<>());
        } else {
            templateSubsetsHandler(rule, subsets, new String[subsetSize], subsetSize - 1, 1);
        }
        return subsets;
    }

    static void templateSubsetsHandler(
            List<Predicate> rule, Set<MultiSet<String>> subsets, String[] template, int depth, int startIdx
    ) {
        if (0 < depth) {
            for (int pred_idx = startIdx; pred_idx < rule.size(); pred_idx++) {
                template[depth] = rule.get(pred_idx).functor;
                templateSubsetsHandler(rule, subsets, template, depth-1, pred_idx+1);
            }
        } else {
            for (int pred_idx = startIdx; pred_idx < rule.size(); pred_idx++) {
                template[depth] = rule.get(pred_idx).functor;
                subsets.add(new MultiSet<>(template));
            }
        }
    }

    static boolean matchRules(List<Predicate> original, List<Predicate> target) {
        int original_length = ruleLength(original);
        int target_length = ruleLength(target);
        if (original_length >= target_length) {
            return CompareDupDetectionByBruteForce.matchRules(original, target);
        }
        List<List<Predicate>> extensions = findExtensions(original, kb.getFunctor2ArityMap());
        for (List<Predicate> extension : extensions) {
            if (matchRules(extension, target)) {
                return true;
            }
        }
        return false;
    }

    static int ruleLength(List<Predicate> rule) {
        int constants = 0;
        int vars = 0;
        Set<Integer> lvs = new HashSet<>();
        for (Predicate predicate: rule) {
            for (Argument argument: predicate.args) {
                if (null != argument) {
                    if (argument.isVar) {
                        vars++;
                        lvs.add(argument.id);
                    } else {
                        constants++;
                    }
                }
            }
        }
        return constants + vars - lvs.size();
    }

    static List<List<Predicate>> findExtensions(final List<Predicate> rule, Map<String, Integer> functor2ArityMap) {
        /* 先找到所有空白的参数 */
        class ArgPos {
            public final int predIdx;
            public final int argIdx;

            public ArgPos(int predIdx, int argIdx) {
                this.predIdx = predIdx;
                this.argIdx = argIdx;
            }
        }
        List<ArgPos> vacant_list = new ArrayList<>();    // 空白参数记录表：{pred_idx, arg_idx}
        Set<Integer> used_LVs = new HashSet<>();
        int max_var_id = 0;
        for (int pred_idx = 0; pred_idx < rule.size(); pred_idx++) {
            final Predicate pred_info = rule.get(pred_idx);
            for (int arg_idx = 0; arg_idx < pred_info.arity(); arg_idx++) {
                Argument argument = pred_info.args[arg_idx];
                if (null == argument) {
                    vacant_list.add(new ArgPos(pred_idx, arg_idx));
                } else if (argument.isVar) {
                    used_LVs.add(argument.id);
                    max_var_id = Math.max(max_var_id, argument.id);
                }
            }
        }

        /* 尝试增加已知变量 */
        List<List<Predicate>> extensions = new ArrayList<>();
        for (int var_id: used_LVs) {
            for (ArgPos vacant: vacant_list) {
                List<Predicate> new_rule = copyRule(rule);
                new_rule.get(vacant.predIdx).args[vacant.argIdx] = new Variable(var_id);
                extensions.add(new_rule);
            }

            for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
                /* 拓展一个谓词，并尝试一个已知变量 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    List<Predicate> new_rule = copyRule(rule);
                    Predicate predicate = new Predicate(functor, arity);
                    predicate.args[arg_idx] = new Variable(var_id);
                    new_rule.add(predicate);
                    extensions.add(new_rule);
                }
            }
        }

        int new_var_id = max_var_id+1;
        for (int i = 0; i < vacant_list.size(); i++) {
            /* 找到新变量的第一个位置 */
            final ArgPos first_vacant = vacant_list.get(i);
            final String functor1 = rule.get(first_vacant.predIdx).functor;

            /* 拓展一个常量 */
//            final Predicate predicate = rule.getPredicate(first_vacant.predIdx);
//            final List<String> const_list = func_2_promising_const_map.get(predicate.functor)[first_vacant.argIdx];
//            for (String const_symbol: const_list) {
//                final Rule new_rule = rule.clone();
//                final Rule.UpdateStatus update_status = new_rule.boundFreeVar2Constant(
//                        first_vacant.predIdx, first_vacant.argIdx, const_symbol
//                );
//                checkThenAddRule(update_status, new_rule, rule, candidates);
//            }

            /* 找到两个位置尝试同一个新变量 */
            for (int j = i + 1; j < vacant_list.size(); j++) {
                /* 新变量的第二个位置可以是当前rule中的其他空位 */
                final ArgPos second_vacant = vacant_list.get(j);
                List<Predicate> new_rule = copyRule(rule);
                new_rule.get(first_vacant.predIdx).args[first_vacant.argIdx] = new Variable(new_var_id);
                new_rule.get(second_vacant.predIdx).args[second_vacant.argIdx] = new Variable(new_var_id);
                extensions.add(new_rule);
            }
            for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
                /* 新变量的第二个位置也可以是拓展一个谓词以后的位置 */
                final String functor = entry.getKey();
                final int arity = entry.getValue();
                for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
                    List<Predicate> new_rule = copyRule(rule);
                    new_rule.get(first_vacant.predIdx).args[first_vacant.argIdx] = new Variable(new_var_id);
                    Predicate predicate = new Predicate(functor, arity);
                    predicate.args[arg_idx] = new Variable(new_var_id);
                    new_rule.add(predicate);
                    extensions.add(new_rule);
                }
            }
        }
        return extensions;
    }

    static List<Predicate> copyRule(List<Predicate> rule) {
        List<Predicate> copied = new ArrayList<>(rule.size());
        for (Predicate predicate: rule) {
            copied.add(new Predicate(predicate));
        }
        return copied;
    }

    static String toDumpString(List<Predicate> structure) {
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
}
