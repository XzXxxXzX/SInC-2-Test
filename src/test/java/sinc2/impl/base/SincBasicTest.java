package sinc2.impl.base;

import org.junit.jupiter.api.Test;
import sinc2.SincConfig;
import sinc2.common.Argument;
import sinc2.common.Predicate;
import sinc2.kb.CompressedKb;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;
import sinc2.rule.BareRule;
import sinc2.rule.EvalMetric;
import sinc2.rule.Fingerprint;
import sinc2.rule.Rule;
import sinc2.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SincBasicTest {

    static final String TMP_DIR = "/dev/shm/";

    @Test
    void testTinyHypothesis() throws KbException, IOException {
        /*
         * Hypothesis:
         *      gender(X, male) <- father(X, ?)
         *      gender(X, female) <- mother(X, ?)
         */
        String kb_name = "family.tiny." + UUID.randomUUID();
//        String compressed_kb_name = kb_name + ".comp";
        FamilyRelationGenerator.generateTiny(TMP_DIR, kb_name, 10, 0);
        NumeratedKb kb = new NumeratedKb(kb_name, TMP_DIR);

        final Predicate head1 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.OtherRelation.GENDER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.constant(kb.name2Num(FamilyRelationGenerator.Gender.MALE.getName()))
                }
        );
        final Predicate body1 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.FATHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.EMPTY_VALUE
                }
        );
        BareRule r1 = new BareRule(new ArrayList<>(Arrays.asList(head1, body1)), new HashSet<>(), new HashMap<>());
        assertEquals("gender(X0,male):-father(X0,?)", r1.toDumpString(kb.getNumerationMap()));

        final Predicate head2 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.OtherRelation.GENDER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.constant(kb.name2Num(FamilyRelationGenerator.Gender.FEMALE.getName()))
                }
        );
        final Predicate body2 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.MOTHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.EMPTY_VALUE
                }
        );
        BareRule r2 = new BareRule(new ArrayList<>(Arrays.asList(head2, body2)), new HashSet<>(), new HashMap<>());
        assertEquals("gender(X0,female):-mother(X0,?)", r2.toDumpString(kb.getNumerationMap()));

        final Set<Fingerprint> expected_rules = new HashSet<>();
        expected_rules.add(r1.getFingerprint());
        expected_rules.add(r2.getFingerprint());

        for (EvalMetric eval_type: new EvalMetric[]{
                EvalMetric.CompressionCapacity,
                EvalMetric.CompressionRatio
        }) {
            String compressed_kb_name = "family.tiny.comp." + UUID.randomUUID();
            System.out.println("CompKB: " + compressed_kb_name);
            final SincConfig config = new SincConfig(
                    TMP_DIR, kb_name, TMP_DIR, compressed_kb_name, 1, true, 5,
                    eval_type, 0.05, 0.25, 1
            );
            Set<Fingerprint> rule_set_sinc = new HashSet<>();
            SincBasic sinc = new SincBasic(config);
            sinc.run();
            CompressedKb compressed_kb = sinc.getCompressedKb();
            for (Rule r: compressed_kb.getHypothesis()) {
                rule_set_sinc.add(r.getFingerprint());
            }

//            assertTrue(sinc.recover()); TODO: Uncomment here
            assertEquals(expected_rules, rule_set_sinc);
            deleteDir(Paths.get(TMP_DIR, compressed_kb_name).toFile());
        }
        deleteDir(Paths.get(TMP_DIR, kb_name).toFile());
    }

    @Test
    void testSimpleHypothesis() throws KbException, IOException {
        /*
         * Hypothesis:
         *      gender(X,male):-father(X,?).
         *      gender(X,female):-mother(X,?).
         *      parent(X,Y):-father(X,Y).
         *      parent(X,Y):-mother(X,Y).
         */
        String kb_name = "family.simple." + UUID.randomUUID();
        String compressed_kb_name = kb_name + ".comp";
        FamilyRelationGenerator.generateSimple(TMP_DIR, kb_name, 10, 0);
        NumeratedKb kb = new NumeratedKb(kb_name, TMP_DIR);

        final Predicate head1 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.OtherRelation.GENDER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.constant(kb.name2Num(FamilyRelationGenerator.Gender.MALE.getName()))
                }
        );
        final Predicate body1 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.FATHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.EMPTY_VALUE
                }
        );
        BareRule r1 = new BareRule(new ArrayList<>(Arrays.asList(head1, body1)), new HashSet<>(), new HashMap<>());
        assertEquals("gender(X0,male):-father(X0,?)", r1.toDumpString(kb.getNumerationMap()));

        final Predicate head2 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.OtherRelation.GENDER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.constant(kb.name2Num(FamilyRelationGenerator.Gender.FEMALE.getName()))
                }
        );
        final Predicate body2 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.MOTHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.EMPTY_VALUE
                }
        );
        BareRule r2 = new BareRule(new ArrayList<>(Arrays.asList(head2, body2)), new HashSet<>(), new HashMap<>());
        assertEquals("gender(X0,female):-mother(X0,?)", r2.toDumpString(kb.getNumerationMap()));

        final Predicate head3 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.PARENT.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.variable(1)
                }
        );
        final Predicate body3 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.FATHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.variable(1)
                }
        );
        BareRule r3 = new BareRule(new ArrayList<>(Arrays.asList(head3, body3)), new HashSet<>(), new HashMap<>());
        assertEquals("parent(X0,X1):-father(X0,X1)", r3.toDumpString(kb.getNumerationMap()));

        final Predicate head4 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.PARENT.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.variable(1)
                }
        );
        final Predicate body4 = new Predicate(
                kb.name2Num(FamilyRelationGenerator.FamilyRelations.MOTHER.getName()),
                new int[]{
                        Argument.variable(0),
                        Argument.variable(1)
                }
        );
        BareRule r4 = new BareRule(new ArrayList<>(Arrays.asList(head4, body4)), new HashSet<>(), new HashMap<>());
        assertEquals("parent(X0,X1):-mother(X0,X1)", r4.toDumpString(kb.getNumerationMap()));

        final Set<Fingerprint> expected_rules = new HashSet<>();
        expected_rules.add(r1.getFingerprint());
        expected_rules.add(r2.getFingerprint());
        expected_rules.add(r3.getFingerprint());
        expected_rules.add(r4.getFingerprint());

        for (EvalMetric eval_type: new EvalMetric[]{
                EvalMetric.CompressionCapacity,
                EvalMetric.CompressionRatio
        }) {
            final SincConfig config = new SincConfig(
                    TMP_DIR, kb_name, TMP_DIR, compressed_kb_name, 1, true, 5,
                    eval_type, 0.05, 0.25, 1
            );
            Set<Fingerprint> rule_set_sinc = new HashSet<>();
            SincBasic sinc = new SincBasic(config);
            sinc.run();
            CompressedKb compressed_kb = sinc.getCompressedKb();
            for (Rule r: compressed_kb.getHypothesis()) {
                rule_set_sinc.add(r.getFingerprint());
            }

//            assertTrue(sinc.recover()); TODO: Uncomment here
            assertEquals(expected_rules, rule_set_sinc);
            deleteDir(Paths.get(TMP_DIR, compressed_kb_name).toFile());
        }
        deleteDir(Paths.get(TMP_DIR, kb_name).toFile());
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
}