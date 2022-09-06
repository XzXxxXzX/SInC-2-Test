package sinc.util.datagen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FamilyRelationGenerator {
    public static final int ARITY = 2;

    public enum FamilyMember {
        GRAND_FATHER("gf"), GRAND_MOTHER("gm"),
        W_GRAND_FATHER("wgf"), W_GRAND_MOTHER("wgm"),
        FATHER("f"), UNCLE_A("ua"), AUNT_A("aa"),
        MOTHER("m"), AUNT_B("ab"), UNCLE_B("ub"),
        SON("s"), DAUGHTER("d"), BROTHER("b"), SISTER("sis");

        private final String shortName;

        FamilyMember(String shortName) {
            this.shortName = shortName;
        }

        public String getShortName() {
            return shortName;
        }
    }

    public enum FamilyPredicate {
        FATHER("father"), MOTHER("mother"), PARENT("parent"),
        BROTHER("brother"), SISTER("sister"), SIBLING("sibling"),
        UNCLE("uncle"), AUNT("aunt");

        private final String name;

        FamilyPredicate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum OtherPredicate {
        GENDER("gender");

        private final String name;

        OtherPredicate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Gender {
        MALE("male"), FEMALE("female");

        private final String name;

        Gender(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class Triple {}

    static class FamilyTriple extends Triple {
        FamilyMember subject;
        FamilyPredicate familyPredicate;
        FamilyMember object;

        public FamilyTriple(FamilyMember subject, FamilyPredicate familyPredicate, FamilyMember object) {
            this.subject = subject;
            this.familyPredicate = familyPredicate;
            this.object = object;
        }
    }

    static class GenderTripe extends Triple {
        FamilyMember subject;
        Gender gender;

        public GenderTripe(FamilyMember subject, Gender gender) {
            this.subject = subject;
            this.gender = gender;
        }
    }

    private static Random random = new Random();

    public static void generateTiny(String path, int families, double errorProbability) throws IOException {
        Random random = new Random();
        PrintWriter writer = new PrintWriter(path);
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyPredicate[] familyPredicateValues = FamilyPredicate.values();

        for (int i = 0; i < families; i++) {
            List<Triple> triples = new ArrayList<>();

            /* father/mother */
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.DAUGHTER));

            /* gender */
            triples.add(new GenderTripe(FamilyMember.FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.SON, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.DAUGHTER, Gender.FEMALE));

            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorProbability > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorProbability / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyPredicate =
                                    familyPredicateValues[random.nextInt(familyPredicateValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, triple, i, i);
                }
            }
        }
        writer.close();
    }

    public static void generateSimple(String path, int families, double errorProbability) throws IOException {
        Random random = new Random();
        PrintWriter writer = new PrintWriter(path);
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyPredicate[] familyPredicateValues = FamilyPredicate.values();
        for (int i = 0; i < families; i++) {
            List<Triple> triples = new ArrayList<>();

            /* father/mother */
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.DAUGHTER));

            /* parent */
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.PARENT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.PARENT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.PARENT, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.PARENT, FamilyMember.DAUGHTER));

            /* gender */
            triples.add(new GenderTripe(FamilyMember.GRAND_FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.GRAND_MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.W_GRAND_FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.W_GRAND_MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.SON, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.DAUGHTER, Gender.FEMALE));

            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorProbability > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorProbability / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyPredicate =
                                    familyPredicateValues[random.nextInt(familyPredicateValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, triple, i, i);
                }
            }
        }
        writer.close();
    }

    public static void generateMedium(String path, int families, double errorProbability) throws IOException {
        Random random = new Random();
        PrintWriter writer = new PrintWriter(path);
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyPredicate[] familyPredicateValues = FamilyPredicate.values();
        for (int i = 0; i < families; i++) {
            List<Triple> triples = new ArrayList<>();

            /* father/mother */
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.FATHER, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.MOTHER, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.FATHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.MOTHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.MOTHER, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.MOTHER, FamilyMember.SISTER));

            /* parent */
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyPredicate.PARENT, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyPredicate.PARENT, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.PARENT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.PARENT, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.PARENT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.PARENT, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.PARENT, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.PARENT, FamilyMember.SISTER));

            /* brother/sister */
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.BROTHER, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.BROTHER, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.BROTHER, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.BROTHER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.SISTER, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.SISTER, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.SISTER, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.SISTER, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.SISTER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.SISTER, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.BROTHER, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.BROTHER, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.BROTHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.BROTHER, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.BROTHER, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SISTER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SISTER, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SISTER, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.BROTHER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.BROTHER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.BROTHER, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SISTER, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SISTER, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SISTER, FamilyMember.BROTHER));

            /* sibling */
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.SIBLING, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyPredicate.SIBLING, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.SIBLING, FamilyMember.AUNT_A));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.SIBLING, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.SIBLING, FamilyMember.UNCLE_A));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.SIBLING, FamilyMember.FATHER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.SIBLING, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.SIBLING, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.SIBLING, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.SIBLING, FamilyMember.UNCLE_B));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.SIBLING, FamilyMember.MOTHER));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.SIBLING, FamilyMember.AUNT_B));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.SIBLING, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.SIBLING, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.SON, FamilyPredicate.SIBLING, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SIBLING, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SIBLING, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyPredicate.SIBLING, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.SIBLING, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.SIBLING, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyPredicate.SIBLING, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SIBLING, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SIBLING, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyPredicate.SIBLING, FamilyMember.BROTHER));

            /* uncle/aunt */
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.UNCLE, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyPredicate.UNCLE, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.AUNT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyPredicate.AUNT, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.UNCLE, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.UNCLE, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.AUNT, FamilyMember.SON));
            triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyPredicate.AUNT, FamilyMember.DAUGHTER));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.UNCLE, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyPredicate.UNCLE, FamilyMember.SISTER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.AUNT, FamilyMember.BROTHER));
            triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyPredicate.AUNT, FamilyMember.SISTER));

            /* gender */
            triples.add(new GenderTripe(FamilyMember.GRAND_FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.GRAND_MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.W_GRAND_FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.W_GRAND_MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.FATHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.UNCLE_A, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.AUNT_A, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.MOTHER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.AUNT_B, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.UNCLE_B, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.SON, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.DAUGHTER, Gender.FEMALE));
            triples.add(new GenderTripe(FamilyMember.BROTHER, Gender.MALE));
            triples.add(new GenderTripe(FamilyMember.SISTER, Gender.FEMALE));

            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorProbability > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorProbability / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyPredicate =
                                    familyPredicateValues[random.nextInt(familyPredicateValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, triple, i, i);
                }
            }
        }
        writer.close();
    }

    private static void writeRelation(PrintWriter writer, Triple triple, int subjId, int objId) {
        if (triple instanceof FamilyTriple) {
            FamilyTriple triple_family = (FamilyTriple)triple;
            writer.printf(
                    "%s\t%s_%d\t%s_%d\n", triple_family.familyPredicate.name, triple_family.subject.shortName, subjId,
                    triple_family.object.shortName, objId
            );
        } else if (triple instanceof GenderTripe) {
            GenderTripe triple_gender = (GenderTripe)triple;
            writer.printf(
                    "%s\t%s_%d\t%s\n", OtherPredicate.GENDER.name, triple_gender.subject.shortName, subjId,
                    triple_gender.gender.name
            );
        }
    }

    public static void main(String[] args) throws IOException {
//        generateTiny("testData/familyRelation/FamilyRelationTiny(10x)(0.0)", 10, 0.0);
//        generateSimple("testData/familyRelation/FamilyRelationSimple(10x)(0.0)", 10, 0.0);
//        generateMedium("testData/familyRelation/FamilyRelationMedium(10x)(0.0)", 10, 0.0);
        for (int i: new int[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20}) {
            generateMedium(String.format("Fm_%d.tsv", i), i, 0.0);
        }
    }
}
