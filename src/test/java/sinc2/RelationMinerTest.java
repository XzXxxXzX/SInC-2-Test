package sinc2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.InterruptedSignal;
import sinc2.common.Predicate;
import sinc2.kb.KbException;
import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;
import sinc2.rule.*;
import sinc2.util.graph.GraphNode;

import java.io.PrintWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationMinerTest {

    static class TestRelationMiner extends RelationMiner {

        static final BareRule bad_rule = new BareRule(0, 0, new HashSet<>(), new HashMap<>());
        static {
            bad_rule.returningEval = Eval.MIN;
        }

        public TestRelationMiner(
                NumeratedKb kb, int targetRelation, EvalMetric evalMetric, int beamwidth, double stopCompressionRatio,
                Map<Predicate, GraphNode<Predicate>> predicate2NodeMap, Map<GraphNode<Predicate>,
                Set<GraphNode<Predicate>>> dependencyGraph, PrintWriter logger
        ) {
            super(kb, targetRelation, evalMetric, beamwidth, stopCompressionRatio, predicate2NodeMap, dependencyGraph, logger);
        }

        @Override
        protected Rule getStartRule() {
            return new BareRule(targetRelation, kb.getRelationArity(targetRelation), new HashSet<>(), new HashMap<>());
        }

        @Override
        protected int checkThenAddRule(UpdateStatus updateStatus, Rule updatedRule, Rule originalRule, Rule[] candidates) throws InterruptedSignal {
            return super.checkThenAddRule(updateStatus, updatedRule, bad_rule, candidates);
        }

        @Override
        protected void selectAsBeam(Rule r) {}
    }

    /*
     * KB: family/3, father/2, mother/2, isMale/1
     */
    static NumeratedKb kb = new NumeratedKb("test");

    @BeforeAll
    static void setupKb() throws KbException {
        kb.createRelation("family", 3);
        kb.createRelation("father", 2);
        kb.createRelation("mother", 2);
        kb.createRelation("isMale", 1);
        kb.addRecord("family", new String[]{"mom", "dad", "son"});
        kb.addRecord("family", new String[]{"mom", "dad", "daughter"});
        kb.addRecord("isMale", new String[]{"son"});
        KbRelation.MIN_CONSTANT_COVERAGE = 0.6;
        kb.updatePromisingConstants();
    }

    @Test
    void testFindSpecializations1() throws InterruptedSignal {
        Rule base_rule = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        assertEquals("family(?,?,?):-", base_rule.toDumpString(kb.getNumerationMap()));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(X0,X0,?):-",
                "family(X0,?,X0):-",
                "family(?,X0,X0):-",
//                "family(X0,?,?):-family(X0,?,?)",
                "family(X0,?,?):-family(?,X0,?)",
                "family(X0,?,?):-family(?,?,X0)",
                "family(?,X0,?):-family(X0,?,?)",
//                "family(?,X0,?):-family(?,X0,?)",
                "family(?,X0,?):-family(?,?,X0)",
                "family(?,?,X0):-family(X0,?,?)",
                "family(?,?,X0):-family(?,X0,?)",
//                "family(?,?,X0):-family(?,?,X0)",
                "family(X0,?,?):-mother(X0,?)",
                "family(X0,?,?):-mother(?,X0)",
                "family(?,X0,?):-mother(X0,?)",
                "family(?,X0,?):-mother(?,X0)",
                "family(?,?,X0):-mother(X0,?)",
                "family(?,?,X0):-mother(?,X0)",
                "family(X0,?,?):-father(X0,?)",
                "family(X0,?,?):-father(?,X0)",
                "family(?,X0,?):-father(X0,?)",
                "family(?,X0,?):-father(?,X0)",
                "family(?,?,X0):-father(X0,?)",
                "family(?,?,X0):-father(?,X0)",
                "family(X0,?,?):-isMale(X0)",
                "family(?,X0,?):-isMale(X0)",
                "family(?,?,X0):-isMale(X0)",
                "family(mom,?,?):-",
                "family(?,dad,?):-"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, kb.name2Num("family"), EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        assertEquals(expected_specs.size(), miner.findSpecializations(base_rule, spec_rules));
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb.getNumerationMap()));
        }
        assertEquals(expected_specs, actual_specs);
    }

    @Test
    void testFindSpecializations2() throws InterruptedSignal {
        Rule base_rule = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        base_rule.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        assertEquals("family(?,X0,?):-father(X0,?)", base_rule.toDumpString(kb.getNumerationMap()));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(X0,X0,?):-father(X0,?)",
                "family(?,X0,X0):-father(X0,?)",
                "family(?,X0,?):-father(X0,X0)",
                "family(?,X0,?):-father(X0,?),family(X0,?,?)",
//                "family(?,X0,?):-father(X0,?),family(?,X0,?)",
                "family(?,X0,?):-father(X0,?),family(?,?,X0)",
                "family(?,X0,?):-father(X0,?),mother(X0,?)",
                "family(?,X0,?):-father(X0,?),mother(?,X0)",
                "family(?,X0,?):-father(X0,?),father(X0,?)",
                "family(?,X0,?):-father(X0,?),father(?,X0)",
                "family(?,X0,?):-father(X0,?),isMale(X0)",
                "family(X1,X0,X1):-father(X0,?)",
                "family(X1,X0,?):-father(X0,X1)",
                "family(?,X0,X1):-father(X0,X1)",
//                "family(X1,X0,?):-father(X0,?),family(X1,?,?)",
                "family(X1,X0,?):-father(X0,?),family(?,X1,?)",
                "family(X1,X0,?):-father(X0,?),family(?,?,X1)",
                "family(?,X0,X1):-father(X0,?),family(X1,?,?)",
                "family(?,X0,X1):-father(X0,?),family(?,X1,?)",
//                "family(?,X0,X1):-father(X0,?),family(?,?,X1)",
                "family(?,X0,?):-father(X0,X1),family(X1,?,?)",
                "family(?,X0,?):-father(X0,X1),family(?,X1,?)",
                "family(?,X0,?):-father(X0,X1),family(?,?,X1)",
                "family(X1,X0,?):-father(X0,?),father(X1,?)",
                "family(X1,X0,?):-father(X0,?),father(?,X1)",
                "family(?,X0,X1):-father(X0,?),father(X1,?)",
                "family(?,X0,X1):-father(X0,?),father(?,X1)",
                "family(?,X0,?):-father(X0,X1),father(X1,?)",
                "family(?,X0,?):-father(X0,X1),father(?,X1)",
                "family(X1,X0,?):-father(X0,?),mother(X1,?)",
                "family(X1,X0,?):-father(X0,?),mother(?,X1)",
                "family(?,X0,X1):-father(X0,?),mother(X1,?)",
                "family(?,X0,X1):-father(X0,?),mother(?,X1)",
                "family(?,X0,?):-father(X0,X1),mother(X1,?)",
                "family(?,X0,?):-father(X0,X1),mother(?,X1)",
                "family(X1,X0,?):-father(X0,?),isMale(X1)",
                "family(?,X0,X1):-father(X0,?),isMale(X1)",
                "family(?,X0,?):-father(X0,X1),isMale(X1)",
                "family(mom,X0,?):-father(X0,?)"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, kb.name2Num("family"), EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        assertEquals(expected_specs.size(), miner.findSpecializations(base_rule, spec_rules));
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb.getNumerationMap()));
        }
        assertEquals(expected_specs, actual_specs);
    }

    @Test
    void testFindGeneralizations() throws InterruptedSignal {
        Rule base_rule = new BareRule(kb.name2Num("family"), 3, new HashSet<>(), new HashMap<>());
        base_rule.cvt2Uvs2NewLv(kb.name2Num("father"), 2, 0, 0, 1);
        base_rule.cvt2Uvs2NewLv(kb.name2Num("mother"), 2, 0, 0, 0);
        base_rule.cvt2Uvs2NewLv(0,2, 1, 1);
        base_rule.cvt1Uv2ExtLv(kb.name2Num("isMale"), 1, 0, 2);
        assertEquals("family(X1,X0,X2):-father(X0,X2),mother(X1,?),isMale(X2)", base_rule.toDumpString(kb.getNumerationMap()));

        Set<String> expected_specs = new HashSet<>(List.of(
                "family(?,X0,X1):-father(X0,X1),isMale(X1)",
                "family(X1,?,X0):-father(?,X0),mother(X1,?),isMale(X0)",
                "family(X1,X0,?):-father(X0,X2),mother(X1,?),isMale(X2)",
//                "family(X1,X0,X2):-father(X0,X2),mother(X1,?)",  // This rule is in the cache
                "family(X1,X0,X2):-father(X0,?),mother(X1,?),isMale(X2)"
        ));

        RelationMiner miner = new TestRelationMiner(
                kb, kb.name2Num("family"), EvalMetric.CompressionCapacity, 1, 1.0,
                new HashMap<>(), new HashMap<>(), new PrintWriter(System.out)
        );
        Rule[] spec_rules = new Rule[expected_specs.size() * 2];
        int added_rules = miner.findGeneralizations(base_rule, spec_rules);
        assertEquals(expected_specs.size(), added_rules);
        Set<String> actual_specs =new HashSet<>();
        for (Rule rule: spec_rules) {
            if (null == rule) {
                break;
            }
            actual_specs.add(rule.toDumpString(kb.getNumerationMap()));
        }
        assertEquals(expected_specs, actual_specs);
    }
}