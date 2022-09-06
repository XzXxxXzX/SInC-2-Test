package sinc.impl.pruned.observed;

import org.junit.jupiter.api.Test;
import sinc.SincConfig;
import sinc.common.*;
import sinc.util.datagen.FamilyRelationGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SincWithFingerprintObservationTest {

    static final int CONSTANT_ID = Rule.CONSTANT_ARG_ID;

    @Test
    void testTinyHypothesis() {
        /*
         * Hypothesis:
         *      gender(X, male) <- father(X, ?)
         *      gender(X, female) <- mother(X, ?)
         */
        UUID id = UUID.randomUUID();
        final String tmp_bk_file_path = id + "_bk";
        checkFile(tmp_bk_file_path);

        try {
            FamilyRelationGenerator.generateTiny(tmp_bk_file_path, 10, 0);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        for (Eval.EvalMetric eval_type: new Eval.EvalMetric[]{
                Eval.EvalMetric.CompressionCapacity,
                Eval.EvalMetric.CompressionRate
        }) {
            final SincConfig config = new SincConfig(
                    1,
                    false,
                    false,
                    5,
                    true,
                    eval_type,
                    0.05,
                    0.25,
                    0,
                    1,
                    false,
                    -1.0,
                    false,
                    false
            );

            SincWithFingerprintObservation sinc = new SincWithFingerprintObservation(
                    config,
                    tmp_bk_file_path,
                    null,
                    "tmp.log"
            );
            sinc.run();
            assertTrue(sinc.recover());

            try {
                Set<RuleFingerPrint> rule_set_sinc = new HashSet<>();
                for (Rule r: sinc.getHypothesis()) {
                    rule_set_sinc.add(r.getFingerPrint());
                }
                final Predicate head1 = new Predicate(
                        FamilyRelationGenerator.OtherPredicate.GENDER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head1.args[0] = new Variable(0);
                head1.args[1] = new Constant(
                        CONSTANT_ID,
                        FamilyRelationGenerator.Gender.MALE.getName()
                );
                final Predicate body1 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.FATHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body1.args[0] = new Variable(0);
                List<Predicate> r1 = new ArrayList<>(Arrays.asList(head1, body1));
                assertEquals("gender(X0,male):-father(X0,?)", rule2String(r1));

                final Predicate head2 = new Predicate(
                        FamilyRelationGenerator.OtherPredicate.GENDER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head2.args[0] = new Variable(0);
                head2.args[1] = new Constant(
                        CONSTANT_ID,
                        FamilyRelationGenerator.Gender.FEMALE.getName()
                );
                final Predicate body2 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.MOTHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body2.args[0] = new Variable(0);
                List<Predicate> r2 = new ArrayList<>(Arrays.asList(head2, body2));
                assertEquals("gender(X0,female):-mother(X0,?)", rule2String(r2));

                final Set<RuleFingerPrint> expected_rules = new HashSet<>();
                expected_rules.add(new RuleFingerPrint(r1));
                expected_rules.add(new RuleFingerPrint(r2));
                assertEquals(expected_rules, rule_set_sinc);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        deleteFile(tmp_bk_file_path);
    }

    @Test
    void testSimpleHypothesis() {
        /*
         * Hypothesis:
         *      gender(X,male):-father(X,?).
         *      gender(X,female):-mother(X,?).
         *      parent(X,Y):-father(X,Y).
         *      parent(X,Y):-mother(X,Y).
         */
        UUID id = UUID.randomUUID();
        final String tmp_bk_file_path = id + "_bk";
        checkFile(tmp_bk_file_path);

        try {
            FamilyRelationGenerator.generateSimple(tmp_bk_file_path, 10, 0);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        for (Eval.EvalMetric eval_type: new Eval.EvalMetric[]{
                Eval.EvalMetric.CompressionCapacity,
                Eval.EvalMetric.CompressionRate
        }) {
            final SincConfig config = new SincConfig(
                    1,
                    false,
                    false,
                    5,
                    true,
                    eval_type,
                    0.05,
                    0.25,
                    0,
                    1,
                    false,
                    -1.0,
                    false,
                    false
            );

            SincWithFingerprintObservation sinc = new SincWithFingerprintObservation(
                    config,
                    tmp_bk_file_path,
                    null,
                    null
            );
            sinc.run();
            assertTrue(sinc.recover());

            try {
                Set<RuleFingerPrint> rule_set_sinc = new HashSet<>();
                for (Rule r: sinc.getHypothesis()) {
                    rule_set_sinc.add(r.getFingerPrint());
                }
                final Predicate head1 = new Predicate(
                        FamilyRelationGenerator.OtherPredicate.GENDER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head1.args[0] = new Variable(0);
                head1.args[1] = new Constant(
                        CONSTANT_ID,
                        FamilyRelationGenerator.Gender.MALE.getName()
                );
                final Predicate body1 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.FATHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body1.args[0] = new Variable(0);
                List<Predicate> r1 = new ArrayList<>(Arrays.asList(head1, body1));
                assertEquals("gender(X0,male):-father(X0,?)", rule2String(r1));

                final Predicate head2 = new Predicate(
                        FamilyRelationGenerator.OtherPredicate.GENDER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head2.args[0] = new Variable(0);
                head2.args[1] = new Constant(
                        CONSTANT_ID,
                        FamilyRelationGenerator.Gender.FEMALE.getName()
                );
                final Predicate body2 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.MOTHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body2.args[0] = new Variable(0);
                List<Predicate> r2 = new ArrayList<>(Arrays.asList(head2, body2));
                assertEquals("gender(X0,female):-mother(X0,?)", rule2String(r2));

                final Predicate head3 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.PARENT.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head3.args[0] = new Variable(0);
                head3.args[1] = new Variable(1);
                final Predicate body3 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.FATHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body3.args[0] = new Variable(0);
                body3.args[1] = new Variable(1);
                List<Predicate> r3 = new ArrayList<>(Arrays.asList(head3, body3));
                assertEquals("parent(X0,X1):-father(X0,X1)", rule2String(r3));

                final Predicate head4 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.PARENT.getName(),
                        FamilyRelationGenerator.ARITY
                );
                head4.args[0] = new Variable(0);
                head4.args[1] = new Variable(1);
                final Predicate body4 = new Predicate(
                        FamilyRelationGenerator.FamilyPredicate.MOTHER.getName(),
                        FamilyRelationGenerator.ARITY
                );
                body4.args[0] = new Variable(0);
                body4.args[1] = new Variable(1);
                List<Predicate> r4 = new ArrayList<>(Arrays.asList(head4, body4));
                assertEquals("parent(X0,X1):-mother(X0,X1)", rule2String(r4));

                final Set<RuleFingerPrint> expected_rules = new HashSet<>();
                expected_rules.add(new RuleFingerPrint(r1));
                expected_rules.add(new RuleFingerPrint(r2));
                expected_rules.add(new RuleFingerPrint(r3));
                expected_rules.add(new RuleFingerPrint(r4));
                assertEquals(expected_rules, rule_set_sinc);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        deleteFile(tmp_bk_file_path);
    }

    private void checkFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            System.err.println("Temporary File Existed: " + filePath);
            fail();
        }
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        file.delete();
    }

    private String rule2String(List<Predicate> rule) {
        StringBuilder builder = new StringBuilder();
        builder.append(rule.get(0).toString()).append(":-");
        if (1 < rule.size()) {
            builder.append(rule.get(1));
            for (int i = 2; i < rule.size(); i++) {
                builder.append(',');
                builder.append(rule.get(i).toString());
            }
        }
        return builder.toString();
    }

}