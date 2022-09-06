package sinc.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RuleTest {
    static class RuleImpl extends Rule {

        public RuleImpl(String headFunctor, int arity, Set<RuleFingerPrint> cache) {
            super(headFunctor, arity, cache);
        }

        public RuleImpl(List<Predicate> structure, Set<RuleFingerPrint> searchedFingerprints) {
            super(structure, searchedFingerprints);
        }

        @Override
        public RuleImpl clone() {
            return new RuleImpl(this);
        }

        public RuleImpl(RuleImpl another) {
            super(another);
        }

        @Override
        protected double factCoverage() {
            return Rule.MIN_FACT_COVERAGE + 1;
        }

        @Override
        protected Eval calculateEval() {
            return Eval.MIN;
        }
    }

    @Test
    void testConstruction() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertTrue(r.toString().contains("h(?,?,?):-"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-"));
        assertEquals(0, r.size());
        assertEquals(1, r.length());
        assertEquals(0, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertTrue(r.toString().contains("h(X0,?,?):-p(X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0)"));
        assertEquals(1, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1)"));
        assertEquals(2, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(4, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(3, 0, "c"));
        assertTrue(r.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(c,X0)"));
        assertEquals(4, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(5, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(0, 2, "c"));
        assertTrue(r.toString().contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-p(X0),q(X2,X1),q(c,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(6, cache.size());

        Predicate predicate_head = new Predicate("h", 3);
        predicate_head.args[0] = new Variable(0);
        predicate_head.args[1] = new Variable(1);
        predicate_head.args[2] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        Predicate predicate_body1 = new Predicate("p", 1);
        predicate_body1.args[0] = new Variable(0);
        Predicate predicate_body2 = new Predicate("q", 2);
        predicate_body2.args[1] = new Variable(1);
        Predicate predicate_body3 = new Predicate("q", 2);
        predicate_body3.args[0] = new Constant(Rule.CONSTANT_ARG_ID, "c");
        predicate_body3.args[1] = new Variable(0);

        assertEquals(predicate_head, r.getPredicate(0));
        assertEquals(predicate_head, r.getHead());
        assertEquals(predicate_body1, r.getPredicate(1));
        assertEquals(predicate_body2, r.getPredicate(2));
        assertEquals(predicate_body3, r.getPredicate(3));
    }

    @Test
    void testConstructionAndRemoval1() {
        /* h(X, Y, c) <- p(X), q(?, Y), q(c, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(3, 0, "c"));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2Constant(0, 2, "c"));
        assertTrue(r.toString().contains("h(X0,X1,c):-p(X0),q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-p(X0),q(X2,X1),q(c,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(1, 0));
        assertTrue(r.toString().contains("h(X0,X1,c):-q(?,X1),q(c,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-q(X2,X1),q(c,X0)"));
        assertEquals(4, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(2, 0));
        assertTrue(r.toString().contains("h(X0,X1,c):-q(?,X1),q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,c):-q(X2,X1),q(X3,X0)"));
        assertEquals(3, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(2, 1));
        assertTrue(r.toString().contains("h(?,X0,c):-q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X0,c):-q(X2,X0)"));
        assertEquals(2, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(0, 2));
        assertTrue(r.toString().contains("h(?,X0,?):-q(?,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X0,X2):-q(X3,X0)"));
        assertEquals(1, r.size());
        assertEquals(2, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(4, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(0, 1));
        assertTrue(r.toString().contains("h(?,?,?):-"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-"));
        assertEquals(0, r.size());
        assertEquals(1, r.length());
        assertEquals(0, r.usedBoundedVars());
        assertEquals(5, cache.size());
        assertEquals(new Predicate("h", 3), r.getHead());
    }

    @Test
    void testConstructionAndRemoval2() {
        /* h(X, Y, Z) <- p(X), q(Z, Y), q(Z, X) */
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r = new RuleImpl("h", 3, cache);
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVars2NewVar(2, 0, 0, 2));
        assertEquals(Rule.UpdateStatus.NORMAL, r.boundFreeVar2ExistingVar(3, 0, 2));
        assertTrue(r.toString().contains("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals(5, r.size());
        assertEquals(4, r.length());
        assertEquals(3, r.usedBoundedVars());
        assertEquals(6, cache.size());

        cache.clear();
        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(0, 0));
        assertTrue(r.toString().contains("h(?,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertTrue(r.toCompleteRuleString().contains("h(X3,X1,X2):-p(X0),q(X2,X1),q(X2,X0)"));
        assertEquals(4, r.size());
        assertEquals(4, r.length());
        assertEquals(3, r.usedBoundedVars());
        assertEquals(1, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(3, 1));
        assertTrue(r.toString().contains("h(?,X1,X0):-q(X0,X1),q(X0,?)"));
        assertTrue(r.toCompleteRuleString().contains("h(X2,X1,X0):-q(X0,X1),q(X0,X3)"));
        assertEquals(3, r.size());
        assertEquals(3, r.length());
        assertEquals(2, r.usedBoundedVars());
        assertEquals(2, cache.size());

        assertEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(1, 1));
        assertTrue(r.toString().contains("h(?,?,X0):-q(X0,?),q(X0,?)"));
        assertTrue(r.toCompleteRuleString().contains("h(X1,X2,X0):-q(X0,X3),q(X0,X4)"));
        assertEquals(2, r.size());
        assertEquals(3, r.length());
        assertEquals(1, r.usedBoundedVars());
        assertEquals(3, cache.size());

        assertNotEquals(Rule.UpdateStatus.NORMAL, r.removeBoundedArg(0, 2));
    }

    @Test
    void testCopyConstructor() {
        final Set<RuleFingerPrint> cache = new HashSet<>();
        Rule r1 = new RuleImpl("h", 3, cache);
        assertEquals(Rule.UpdateStatus.NORMAL, r1.boundFreeVars2NewVar("p", 1, 0, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r1.boundFreeVars2NewVar("q", 2, 1, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, r1.boundFreeVar2ExistingVar("q", 2, 1, 0));
        assertTrue(r1.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r1.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r1.size());
        assertEquals(4, r1.length());
        assertEquals(2, r1.usedBoundedVars());
        assertEquals(4, cache.size());

        Rule r2 = r1.clone();
        assertEquals(Rule.UpdateStatus.NORMAL, r2.removeBoundedArg(1, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, r2.removeBoundedArg(0, 0));
        assertTrue(r2.toString().contains("h(?,X0,?):-q(?,X0)"));
        assertTrue(r2.toCompleteRuleString().contains("h(X1,X0,X2):-q(X3,X0)"));
        assertEquals(1, r2.size());
        assertEquals(2, r2.length());
        assertEquals(1, r2.usedBoundedVars());
        assertEquals(6, cache.size());

        assertTrue(r1.toString().contains("h(X0,X1,?):-p(X0),q(?,X1),q(?,X0)"));
        assertTrue(r1.toCompleteRuleString().contains("h(X0,X1,X2):-p(X0),q(X3,X1),q(X4,X0)"));
        assertEquals(3, r1.size());
        assertEquals(4, r1.length());
        assertEquals(2, r1.usedBoundedVars());
        assertEquals(6, cache.size());
    }

    @Test
    void testStructureConstructor1() {
        /* h(?,?,?) :- */
        RuleImpl expected_rule = new RuleImpl("h", 3, new HashSet<>());
        assertTrue(expected_rule.toDumpString().contains("h(?,?,?):-"));

        RuleImpl actual_rule = new RuleImpl(expected_rule.structure, new HashSet<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor2() {
        /* h(X,X,?) :- p(X,X) */
        RuleImpl expected_rule = new RuleImpl("h", 3, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar(0, 0, 0, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2ExistingVar("p", 2, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2ExistingVar(1, 1, 0));
        assertTrue(expected_rule.toDumpString().contains("h(X0,X0,?):-p(X0,X0)"));

        RuleImpl actual_rule = new RuleImpl(expected_rule.structure, new HashSet<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor3() {
        /* h(?,?,X) :- p(X), q(X,Y), q(Y,?) */
        RuleImpl expected_rule = new RuleImpl("h", 3, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("p", 1,0, 0, 2));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2ExistingVar("q", 2, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("q",2, 0, 2, 1));
        assertTrue(expected_rule.toDumpString().contains("h(?,?,X0):-p(X0),q(X0,X1),q(X1,?)"));

        RuleImpl actual_rule = new RuleImpl(expected_rule.structure, new HashSet<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    @Test
    void testStructureConstructor4() {
        /* h(?,d,X) :- p(X), q(X,Y), q(Y,c) */
        RuleImpl expected_rule = new RuleImpl("h", 3, new HashSet<>());
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("p", 1,0, 0, 2));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2ExistingVar("q", 2, 0, 0));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVars2NewVar("q",2, 0, 2, 1));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2Constant(0,1,"d"));
        assertEquals(Rule.UpdateStatus.NORMAL, expected_rule.boundFreeVar2Constant(3,1,"c"));
        assertTrue(expected_rule.toDumpString().contains("h(?,d,X0):-p(X0),q(X0,X1),q(X1,c)"));

        RuleImpl actual_rule = new RuleImpl(expected_rule.structure, new HashSet<>());
        structureIdentityCheck(expected_rule, actual_rule);
    }

    void structureIdentityCheck(RuleImpl expected, RuleImpl actual) {
        assertEquals(expected.structure, actual.structure);
        assertEquals(expected.boundedVars, actual.boundedVars);
        assertEquals(expected.boundedVarCnts, actual.boundedVarCnts);
        assertEquals(expected.fingerPrint, actual.fingerPrint);
        assertEquals(expected.equivConds, actual.equivConds);
    }
}
