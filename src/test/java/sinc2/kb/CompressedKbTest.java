package sinc2.kb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.rule.BareRule;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressedKbTest {

    static TestKbManager testKbManager;

    @BeforeAll
    static void setupKb() throws IOException {
        testKbManager = new TestKbManager();
    }

    @AfterAll
    static void removeKb() {
        testKbManager.cleanUpKb();
    }

    @Test
    void testCreateEmpty() throws IOException, KbException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        CompressedKb ckb = new CompressedKb("test", kb);

        assertEquals("test", ckb.getName());
        assertEquals(17, ckb.totalMappings());
        assertEquals(0, ckb.totalNecessaryRecords());
        assertEquals(0, ckb.totalCounterexamples());
        assertEquals(0, ckb.totalHypothesisSize());
        assertEquals(14, ckb.totalSupplementaryConstants());
        assertEquals(kb.getAllConstants(), ckb.supplementaryConstants);

        ckb.addRecord("family", new String[]{"diana", "erick", "frederick"});
        ckb.addRecords("family", new String[][]{
                new String[]{"gabby", "harry", "isaac"}, new String[]{"jena", "kyle", "lily"}
        });

        List<Predicate> rule_father_structure = List.of(
                new Predicate(ckb.name2Num("father"), new int[]{
                        Argument.variable(0), Argument.variable(1)
                }),
                new Predicate(ckb.name2Num("family"), new int[]{
                        Argument.EMPTY_VALUE, Argument.variable(0), Argument.variable(1)
                })
        );
        Rule rule_father = new BareRule(rule_father_structure, new HashSet<>(), new HashMap<>());
        List<Predicate> rule_mother_structure = List.of(
                new Predicate(ckb.name2Num("mother"), new int[]{
                        Argument.variable(0), Argument.constant(ckb.name2Num("catherine"))
                }),
                new Predicate(ckb.name2Num("family"), new int[]{
                        Argument.variable(0), Argument.EMPTY_VALUE, Argument.constant(ckb.name2Num("catherine"))
                })
        );
        Rule rule_mother = new BareRule(rule_mother_structure, new HashSet<>(), new HashMap<>());

        assertEquals("father(X0,X1):-family(?,X0,X1)", rule_father.toDumpString(ckb.numMap));
        assertEquals("mother(X0,catherine):-family(X0,?,catherine)", rule_mother.toDumpString(ckb.numMap));
        ckb.addHypothesisRule(rule_father);
        ckb.addHypothesisRules(new Rule[]{rule_mother});

        ckb.addCounterexample(ckb.name2Num("mother"), new int[]{
                Argument.decode(ckb.name2Num("bob")),Argument.decode(ckb.name2Num("bob"))
        });
        ckb.addCounterexamples(ckb.name2Num("father"), new int[][]{
                new int[]{Argument.decode(ckb.name2Num("marvin")),Argument.decode(ckb.name2Num("nataly"))},
                new int[]{Argument.decode(ckb.name2Num("kyle")),Argument.decode(ckb.name2Num("lily"))},
        });

        assertEquals(17, ckb.totalMappings());
        assertEquals(3, ckb.totalNecessaryRecords());
        assertEquals(3, ckb.totalCounterexamples());
        assertEquals(5, ckb.totalHypothesisSize());
        assertEquals(1, ckb.totalSupplementaryConstants());
        assertEquals(new HashSet<>(List.of(ckb.name2Num("alice"))), ckb.supplementaryConstants);
    }

    @Test
    void testLoad() throws IOException, KbException, RuleParseException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        CompressedKb ckb = new CompressedKb(testKbManager.getCkbName(), TestKbManager.MEM_DIR, kb);

        assertEquals(17, ckb.totalMappings());
        assertEquals(3, ckb.totalNecessaryRecords());
        assertEquals(3, ckb.totalCounterexamples());
        assertEquals(5, ckb.totalHypothesisSize());
        assertEquals(1, ckb.totalSupplementaryConstants());
        assertEquals(new HashSet<>(List.of(ckb.name2Num("alice"))), ckb.supplementaryConstants);
    }

    @Test
    void testDump() throws IOException, KbException, RuleParseException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        CompressedKb ckb = new CompressedKb(testKbManager.getCkbName(), TestKbManager.MEM_DIR, kb, true);
        String tmp_dir_path = testKbManager.createTmpDir();
        ckb.dump(tmp_dir_path);
        CompressedKb ckb2 = new CompressedKb(ckb.getName(), tmp_dir_path, kb, true);

        assertEquals(17, ckb2.totalMappings());
        assertEquals(3, ckb2.totalNecessaryRecords());
        assertEquals(3, ckb2.totalCounterexamples());
        assertEquals(5, ckb2.totalHypothesisSize());
        assertEquals(1, ckb2.totalSupplementaryConstants());
        assertEquals(new HashSet<>(List.of(ckb2.name2Num("alice"))), ckb2.supplementaryConstants);
    }

    @Test
    void testAddWithRemove() throws IOException, KbException, RuleParseException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        CompressedKb ckb = new CompressedKb(testKbManager.getCkbName(), TestKbManager.MEM_DIR, kb, true);

        ckb.addRecord("family", new String[]{"alice", "bob", "catherine"});
        ckb.addRecords("father", new String[][]{new String[]{"marvin", "nataly"}});
        List<Predicate> rule_mother_structure = List.of(
                new Predicate(ckb.name2Num("mother"), new int[]{
                        Argument.variable(0), Argument.constant(ckb.name2Num("catherine"))
                }),
                new Predicate(ckb.name2Num("family"), new int[]{
                        Argument.variable(0), Argument.EMPTY_VALUE, Argument.constant(ckb.name2Num("catherine"))
                })
        );
        Rule rule_mother = new BareRule(rule_mother_structure, new HashSet<>(), new HashMap<>());
        List<Predicate> rule_mother_structure2 = List.of(
                new Predicate(ckb.name2Num("mother"), new int[]{
                        Argument.variable(0), Argument.variable(1)
                }),
                new Predicate(ckb.name2Num("family"), new int[]{
                        Argument.variable(0), Argument.EMPTY_VALUE, Argument.variable(1)
                })
        );
        Rule rule_mother2 = new BareRule(rule_mother_structure2, new HashSet<>(), new HashMap<>());
        assertEquals("mother(X0,catherine):-family(X0,?,catherine)", rule_mother.toDumpString(ckb.numMap));
        assertEquals("mother(X0,X1):-family(X0,?,X1)", rule_mother2.toDumpString(ckb.numMap));
        ckb.removeHypothesisRule(rule_mother);
        ckb.addHypothesisRules(new Rule[]{rule_mother2});
        ckb.removeCounterexample(ckb.name2Num("mother"), new int[]{
                Argument.decode(ckb.name2Num("bob")),Argument.decode(ckb.name2Num("bob"))
        });
        ckb.removeCounterexample(ckb.name2Num("mother"), new int[]{
                Argument.decode(ckb.name2Num("bob")),Argument.decode(ckb.name2Num("bob"))
        });
        ckb.removeCounterexample(ckb.name2Num("father"),
                new int[]{Argument.decode(ckb.name2Num("marvin")),Argument.decode(ckb.name2Num("nataly"))
        });

        assertEquals(17, ckb.totalMappings());
        assertEquals(5, ckb.totalNecessaryRecords());
        assertEquals(1, ckb.totalCounterexamples());
        assertEquals(4, ckb.totalHypothesisSize());
        assertEquals(0, ckb.totalSupplementaryConstants());
    }

}