package sinc.impl.pruned.symmetric;

import sinc.SincConfig;
import sinc.common.Eval;
import sinc.common.InterruptedSignal;
import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.impl.pruned.tabu.SincWithTabuPruning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Sinc4Symmetric extends SincWithTabuPruning {
    public Sinc4Symmetric(SincConfig config, String kbPath, String dumpPath, String logPath) {
        super(config, kbPath, dumpPath, logPath);
    }

    @Override
    protected void findExtension(Rule rule, PriorityQueue<Rule> candidates) throws InterruptedSignal {
        Eval eval = rule.getEval();
        if (config.stopCompressionRate <= eval.value(Eval.EvalMetric.CompressionRate) || 0 == eval.getNegCnt()) {
            /* 如果到达停止阈值，不再进行extension */
            return;
        }

        /* 只考虑将rule扩展成为可能的对称规则 */
        final Predicate head_pred = rule.getHead();
        if (2 <= rule.size() || 2 != head_pred.arity()) {
            return;
        }

        if (0 == rule.size()) {
            /* Put the first var */
            final Map<String, Integer> func_2_arity_map = getFunctor2ArityMap();
            for (Map.Entry<String, Integer> entry: func_2_arity_map.entrySet()) {
                final String functor = entry.getKey();
                final Integer arity = entry.getValue();
                if (2 != arity) {
                    continue;
                }

                final Rule new_rule1 = rule.clone();
                final Rule.UpdateStatus update_status1 = new_rule1.boundFreeVars2NewVar(
                        functor, arity, 0, 0, 0
                );
                checkThenAddRule(update_status1, new_rule1, rule, candidates);

                final Rule new_rule2 = rule.clone();
                final Rule.UpdateStatus update_status2 = new_rule2.boundFreeVars2NewVar(
                        functor, arity, 1, 0, 0
                );
                checkThenAddRule(update_status2, new_rule2, rule, candidates);

                final Rule new_rule3 = rule.clone();
                final Rule.UpdateStatus update_status3 = new_rule3.boundFreeVars2NewVar(
                        functor, arity, 0, 0, 1
                );
                checkThenAddRule(update_status3, new_rule3, rule, candidates);

                final Rule new_rule4 = rule.clone();
                final Rule.UpdateStatus update_status4 = new_rule4.boundFreeVars2NewVar(
                        functor, arity, 1, 0, 1
                );
                checkThenAddRule(update_status4, new_rule4, rule, candidates);
            }
        } else {
            /* |r| = 1 */
            /* Complete the Symmetric form */
            class ArgPos {
                public final int predIdx;
                public final int argIdx;

                public ArgPos(int predIdx, int argIdx) {
                    this.predIdx = predIdx;
                    this.argIdx = argIdx;
                }
            }
            final ArgPos[] uv_poss = new ArgPos[2];  // only need 2
            int idx = 0;
            for (int pred_idx = Rule.HEAD_PRED_IDX; pred_idx < rule.length(); pred_idx++) {
                final Predicate predicate = rule.getPredicate(pred_idx);
                for (int arg_idx = 0; arg_idx < predicate.arity() && idx < 2; arg_idx++) {
                    if (null == predicate.args[arg_idx]) {
                        uv_poss[idx] = new ArgPos(pred_idx, arg_idx);
                        idx++;
                    }
                }
            }
            if (2 <= idx) {
                final Rule new_rule = rule.clone();
                final Rule.UpdateStatus update_status = new_rule.boundFreeVars2NewVar(
                        uv_poss[0].predIdx, uv_poss[0].argIdx, uv_poss[1].predIdx, uv_poss[1].argIdx
                );
                checkThenAddRule(update_status, new_rule, rule, candidates);
            }
        }
    }

    @Override
    public String getModelName() {
        return "Sym";
    }
}
