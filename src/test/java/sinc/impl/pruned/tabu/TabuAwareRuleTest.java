package sinc.impl.pruned.tabu;

import org.junit.jupiter.api.Test;
import sinc.common.Constant;
import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.impl.cached.MemKB;
import sinc.util.MultiSet;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TabuAwareRuleTest {

    public static final String HEAD_FUNC = "h";
    public static final int ARITY = 2;
    public static final MemKB KB = new MemKB();
    public static final int CONSTANT_ID = -1;
    public static final String CONSTANT_SYMBOL = "c";

    static {
        final Predicate head_pred = new Predicate(HEAD_FUNC, ARITY);
        head_pred.args[0] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        head_pred.args[1] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        final Predicate p = new Predicate("p", ARITY);
        p.args[0] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        p.args[1] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        final Predicate q = new Predicate("q", ARITY);
        q.args[0] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        q.args[1] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        final Predicate r = new Predicate("r", ARITY);
        r.args[0] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        r.args[1] = new Constant(CONSTANT_ID, CONSTANT_SYMBOL);
        KB.addFact(head_pred);
        KB.addFact(p);
        KB.addFact(q);
        KB.addFact(r);
    }

    @Test
    void testSubsets1() {
        /* {p, q, r} */
        final TabuAwareRule rule = new TabuAwareRule(HEAD_FUNC, new HashSet<>(), KB, new HashMap<>());
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar("p", ARITY, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("q", ARITY, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("r", ARITY, 0, 0));
        assertTrue(rule.toString().contains("h(X0,?):-p(X0,?),q(X0,?),r(X0,?)"));

        final Set<MultiSet<String>> expected_subset0 = new HashSet<>(Collections.singletonList(new MultiSet<>()));
        final Set<MultiSet<String>> actual_subset0 = rule.categorySubsets(0);
        assertEquals(expected_subset0, actual_subset0);

        final Set<MultiSet<String>> expected_subset1 = new HashSet<>();
        expected_subset1.add(new MultiSet<>(new String[]{"p"}));
        expected_subset1.add(new MultiSet<>(new String[]{"q"}));
        expected_subset1.add(new MultiSet<>(new String[]{"r"}));
        final Set<MultiSet<String>> actual_subset1 = rule.categorySubsets(1);
        assertEquals(expected_subset1, actual_subset1);

        final Set<MultiSet<String>> expected_subset2 = new HashSet<>();
        expected_subset2.add(new MultiSet<>(new String[]{"p", "q"}));
        expected_subset2.add(new MultiSet<>(new String[]{"p", "r"}));
        expected_subset2.add(new MultiSet<>(new String[]{"q", "r"}));
        final Set<MultiSet<String>> actual_subset2 = rule.categorySubsets(2);
        assertEquals(expected_subset2, actual_subset2);

        final Set<MultiSet<String>> expected_subset3 = new HashSet<>();
        expected_subset3.add(new MultiSet<>(new String[]{"p", "q", "r"}));
        final Set<MultiSet<String>> actual_subset3 = rule.categorySubsets(3);
        assertEquals(expected_subset3, actual_subset3);
    }

    @Test
    void testSubsets2() {
        /* {p, q, q, r, r} */
        final TabuAwareRule rule = new TabuAwareRule(HEAD_FUNC, new HashSet<>(), KB, new HashMap<>());
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVars2NewVar("p", ARITY, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("q", ARITY, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("r", ARITY, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("q", ARITY, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, rule.boundFreeVar2ExistingVar("r", ARITY, 0, 0));
        assertTrue(rule.toString().contains("h(X0,?):-p(X0,?),q(X0,?),r(X0,?),q(X0,?),r(X0,?)"));

        final Set<MultiSet<String>> expected_subset0 = new HashSet<>(Collections.singletonList(new MultiSet<>()));
        final Set<MultiSet<String>> actual_subset0 = rule.categorySubsets(0);
        assertEquals(expected_subset0, actual_subset0);

        final Set<MultiSet<String>> expected_subset1 = new HashSet<>();
        expected_subset1.add(new MultiSet<>(new String[]{"p"}));
        expected_subset1.add(new MultiSet<>(new String[]{"q"}));
        expected_subset1.add(new MultiSet<>(new String[]{"r"}));
        final Set<MultiSet<String>> actual_subset1 = rule.categorySubsets(1);
        assertEquals(expected_subset1, actual_subset1);

        final Set<MultiSet<String>> expected_subset2 = new HashSet<>();
        expected_subset2.add(new MultiSet<>(new String[]{"p", "q"}));
        expected_subset2.add(new MultiSet<>(new String[]{"p", "r"}));
        expected_subset2.add(new MultiSet<>(new String[]{"q", "q"}));
        expected_subset2.add(new MultiSet<>(new String[]{"q", "r"}));
        expected_subset2.add(new MultiSet<>(new String[]{"r", "r"}));
        final Set<MultiSet<String>> actual_subset2 = rule.categorySubsets(2);
        assertEquals(expected_subset2, actual_subset2);

        final Set<MultiSet<String>> expected_subset3 = new HashSet<>();
        expected_subset3.add(new MultiSet<>(new String[]{"p", "q", "q"}));
        expected_subset3.add(new MultiSet<>(new String[]{"p", "q", "r"}));
        expected_subset3.add(new MultiSet<>(new String[]{"p", "r", "r"}));
        expected_subset3.add(new MultiSet<>(new String[]{"q", "q", "r"}));
        expected_subset3.add(new MultiSet<>(new String[]{"q", "r", "r"}));
        final Set<MultiSet<String>> actual_subset3 = rule.categorySubsets(3);
        assertEquals(expected_subset3, actual_subset3);
    }

    @Test
    void testSubsets3() {
        /* {} */
        final TabuAwareRule rule = new TabuAwareRule(HEAD_FUNC, new HashSet<>(), KB, new HashMap<>());
        assertTrue(rule.toString().contains("h(?,?):-"));

        final Set<MultiSet<String>> expected_subset0 = new HashSet<>(Collections.singletonList(new MultiSet<>()));
        final Set<MultiSet<String>> actual_subset0 = rule.categorySubsets(0);
        assertEquals(expected_subset0, actual_subset0);
    }
}