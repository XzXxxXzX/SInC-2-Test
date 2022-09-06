package sinc2.impl.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.kb.Record;
import sinc2.rule.*;
import sinc2.util.ComparableArray;
import sinc2.util.MultiSet;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CachedRuleTest {

    static final int NUM_FATHER = 1;
    static final int NUM_PARENT = 2;
    static final int NUM_GRANDPARENT = 3;
    static final int NUM_G1 = 4;
    static final int NUM_G2 = 5;
    static final int NUM_G3 = 6;
    static final int NUM_G4 = 7;
    static final int NUM_F1 = 8;
    static final int NUM_F2 = 9;
    static final int NUM_F3 = 10;
    static final int NUM_F4 = 11;
    static final int NUM_M2 = 12;
    static final int NUM_S1 = 13;
    static final int NUM_S2 = 14;
    static final int NUM_S3 = 15;
    static final int NUM_S4 = 16;
    static final int NUM_D1 = 17;
    static final int NUM_D2 = 18;
    static final int NUM_D4 = 19;
    static final Record FATHER1 = new Record(new int[]{NUM_F1, NUM_S1});
    static final Record FATHER2 = new Record(new int[]{NUM_F2, NUM_S2});
    static final Record FATHER3 = new Record(new int[]{NUM_F2, NUM_D2});
    static final Record FATHER4 = new Record(new int[]{NUM_F3, NUM_S3});
    static final Record FATHER5 = new Record(new int[]{NUM_F4, NUM_D4});
    static final Record PARENT1 = new Record(new int[]{NUM_F1, NUM_S1});
    static final Record PARENT2 = new Record(new int[]{NUM_F1, NUM_D1});
    static final Record PARENT3 = new Record(new int[]{NUM_F2, NUM_S2});
    static final Record PARENT4 = new Record(new int[]{NUM_F2, NUM_D2});
    static final Record PARENT5 = new Record(new int[]{NUM_M2, NUM_D2});
    static final Record PARENT6 = new Record(new int[]{NUM_G1, NUM_F1});
    static final Record PARENT7 = new Record(new int[]{NUM_G2, NUM_F2});
    static final Record PARENT8 = new Record(new int[]{NUM_G2, NUM_M2});
    static final Record PARENT9 = new Record(new int[]{NUM_G3, NUM_F3});
    static final Record GRAND1 = new Record(new int[]{NUM_G1, NUM_S1});
    static final Record GRAND2 = new Record(new int[]{NUM_G2, NUM_D2});
    static final Record GRAND3 = new Record(new int[]{NUM_G4, NUM_S4});

    static NumeratedKb kbFamily() throws KbException  {
        NumeratedKb kb = new NumeratedKb("family");
        assertEquals(NUM_FATHER, kb.mapName("father"));
        assertEquals(NUM_PARENT, kb.mapName("parent"));
        assertEquals(NUM_GRANDPARENT, kb.mapName("grandParent"));
        assertEquals(NUM_G1, kb.mapName("g1"));
        assertEquals(NUM_G2, kb.mapName("g2"));
        assertEquals(NUM_G3, kb.mapName("g3"));
        assertEquals(NUM_G4, kb.mapName("g4"));
        assertEquals(NUM_F1, kb.mapName("f1"));
        assertEquals(NUM_F2, kb.mapName("f2"));
        assertEquals(NUM_F3, kb.mapName("f3"));
        assertEquals(NUM_F4, kb.mapName("f4"));
        assertEquals(NUM_M2, kb.mapName("m2"));
        assertEquals(NUM_S1, kb.mapName("s1"));
        assertEquals(NUM_S2, kb.mapName("s2"));
        assertEquals(NUM_S3, kb.mapName("s3"));
        assertEquals(NUM_S4, kb.mapName("s4"));
        assertEquals(NUM_D1, kb.mapName("d1"));
        assertEquals(NUM_D2, kb.mapName("d2"));
        assertEquals(NUM_D4, kb.mapName("d4"));
        assertEquals(NUM_FATHER, kb.createRelation("father", 2).getNumeration());
        assertEquals(NUM_PARENT, kb.createRelation("parent", 2).getNumeration());
        assertEquals(NUM_GRANDPARENT, kb.createRelation("grandParent", 2).getNumeration());

        /* father(X, Y):
         *   f1, s1
         *   f2, s2
         *   f2, d2
         *   f3, s3
         *   f4, d4
         */
        kb.addRecord(NUM_FATHER, FATHER1);
        kb.addRecord(NUM_FATHER, FATHER2);
        kb.addRecord(NUM_FATHER, FATHER3);
        kb.addRecord(NUM_FATHER, FATHER4);
        kb.addRecord(NUM_FATHER, FATHER5);

        /* parent(X, Y):
         *   f1, s1
         *   f1, d1
         *   f2, s2
         *   f2, d2
         *   m2, d2
         *   g1, f1
         *   g2, f2
         *   g2, m2
         *   g3, f3
         */
        kb.addRecord(NUM_PARENT, PARENT1);
        kb.addRecord(NUM_PARENT, PARENT2);
        kb.addRecord(NUM_PARENT, PARENT3);
        kb.addRecord(NUM_PARENT, PARENT4);
        kb.addRecord(NUM_PARENT, PARENT5);
        kb.addRecord(NUM_PARENT, PARENT6);
        kb.addRecord(NUM_PARENT, PARENT7);
        kb.addRecord(NUM_PARENT, PARENT8);
        kb.addRecord(NUM_PARENT, PARENT9);

        /* grandParent(X, Y):
         *   g1, s1
         *   g2, d2
         *   g4, s4
         */
        kb.addRecord(NUM_GRANDPARENT, GRAND1);
        kb.addRecord(NUM_GRANDPARENT, GRAND2);
        kb.addRecord(NUM_GRANDPARENT, GRAND3);

        /* Constants(16):
         *   g1, g2, g3, g4
         *   f1, f2, f3, f4
         *   m2
         *   s1, s2, s3, s4
         *   d1, d2, d4
         */
        return kb;
    }

    @BeforeEach
    void setParameters() {
        Rule.MIN_FACT_COVERAGE = -1.0;
    }

    @Test
    void testFamilyRule1() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("parent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* parent(X, ?) :- father(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 0, 0));
        assertEquals("parent(X0,?):-father(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 4, 4 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{PARENT1, FATHER1}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{PARENT2, FATHER1}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{PARENT3, FATHER2}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{PARENT4, FATHER2}));
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{PARENT1, FATHER1}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{PARENT2, FATHER1}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{PARENT3, FATHER2}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{PARENT4, FATHER3}));
        final Set<ComparableArray<Record>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{PARENT1, FATHER1}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{PARENT2, FATHER1}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{PARENT3, FATHER3}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{PARENT4, FATHER2}));
        final Set<ComparableArray<Record>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{PARENT1, FATHER1}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{PARENT2, FATHER1}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{PARENT3, FATHER3}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{PARENT4, FATHER3}));
        final Set<Record> expected_counter_examples = new HashSet<>();
        for (int arg1: new int[]{NUM_F1, NUM_F2, NUM_F3, NUM_F4}) {
            for (int arg2: kb.getAllConstants()) {
                expected_counter_examples.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counter_examples.remove(PARENT1);
        expected_counter_examples.remove(PARENT2);
        expected_counter_examples.remove(PARENT3);
        expected_counter_examples.remove(PARENT4);
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_PARENT, NUM_FATHER}, new Set[]{
                expected_grounding_set1, expected_grounding_set2, expected_grounding_set3, expected_grounding_set4
        });
        assertEquals(expected_counter_examples, rule.getCounterexamples());

        /* parent(X, Y) :- father(X, Y) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 1, 1, 1));
        assertEquals("parent(X0,X1):-father(X0,X1)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 2, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_PARENT, NUM_FATHER}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        Record counter1 = new Record(new int[]{NUM_F3, NUM_S3});
        Record counter2 = new Record(new int[]{NUM_F4, NUM_D4});
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule2() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("parent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* parent(?, X) :- father(?, X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 1));
        assertEquals("parent(?,X0):-father(?,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 4, 5 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{PARENT1, FATHER1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{PARENT3, FATHER2}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{PARENT4, FATHER3}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{PARENT5, FATHER3}));
        Set<Record> expected_counter_examples = new HashSet<>();
        for (int arg1: kb.getAllConstants()) {
            for (int arg2: new int[]{NUM_S1, NUM_S2, NUM_D2, NUM_S3, NUM_D4}) {
                expected_counter_examples.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counter_examples.remove(PARENT1);
        expected_counter_examples.remove(PARENT3);
        expected_counter_examples.remove(PARENT4);
        expected_counter_examples.remove(PARENT5);
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_PARENT, NUM_FATHER}, new Set[]{expected_grounding_set});
        assertEquals(expected_counter_examples, rule.getCounterexamples());

        /* parent(Y, X) :- father(Y, X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 0, 1, 0));
        assertEquals("parent(X1,X0):-father(X1,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 2, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_PARENT, NUM_FATHER}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        Record counter1 = new Record(new int[]{NUM_F3, NUM_S3});
        Record counter2 = new Record(new int[]{NUM_F4, NUM_D4});
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule3() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_GRANDPARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("grandParent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals("grandParent(X0,?):-parent(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, Y) :- parent(X, ?), parent(?, Y) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 1, 0, 1));
        assertEquals("grandParent(X0,X1):-parent(X0,?),parent(?,X1)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 6 * 8, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, PARENT5}));
        final Set<ComparableArray<Record>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{GRAND2, PARENT8, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{GRAND2, PARENT8, PARENT5}));
        Set<Record> expected_counter_examples = new HashSet<>();
        for (int arg1: new int[]{NUM_F1, NUM_F2, NUM_M2, NUM_G1, NUM_G2, NUM_G3}) {
            for (int arg2: new int[]{NUM_S1, NUM_S2, NUM_D1, NUM_D2, NUM_F1, NUM_F2, NUM_M2, NUM_F3}) {
                expected_counter_examples.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counter_examples.remove(GRAND1);
        expected_counter_examples.remove(GRAND2);
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_GRANDPARENT, NUM_PARENT, NUM_PARENT}, new Set[]{
                expected_grounding_set1, expected_grounding_set2, expected_grounding_set3, expected_grounding_set4
        });
        assertEquals(expected_counter_examples, rule.getCounterexamples());

        /* grandParent(X, Y) :- parent(X, Z), parent(Z, Y) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(1, 1, 2, 0));
        assertEquals("grandParent(X0,X1):-parent(X0,X2),parent(X2,X1)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 2, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedLimitedVars());
        assertEquals(3, rule.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_GRANDPARENT, NUM_PARENT, NUM_PARENT}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        Record counter1 = new Record(new int[]{NUM_G1, NUM_D1});
        Record counter2 = new Record(new int[]{NUM_G2, NUM_S2});
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule4() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_GRANDPARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("grandParent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals("grandParent(X0,?):-parent(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, Y), parent(Y, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 1, 1));
        assertEquals("grandParent(X0,?):-parent(X0,X1),parent(X1,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 2 * 16, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, Z) :- parent(X, Y), parent(Y, Z) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(2, 1, 0, 1));
        assertEquals("grandParent(X0,X2):-parent(X0,X1),parent(X1,X2)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedLimitedVars());
        assertEquals(3, rule.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, PARENT5}));
        final Set<ComparableArray<Record>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{GRAND2, PARENT8, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, PARENT1}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{GRAND2, PARENT8, PARENT5}));
        Record counter1 = new Record(new int[]{NUM_G1, NUM_D1});
        Record counter2 = new Record(new int[]{NUM_G2, NUM_S2});
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_GRANDPARENT, NUM_PARENT, NUM_PARENT}, new Set[]{
                expected_grounding_set1, expected_grounding_set2, expected_grounding_set3, expected_grounding_set4
        });
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule5() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_GRANDPARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("grandParent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals("grandParent(X0,?):-parent(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, Y), father(Y, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 1, 1));
        assertEquals("grandParent(X0,?):-parent(X0,X1),father(X1,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 3 * 16, 2),
                rule.getEval()
        );
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(2, 1, 0, 1));
        assertEquals("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule.getEval()
        );
        assertEquals(3, rule.usedLimitedVars());
        assertEquals(3, rule.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, FATHER1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, FATHER3}));
        Record counter1 = new Record(new int[]{NUM_G2, NUM_S2});
        Record counter2 = new Record(new int[]{NUM_G3, NUM_S3});
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_GRANDPARENT, NUM_PARENT, NUM_FATHER}, new Set[]{expected_grounding_set});
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule6() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_GRANDPARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("grandParent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(X, ?) :- parent(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 1));
        assertEquals("grandParent(?,X0):-father(?,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 5 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* grandParent(g1, X) :- father(?, X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2Const(0, 0, NUM_G1));
        assertEquals("grandParent(g1,X0):-father(?,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 1, 5, 2),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{GRAND1, FATHER1}));
        Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg2: new int[]{NUM_S2, NUM_D2, NUM_S3, NUM_D4}) {
            expected_counterexample_set.add(new Record(new int[]{NUM_G1, arg2}));
        }
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_GRANDPARENT, NUM_FATHER}, new Set[]{expected_grounding_set});
        assertEquals(expected_counterexample_set, rule.getCounterexamples());

        /* grandParent(g1, X) :- father(f2, X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2Const(1, 0, NUM_F2));
        assertEquals("grandParent(g1,X0):-father(f2,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 2, 3),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(3, rule.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Record counter1 = new Record(new int[]{NUM_G1, NUM_S2});
        Record counter2 = new Record(new int[]{NUM_G1, NUM_D2});
        actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_GRANDPARENT, NUM_FATHER}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule.getCounterexamples());
    }

    @Test
    void testFamilyRule7() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("parent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* parent(X, X) :- */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 0, 0, 1));
        assertEquals("parent(X0,X0):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg: kb.getAllConstants()) {
            expected_counterexample_set.add(new Record(new int[]{arg, arg}));
        }
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_PARENT}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        assertEquals(expected_counterexample_set, rule.getCounterexamples());
    }

    @Test
    void testFamilyRule8() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* father(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_FATHER, 2, fp_cache, tabu_map, kb);
        assertEquals("father(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* father(X, ?):- parent(?, X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_PARENT, 2, 1, 0, 0));
        assertEquals("father(X0,?):-parent(?,X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 4, 8 * 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* father(X, ?):- parent(?, X), parent(X, ?) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(NUM_PARENT, 2, 0, 0));
        assertEquals("father(X0,?):-parent(?,X0),parent(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 3 * 16, 2),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(2, rule.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT1}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT3}));
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT3}));
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT1}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT3}));
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT1}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT4}));
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT3}));
        final Set<ComparableArray<Record>> expected_grounding_set4 = new HashSet<>();
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT1}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT4}));
        expected_grounding_set4.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set5 = new HashSet<>();
        expected_grounding_set5.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT2}));
        expected_grounding_set5.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT3}));
        expected_grounding_set5.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT3}));
        final Set<ComparableArray<Record>> expected_grounding_set6 = new HashSet<>();
        expected_grounding_set6.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT2}));
        expected_grounding_set6.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT3}));
        expected_grounding_set6.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT4}));
        final Set<ComparableArray<Record>> expected_grounding_set7 = new HashSet<>();
        expected_grounding_set7.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT2}));
        expected_grounding_set7.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT4}));
        expected_grounding_set7.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT3}));
        final Set<ComparableArray<Record>> expected_grounding_set8 = new HashSet<>();
        expected_grounding_set8.add(new ComparableArray<>(new Record[]{FATHER1, PARENT6, PARENT2}));
        expected_grounding_set8.add(new ComparableArray<>(new Record[]{FATHER2, PARENT7, PARENT4}));
        expected_grounding_set8.add(new ComparableArray<>(new Record[]{FATHER3, PARENT7, PARENT4}));
        final Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg1: new int[]{NUM_F1, NUM_F2, NUM_M2}) {
            for (int arg2: kb.getAllConstants()) {
                expected_counterexample_set.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counterexample_set.remove(FATHER1);
        expected_counterexample_set.remove(FATHER2);
        expected_counterexample_set.remove(FATHER3);
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        checkEvidence(actual_evidence, new int[]{NUM_FATHER, NUM_PARENT, NUM_PARENT}, new Set[]{
                expected_grounding_set1, expected_grounding_set2, expected_grounding_set3, expected_grounding_set4,
                expected_grounding_set5, expected_grounding_set6, expected_grounding_set7, expected_grounding_set8
        });
        assertEquals(expected_counterexample_set, rule.getCounterexamples());
    }

    @Test
    void testFamilyRule9() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* father(?, ?) :- */
        final CachedRule rule1 = new CachedRule(NUM_FATHER, 2, fp_cache, tabu_map, kb);
        assertEquals("father(?,?):-", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedLimitedVars());
        assertEquals(0, rule1.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #1: father(f2,?):- */
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt1Uv2Const(0, 0, NUM_F2));
        assertEquals("father(f2,?):-", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 16, 1),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedLimitedVars());
        assertEquals(1, rule1.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());
        rule1.getEvidenceAndMarkEntailment();

        /* father(?, ?) :- */
        final Set<Fingerprint> fp_cache2 = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map2 = new HashMap<>();
        final CachedRule rule2 = new CachedRule(NUM_FATHER, 2, fp_cache2, tabu_map2, kb);
        assertEquals("father(?,?):-", rule2.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16 - 2, 0),
                rule2.getEval()
        );
        assertEquals(0, rule2.usedLimitedVars());
        assertEquals(0, rule2.length());
        assertEquals(1, fp_cache2.size());
        assertEquals(0, tabu_map2.size());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{FATHER1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{FATHER4}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{FATHER5}));
        final Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg1: kb.getAllConstants()) {
            for (int arg2: kb.getAllConstants()) {
                expected_counterexample_set.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counterexample_set.remove(FATHER1);
        expected_counterexample_set.remove(FATHER2);
        expected_counterexample_set.remove(FATHER3);
        expected_counterexample_set.remove(FATHER4);
        expected_counterexample_set.remove(FATHER5);
        checkEvidence(rule2.getEvidenceAndMarkEntailment(), new int[]{NUM_FATHER}, new Set[]{expected_grounding_set});
        assertEquals(expected_counterexample_set, rule2.getCounterexamples());
    }

    @Test
    void testFamilyRule10() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("parent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* parent(X, X):- */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 0, 0, 1));
        assertEquals("parent(X0,X0):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 16, 1),
                rule.getEval()
        );
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg: kb.getAllConstants()) {
            expected_counterexample_set.add(new Record(new int[]{arg, arg}));
        }
        EvidenceBatch actual_evidence = rule.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_PARENT}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        assertEquals(expected_counterexample_set, rule.getCounterexamples());
    }

    @Test
    void testCounterexample1() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* father(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_FATHER, 2, fp_cache, tabu_map, kb);
        assertEquals("father(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 5, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg1: kb.getAllConstants()) {
            for (int arg2: kb.getAllConstants()) {
                expected_counterexample_set.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counterexample_set.removeAll(kb.getRelation(NUM_FATHER).getRecords());
        assertEquals(expected_counterexample_set, rule.getCounterexamples());
    }

    @Test
    void testFamilyWithCopy1() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* grandParent(?, ?) :- */
        final CachedRule rule = new CachedRule(NUM_GRANDPARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("grandParent(?,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 3, 16 * 16, 0),
                rule.getEval()
        );
        assertEquals(0, rule.usedLimitedVars());
        assertEquals(0, rule.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #1: grandParent(X, ?) :- parent(X, ?) */
        final CachedRule rule1 = new CachedRule(rule);
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals("grandParent(X0,?):-parent(X0,?)", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 6 * 16, 1),
                rule1.getEval()
        );
        assertEquals(1, rule1.usedLimitedVars());
        assertEquals(1, rule1.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #1: grandParent(X, ?) :- parent(X, Y), father(Y, ?) */
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 1, 1));
        assertEquals("grandParent(X0,?):-parent(X0,X1),father(X1,?)", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 3 * 16, 2),
                rule1.getEval()
        );
        assertEquals(2, rule1.usedLimitedVars());
        assertEquals(2, rule1.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #1: grandParent(X, Z) :- parent(X, Y), father(Y, Z) */
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt2Uvs2NewLv(0, 1, 2, 1));
        assertEquals("grandParent(X0,X2):-parent(X0,X1),father(X1,X2)", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 4, 3),
                rule1.getEval()
        );
        assertEquals(3, rule1.usedLimitedVars());
        assertEquals(3, rule1.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{GRAND1, PARENT6, FATHER1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{GRAND2, PARENT7, FATHER3}));
        Record counter1 = new Record(new int[]{NUM_G2, NUM_S2});
        Record counter2 = new Record(new int[]{NUM_G3, NUM_S3});
        checkEvidence(rule1.getEvidenceAndMarkEntailment(), new int[]{NUM_GRANDPARENT, NUM_PARENT, NUM_FATHER}, new Set[]{expected_grounding_set});
        assertEquals(new HashSet<>(List.of(counter1, counter2)), rule1.getCounterexamples());

        /* #2: grandParent(X, ?) :- parent(X, ?) */
        final CachedRule rule2 = new CachedRule(rule);
        rule2.updateCacheIndices();
        assertEquals(UpdateStatus.DUPLICATED, rule2.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #3: grandParent(?, X) :- father(?, X) */
        final CachedRule rule3 = new CachedRule(rule);
        rule3.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule3.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 1));
        assertEquals("grandParent(?,X0):-father(?,X0)", rule3.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 5 * 16 - 2, 1),
                rule3.getEval()
        );
        assertEquals(1, rule3.usedLimitedVars());
        assertEquals(1, rule3.length());
        assertEquals(5, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #3: grandParent(Y, X) :- father(?, X), parent(Y, ?) */
        rule3.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule3.cvt2Uvs2NewLv(NUM_PARENT, 2, 0, 0, 0));
        assertEquals("grandParent(X1,X0):-father(?,X0),parent(X1,?)", rule3.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 0, 5 * 6 - 2, 2),
                rule3.getEval()
        );
        assertEquals(2, rule3.usedLimitedVars());
        assertEquals(2, rule3.length());
        assertEquals(6, fp_cache.size());
        assertEquals(0, tabu_map.size());
        EvidenceBatch actual_evidence = rule3.getEvidenceAndMarkEntailment();
        assertArrayEquals(new int[]{NUM_GRANDPARENT, NUM_FATHER, NUM_PARENT}, actual_evidence.relationsInRule);
        assertTrue(actual_evidence.evidenceList.isEmpty());
        final Set<Record> expected_counterexample_set = new HashSet<>();
        for (int arg1: new int[]{NUM_F1, NUM_F2, NUM_M2, NUM_G1, NUM_G2, NUM_G3}) {
            for (int arg2: new int[]{NUM_S1, NUM_S2, NUM_D2, NUM_S3, NUM_D4}) {
                expected_counterexample_set.add(new Record(new int[]{arg1, arg2}));
            }
        }
        expected_counterexample_set.remove(GRAND1);
        expected_counterexample_set.remove(GRAND2);
        assertEquals(expected_counterexample_set, rule3.getCounterexamples());

        /* #3: grandParent(Y, X) :- father(Z, X), parent(Y, Z) */
        rule3.updateCacheIndices();
        assertEquals(UpdateStatus.DUPLICATED, rule3.cvt2Uvs2NewLv(1, 0, 2, 1));
        assertEquals(6, fp_cache.size());
        assertEquals(0, tabu_map.size());
    }

    @Test
    void testFamilyWithCopy2() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* #1: parent(?, ?) :- */
        final CachedRule rule1 = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        assertEquals("parent(?,?):-", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 9, 16 * 16, 0),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedLimitedVars());
        assertEquals(0, rule1.length());
        assertEquals(1, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #1: parent(f2, ?) :- */
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt1Uv2Const(0, 0, NUM_F2));
        assertEquals("parent(f2,?):-", rule1.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 2, 16, 1),
                rule1.getEval()
        );
        assertEquals(0, rule1.usedLimitedVars());
        assertEquals(1, rule1.length());
        assertEquals(2, fp_cache.size());
        assertEquals(0, tabu_map.size());

        /* #2: parent(f2, d2) :- */
        final CachedRule rule2 = new CachedRule(rule1);
        rule2.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule2.cvt1Uv2Const(0, 1, NUM_D2));
        assertEquals("parent(f2,d2):-", rule2.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 1, 1, 2),
                rule2.getEval()
        );
        assertEquals(0, rule2.usedLimitedVars());
        assertEquals(2, rule2.length());
        assertEquals(3, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{PARENT4}));
        checkEvidence(rule2.getEvidenceAndMarkEntailment(), new int[]{NUM_PARENT}, new Set[]{expected_grounding_set2});
        assertTrue(rule2.getCounterexamples().isEmpty());

        /* #3: parent(f2, X) :- father(?, X) */
        final CachedRule rule3 = new CachedRule(rule1);
        assertEquals(UpdateStatus.NORMAL, rule3.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 1));
        assertEquals("parent(f2,X0):-father(?,X0)", rule3.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 1, 4, 2),
                rule3.getEval()
        );
        assertEquals(1, rule3.usedLimitedVars());
        assertEquals(2, rule3.length());
        assertEquals(4, fp_cache.size());
        assertEquals(0, tabu_map.size());
        final Set<ComparableArray<Record>> expected_grounding_set3 = new HashSet<>();
        expected_grounding_set3.add(new ComparableArray<>(new Record[]{PARENT3, FATHER2}));
        final Set<Record> expected_counterexample_set3 = new HashSet<>();
        for (int arg: new int[]{NUM_S1, NUM_S3, NUM_D4}) {
            expected_counterexample_set3.add(new Record(new int[]{NUM_F2, arg}));
        }
        checkEvidence(rule3.getEvidenceAndMarkEntailment(), new int[]{NUM_PARENT, NUM_FATHER}, new Set[]{expected_grounding_set3});
        assertEquals(expected_counterexample_set3, rule3.getCounterexamples());
    }

    void checkEvidence(EvidenceBatch actualEvidence, int[] expectedRelationsInRule, Set<ComparableArray<Record>>[] expectedGroundingSets) {
        assertArrayEquals(expectedRelationsInRule, actualEvidence.relationsInRule);
        Set<ComparableArray<Record>> actual_grounding_set = new HashSet<>();
        for (int[][] grounding: actualEvidence.evidenceList) {
            Record[] record_list = new Record[grounding.length];
            for (int i = 0; i < grounding.length; i++) {
                record_list[i] = new Record(grounding[i]);
            }
            actual_grounding_set.add(new ComparableArray<>(record_list));
        }
        boolean match_found = false;
        for (Set<ComparableArray<Record>> expected_grounding_set: expectedGroundingSets) {
            if (expected_grounding_set.equals(actual_grounding_set)) {
                match_found = true;
                break;
            }
        }
        assertTrue(match_found);
    }

    @Test
    void testValidity1() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* father(?,?):- */
        final CachedRule rule = new CachedRule(NUM_FATHER, 2, fp_cache, tabu_map, kb);
        assertEquals("father(?,?):-", rule.toDumpString(kb.getNumerationMap()));

        /* #1: father(X,?) :- father(?,X) */
        final CachedRule rule1 = new CachedRule(rule);
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 0));
        assertEquals("father(X0,?):-father(?,X0)", rule1.toDumpString(kb.getNumerationMap()));

        /* #1: father(X,Y) :- father(Y,X) */
        rule1.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule1.cvt2Uvs2NewLv(0, 1, 1, 0));
        assertEquals("father(X0,X1):-father(X1,X0)", rule1.toDumpString(kb.getNumerationMap()));

        /* #2: father(X,?) :- father(X,?) [invalid] */
        final CachedRule rule2 = new CachedRule(rule);
        rule2.updateCacheIndices();
        assertEquals(UpdateStatus.INVALID, rule2.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 0, 0));
    }

    @Test
    void testValidity2() throws KbException {
        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* father(?,?):- */
        final CachedRule rule = new CachedRule(NUM_FATHER, 2, fp_cache, tabu_map, kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 1, 0, 0));
        assertEquals("father(X0,?):-father(?,X0)", rule.toDumpString(kb.getNumerationMap()));

        /* father(X,?) :- father(?,X), father(?,X) */
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(NUM_FATHER, 2, 1, 0));
        assertEquals("father(X0,?):-father(?,X0),father(?,X0)", rule.toDumpString(kb.getNumerationMap()));

        /* father(X,?) :- father(Y,X), father(Y,X) [invalid] */
        assertEquals(UpdateStatus.INVALID, rule.cvt2Uvs2NewLv(1, 0, 2, 0));
    }

    @Test
    void testRcPruning1() throws KbException {
        Rule.MIN_FACT_COVERAGE = 0.44;

        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(X, ?) :- father(X, ?) */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 0, 0));
        assertEquals("parent(X0,?):-father(X0,?)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(
                new Eval(null, 4, 4 * 16, 1),
                rule.getEval()
        );
    }

    @Test
    void testRcPruning2() throws KbException {
        Rule.MIN_FACT_COVERAGE = 0.45;

        final NumeratedKb kb = kbFamily();
        final Set<Fingerprint> fp_cache = new HashSet<>();
        final Map<MultiSet<Integer>, Set<Fingerprint>> tabu_map = new HashMap<>();

        /* parent(X, ?) :- father(X, ?) */
        final CachedRule rule = new CachedRule(NUM_PARENT, 2, fp_cache, tabu_map, kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.INSUFFICIENT_COVERAGE, rule.cvt2Uvs2NewLv(NUM_FATHER, 2, 0, 0, 0));
        assertEquals(4.0 / 9.0, rule.recordCoverage());
        assertEquals(1, tabu_map.size());
        assertTrue(tabu_map.get(new MultiSet<>(new Integer[]{NUM_FATHER})).contains(rule.getFingerprint()));
    }

    @Test
    void testAnyRule1() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        int p = kb.createRelation("p", 3).getNumeration();
        int h = kb.createRelation("h", 4).getNumeration();
        int a = kb.mapName("a");
        int A = kb.mapName("A");
        int plus = kb.mapName("+");
        int b = kb.mapName("b");
        int B = kb.mapName("B");
        int c = kb.mapName("c");
        int C = kb.mapName("C");
        int minus = kb.mapName("-");
        Record p1 = new Record(new int[]{a, A, plus});
        Record p2 = new Record(new int[]{b, B, plus});
        Record p3 = new Record(new int[]{c, C, minus});
        Record h1 = new Record(new int[]{a, a, A, A});
        Record h2 = new Record(new int[]{b, b, B, B});
        kb.addRecords(p, new Record[]{p1, p2, p3});
        kb.addRecords(h, new Record[]{h1, h2});

        /* h(X, X, Y, Y) :- p(X, Y, +) */
        CachedRule rule = new CachedRule(h, 4, new HashSet<>(), new HashMap<>(), kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(p, 3, 0, 0, 0));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 2, 1, 1));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(0, 1, 0));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(0, 3, 1));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2Const(1, 2, plus));
        assertEquals("h(X0,X0,X1,X1):-p(X0,X1,+)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 2, 2, 5), rule.getEval());
        assertEquals(2, rule.usedLimitedVars());
        assertEquals(5, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h1, p1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h2, p2}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h, p}, new Set[]{expected_grounding_set});
        assertTrue(rule.getCounterexamples().isEmpty());
    }

    @Test
    void testAnyRule2() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        int h = kb.createRelation("h", 1).getNumeration();
        int p = kb.createRelation("p", 2).getNumeration();
        int q = kb.createRelation("q", 1).getNumeration();
        int a = kb.mapName("a");
        int A = kb.mapName("A");
        int b = kb.mapName("b");
        Record p1 = new Record(new int[]{A, a});
        Record p2 = new Record(new int[]{a, a});
        Record p3 = new Record(new int[]{A, A});
        Record p4 = new Record(new int[]{b, a});
        Record q1 = new Record(new int[]{b});
        Record q2 = new Record(new int[]{a});
        Record h1 = new Record(new int[]{a});
        kb.addRecords(p, new Record[]{p1, p2, p3, p4});
        kb.addRecords(q, new Record[]{q1, q2});
        kb.addRecords(h, new Record[]{h1});

        /* h(X) :- p(X, X), q(X) */
        CachedRule rule = new CachedRule(h, 1, new HashSet<>(), new HashMap<>(), kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(p, 2, 0, 0, 0));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(q, 1, 0, 0));
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt1Uv2ExtLv(1, 1, 0));
        assertEquals("h(X0):-p(X0,X0),q(X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 1, 1, 3), rule.getEval());
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(3, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h1, p2, q2}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h, p, q}, new Set[]{expected_grounding_set});
        assertTrue(rule.getCounterexamples().isEmpty());
    }

    @Test
    void testAnyRule3() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        int h = kb.createRelation("h", 2).getNumeration();
        int a = kb.mapName("a");
        int b = kb.mapName("b");
        int c = kb.mapName("c");
        Record h1 = new Record(new int[]{a, a});
        Record h2 = new Record(new int[]{b, b});
        Record h3 = new Record(new int[]{a, c});
        kb.addRecords(h, new Record[]{h1, h2, h3});

        /* h(X, X) :- */
        CachedRule rule = new CachedRule(h, 2, new HashSet<>(), new HashMap<>(), kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 0, 0, 1));
        assertEquals("h(X0,X0):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 2, 3, 1), rule.getEval());
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h2}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h}, new Set[]{expected_grounding_set});
        assertEquals(new HashSet<>(List.of(new Record(new int[]{c, c}))), rule.getCounterexamples());

        assertTrue(kb.recordIsEntailed(h, h1.args));
        assertTrue(kb.recordIsEntailed(h, h2.args));
        assertFalse(kb.recordIsEntailed(h, h3.args));
    }

    @Test
    void testAnyRule4() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        int h = kb.createRelation("h", 3).getNumeration();
        int a = kb.mapName("a");
        int b = kb.mapName("b");
        Record h1 = new Record(new int[]{a, a, b});
        Record h2 = new Record(new int[]{b, b, a});
        Record h3 = new Record(new int[]{a, b, b});
        kb.addRecords(h, new Record[]{h1, h2, h3});

        /* h(X, X, ?) :- */
        CachedRule rule = new CachedRule(h, 3, new HashSet<>(), new HashMap<>(), kb);
        rule.updateCacheIndices();
        assertEquals(UpdateStatus.NORMAL, rule.cvt2Uvs2NewLv(0, 0, 0, 1));
        assertEquals("h(X0,X0,?):-", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 2, 4, 1), rule.getEval());
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(1, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h1}));
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h2}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h}, new Set[]{expected_grounding_set});
        assertEquals(new HashSet<>(List.of(new Record(new int[]{a, a, a}), new Record(new int[]{b, b, b}))), rule.getCounterexamples());

        assertTrue(kb.recordIsEntailed(h, h1.args));
        assertTrue(kb.recordIsEntailed(h, h2.args));
        assertFalse(kb.recordIsEntailed(h, h3.args));
    }

    @Test
    void testStructureConstructor1() throws KbException {
        /* h(X, Y, X, ?, a, ?) :- p(Y, Z, Z) */
        NumeratedKb kb = new NumeratedKb("test");
        int h = kb.createRelation("h", 6).getNumeration();
        int p = kb.createRelation("p", 3).getNumeration();
        int a = kb.mapName("a");
        int b = kb.mapName("b");
        int c = kb.mapName("c");
        int d = kb.mapName("d");
        int e = kb.mapName("e");
        Record h1 = new Record(new int[]{a, a, b, a, a, a});
        Record h2 = new Record(new int[]{b, b, b, c, a, e});//
        Record h3 = new Record(new int[]{c, d, c, c, a, d});//
        Record h4 = new Record(new int[]{d, d, d, d, d, d});
        Record h5 = new Record(new int[]{e, a, a, d, d, d});
        kb.addRecords(h, new Record[]{h1, h2, h3, h4, h5});
        Record p1 = new Record(new int[]{a, b, b});
        Record p2 = new Record(new int[]{a, c, d});
        Record p3 = new Record(new int[]{c, c, c});
        Record p4 = new Record(new int[]{b, d, d});//
        Record p5 = new Record(new int[]{e, a, a});
        Record p6 = new Record(new int[]{e, b, c});
        kb.addRecords(p, new Record[]{p1, p2, p3, p4, p5, p6});

        /* Construct */
        CachedRule rule = new CachedRule(new ArrayList<>(List.of(
                new Predicate(h, new int[]{
                        Argument.variable(0), Argument.variable(1), Argument.variable(0),
                        Argument.EMPTY_VALUE, Argument.constant(a), Argument.EMPTY_VALUE
                }),
                new Predicate(p, new int[]{
                        Argument.variable(1), Argument.variable(2), Argument.variable(2)
                }))),
                new HashSet<>(), new HashMap<>(), kb
        );
        assertEquals("h(X0,X1,X0,?,a,?):-p(X1,X2,X2)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 1, 4 * 5 * 5 * 5, 4), rule.getEval());
        assertEquals(3, rule.usedLimitedVars());
        assertEquals(4, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set = new HashSet<>();
        expected_grounding_set.add(new ComparableArray<>(new Record[]{h2, p4}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h, p}, new Set[]{expected_grounding_set});
        Set<Record> expected_counter_example_set = new HashSet<>();
        for (int arg1: kb.getAllConstants()) {
            for (int arg2: new int[]{a, b, c, e}) {
                for (int arg4: kb.getAllConstants()) {
                    for (int arg6: kb.getAllConstants()) {
                        expected_counter_example_set.add(new Record(new int[]{arg1, arg2, arg1, arg4, a, arg6}));
                    }
                }
            }
        }
        expected_counter_example_set.remove(h2);
        assertEquals(expected_counter_example_set, rule.getCounterexamples());
        assertTrue(kb.recordIsEntailed(h, h2.args));
    }

    @Test
    void testStructureConstructor2() throws KbException {
        /* h(?, X, X, ?) :- p(a, X, ?), q(X, ?, X), s(a), r(X) */
        NumeratedKb kb = new NumeratedKb("test");
        int h = kb.createRelation("h", 4).getNumeration();
        int p = kb.createRelation("p", 3).getNumeration();
        int q = kb.createRelation("q", 3).getNumeration();
        int r = kb.createRelation("r", 1).getNumeration();
        int s = kb.createRelation("s", 1).getNumeration();
        int a = kb.mapName("a");
        int b = kb.mapName("b");
        int c = kb.mapName("c");
        int d = kb.mapName("d");
        int e = kb.mapName("e");
        Record h1 = new Record(new int[]{a, b, c, d});
        Record h2 = new Record(new int[]{a, c, c, d});//c
        Record h3 = new Record(new int[]{d, e, e, a});//e
        Record h4 = new Record(new int[]{b, b, b, b});//b
        kb.addRecords(h, new Record[]{h1, h2, h3, h4});
        Record p1 = new Record(new int[]{a, b, b});//b
        Record p2 = new Record(new int[]{a, c, d});//c
        Record p3 = new Record(new int[]{c, c, c});
        Record p4 = new Record(new int[]{b, d, d});
        Record p5 = new Record(new int[]{e, a, a});
        Record p6 = new Record(new int[]{a, b, c});//b
        kb.addRecords(p, new Record[]{p1, p2, p3, p4, p5, p6});
        Record q1 = new Record(new int[]{b, a, b});//b
        Record q2 = new Record(new int[]{b, c, d});//b
        Record q3 = new Record(new int[]{c, e, c});//c
        Record q4 = new Record(new int[]{a, b, c});
        Record q5 = new Record(new int[]{c, d, e});
        kb.addRecords(q, new Record[]{q1, q2, q3, q4, q5});
        Record r1 = new Record(new int[]{a});
        Record r2 = new Record(new int[]{b});
        Record r3 = new Record(new int[]{d});
        kb.addRecords(r, new Record[]{r1, r2, r3});
        Record s1 = new Record(new int[]{a});
        Record s2 = new Record(new int[]{b});
        kb.addRecords(s, new Record[]{s1, s2});

        /* Construct */
        CachedRule rule = new CachedRule(new ArrayList<>(List.of(
                new Predicate(h, new int[]{
                        Argument.EMPTY_VALUE, Argument.variable(0), Argument.variable(0), Argument.EMPTY_VALUE
                }),
                new Predicate(p, new int[]{
                        Argument.constant(a), Argument.variable(0), Argument.EMPTY_VALUE
                }),
                new Predicate(q, new int[]{
                        Argument.variable(0), Argument.EMPTY_VALUE, Argument.variable(0)
                }),
                new Predicate(s, new int[]{Argument.constant(a)}),
                new Predicate(r, new int[]{Argument.variable(0)}))),
                new HashSet<>(), new HashMap<>(), kb
        );
        assertEquals("h(?,X0,X0,?):-p(a,X0,?),q(X0,?,X0),s(a),r(X0)", rule.toDumpString(kb.getNumerationMap()));
        assertEquals(new Eval(null, 1, 1 * 5 * 5, 7), rule.getEval());
        assertEquals(1, rule.usedLimitedVars());
        assertEquals(7, rule.length());
        final Set<ComparableArray<Record>> expected_grounding_set1 = new HashSet<>();
        expected_grounding_set1.add(new ComparableArray<>(new Record[]{h4, p1, q1, s1, r2}));
        final Set<ComparableArray<Record>> expected_grounding_set2 = new HashSet<>();
        expected_grounding_set2.add(new ComparableArray<>(new Record[]{h4, p1, q2, s1, r2}));
        checkEvidence(rule.getEvidenceAndMarkEntailment(), new int[]{h, p, q, s, r}, new Set[]{expected_grounding_set1, expected_grounding_set2});
        Set<Record> expected_counter_example_set = new HashSet<>();
        for (int arg1: kb.getAllConstants()) {
            for (int arg2: new int[]{b}) {
                for (int arg4: kb.getAllConstants()) {
                    expected_counter_example_set.add(new Record(new int[]{arg1, arg2, arg2, arg4}));
                }
            }
        }
        expected_counter_example_set.remove(h4);
        assertEquals(expected_counter_example_set, rule.getCounterexamples());
        assertTrue(kb.recordIsEntailed(h, h4.args));
    }
}