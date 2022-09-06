package sinc2.util.datagen;

import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The generator of family data.
 *
 * The stand-alone usage is: java -jar <the name of the jar> [t|s|m] <basePath> <families> <error rate>
 *
 * @since 1.0
 */
public class FamilyRelationGenerator {

    public static void main(String[] args) throws IOException, KbException {
        if (4 != args.length) {
            System.out.println("Usage: [t|s|m] <basePath> <families> <error rate>");
            return;
        }
        final String mode = args[0];
        final String base_path = args[1];
        final int families = Integer.parseInt(args[2]);
        final double error_rate = Double.parseDouble(args[3]);
        switch (mode) {
            case "t":
                generateTiny(base_path, "family.tiny", families, error_rate);
                break;
            case "s":
                generateSimple(base_path, "family.simple", families, error_rate);
                break;
            case "m":
                generateMedium(base_path, "family.medium", families, error_rate);
                break;
            default:
                throw new KbException("Unknown mode: " + mode);
        }
    }

    /** Family members */
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

    /** Relations between family members */
    public enum FamilyRelations {
        FATHER("father"), MOTHER("mother"), PARENT("parent"),
        BROTHER("brother"), SISTER("sister"), SIBLING("sibling"),
        UNCLE("uncle"), AUNT("aunt");

        private final String name;

        FamilyRelations(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /** Other relations */
    public enum OtherRelation {
        GENDER("gender");

        private final String name;

        OtherRelation(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /** Gender values */
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

    /** The base class for a triple */
    static class Triple {}

    /** The family relation triple */
    static class FamilyTriple extends Triple {
        FamilyMember subject;
        FamilyRelations familyRelations;
        FamilyMember object;

        public FamilyTriple(FamilyMember subject, FamilyRelations familyRelations, FamilyMember object) {
            this.subject = subject;
            this.familyRelations = familyRelations;
            this.object = object;
        }
    }

    /** The gender value tripe */
    static class GenderTripe extends Triple {
        FamilyMember subject;
        Gender gender;

        public GenderTripe(FamilyMember subject, Gender gender) {
            this.subject = subject;
            this.gender = gender;
        }
    }

    /**
     * Generator of tiny datasets, which are composed of 3 relations: father, mother, gender.
     *
     * @param basePath The base directory where the KB is located
     * @param kbName The name of the KB
     * @param families The number of families in the KB
     * @param errorRate The probability that errors occur in the KB
     * @throws IOException File I/O errors
     * @throws KbException KB operation errors
     */
    public static void generateTiny(String basePath, String kbName, int families, double errorRate) throws IOException, KbException {
        Random random = new Random();
        /* Create Triples */
        List<Triple> triples = new ArrayList<>();

        /* father/mother */
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.DAUGHTER));

        /* gender */
        triples.add(new GenderTripe(FamilyMember.FATHER, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.SON, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.MOTHER, Gender.FEMALE));
        triples.add(new GenderTripe(FamilyMember.DAUGHTER, Gender.FEMALE));

        /* Create KB */
        NumeratedKb kb = createKbAndWriteMeta(basePath, kbName, families, errorRate);
        PrintWriter writer = new PrintWriter(Paths.get(basePath, kb.getName(), "triples.meta").toFile());
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyRelations[] familyRelationsValues = FamilyRelations.values();

        for (int i = 0; i < families; i++) {
            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorRate > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorRate / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyRelations =
                                    familyRelationsValues[random.nextInt(familyRelationsValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, kb, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, kb, triple, i, i);
                }
            }
        }
        writer.close();
        kb.dump(basePath);
    }

    /**
     * Generator of simple datasets, which are composed of 4 relations: father, mother, parent, gender.
     *
     * @param basePath The base directory where the KB is located
     * @param kbName The name of the KB
     * @param families The number of families in the KB
     * @param errorRate The probability that errors occur in the KB
     * @throws IOException File I/O errors
     * @throws KbException KB operation errors
     */
    public static void generateSimple(String basePath, String kbName, int families, double errorRate) throws IOException, KbException {
        Random random = new Random();
        List<Triple> triples = new ArrayList<>();

        /* father/mother */
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.DAUGHTER));

        /* parent */
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.PARENT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.PARENT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.PARENT, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.PARENT, FamilyMember.DAUGHTER));

        /* gender */
        triples.add(new GenderTripe(FamilyMember.GRAND_FATHER, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.GRAND_MOTHER, Gender.FEMALE));
        triples.add(new GenderTripe(FamilyMember.W_GRAND_FATHER, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.W_GRAND_MOTHER, Gender.FEMALE));
        triples.add(new GenderTripe(FamilyMember.FATHER, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.MOTHER, Gender.FEMALE));
        triples.add(new GenderTripe(FamilyMember.SON, Gender.MALE));
        triples.add(new GenderTripe(FamilyMember.DAUGHTER, Gender.FEMALE));

        /* Create KB */
        NumeratedKb kb = createKbAndWriteMeta(basePath, kbName, families, errorRate);
        PrintWriter writer = new PrintWriter(Paths.get(basePath, kb.getName(), "triples.meta").toFile());
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyRelations[] familyRelationsValues = FamilyRelations.values();

        for (int i = 0; i < families; i++) {
            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorRate > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorRate / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyRelations =
                                    familyRelationsValues[random.nextInt(familyRelationsValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, kb, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, kb, triple, i, i);
                }
            }
        }
        writer.close();
        kb.dump(basePath);
    }

    /**
     * Generator of simple datasets, which are composed of 9 relations: father, mother, parent, brother, sister, sibling,
     * uncle, aunt, gender.
     *
     * @param basePath The base directory where the KB is located
     * @param kbName The name of the KB
     * @param families The number of families in the KB
     * @param errorRate The probability that errors occur in the KB
     * @throws IOException File I/O errors
     * @throws KbException KB operation errors
     */
    public static void generateMedium(String basePath, String kbName, int families, double errorRate) throws IOException, KbException {
        Random random = new Random();
        List<Triple> triples = new ArrayList<>();

        /* father/mother */
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.FATHER, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.MOTHER, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.FATHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.MOTHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.MOTHER, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.MOTHER, FamilyMember.SISTER));

        /* parent */
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_FATHER, FamilyRelations.PARENT, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.W_GRAND_MOTHER, FamilyRelations.PARENT, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.PARENT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.PARENT, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.PARENT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.PARENT, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.PARENT, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.PARENT, FamilyMember.SISTER));

        /* brother/sister */
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.BROTHER, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.BROTHER, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.BROTHER, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.BROTHER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.SISTER, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.SISTER, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.SISTER, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.SISTER, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.SISTER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.SISTER, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.BROTHER, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.BROTHER, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.BROTHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.BROTHER, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.BROTHER, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SISTER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SISTER, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SISTER, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.BROTHER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.BROTHER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.BROTHER, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SISTER, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SISTER, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SISTER, FamilyMember.BROTHER));

        /* sibling */
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.SIBLING, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.FATHER, FamilyRelations.SIBLING, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.SIBLING, FamilyMember.AUNT_A));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.SIBLING, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.SIBLING, FamilyMember.UNCLE_A));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.SIBLING, FamilyMember.FATHER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.SIBLING, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.SIBLING, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.SIBLING, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.SIBLING, FamilyMember.UNCLE_B));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.SIBLING, FamilyMember.MOTHER));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.SIBLING, FamilyMember.AUNT_B));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.SIBLING, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.SIBLING, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.SON, FamilyRelations.SIBLING, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SIBLING, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SIBLING, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.DAUGHTER, FamilyRelations.SIBLING, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.SIBLING, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.SIBLING, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.BROTHER, FamilyRelations.SIBLING, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SIBLING, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SIBLING, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.SISTER, FamilyRelations.SIBLING, FamilyMember.BROTHER));

        /* uncle/aunt */
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.UNCLE, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_A, FamilyRelations.UNCLE, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.AUNT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.AUNT_A, FamilyRelations.AUNT, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.UNCLE, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.UNCLE, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.AUNT, FamilyMember.SON));
        triples.add(new FamilyTriple(FamilyMember.AUNT_B, FamilyRelations.AUNT, FamilyMember.DAUGHTER));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.UNCLE, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.UNCLE_B, FamilyRelations.UNCLE, FamilyMember.SISTER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.AUNT, FamilyMember.BROTHER));
        triples.add(new FamilyTriple(FamilyMember.MOTHER, FamilyRelations.AUNT, FamilyMember.SISTER));

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

        NumeratedKb kb = createKbAndWriteMeta(basePath, kbName, families, errorRate);
        PrintWriter writer = new PrintWriter(Paths.get(basePath, kb.getName(), "triples.meta").toFile());
        FamilyMember[] familyMemberValues = FamilyMember.values();
        FamilyRelations[] familyRelationsValues = FamilyRelations.values();
        for (int i = 0; i < families; i++) {

            /* Perturb correct data */
            for (Triple triple: triples) {
                double magic = random.nextDouble();
                if (errorRate > magic) {
                    magic = random.nextDouble();

                    /* Determine which type of error it is */
                    if (errorRate / magic < 2) {
                        /* Alter constant */
                        if (triple instanceof GenderTripe) {
                            GenderTripe triple_gender = (GenderTripe)triple;
                            triple_gender.gender = (Gender.MALE == triple_gender.gender)? Gender.FEMALE: Gender.MALE;
                        } else if (triple instanceof FamilyTriple) {
                            FamilyTriple triple_family = (FamilyTriple) triple;
                            triple_family.subject = familyMemberValues[random.nextInt(familyMemberValues.length)];
                            triple_family.familyRelations =
                                    familyRelationsValues[random.nextInt(familyRelationsValues.length)];
                            triple_family.object = familyMemberValues[random.nextInt(familyMemberValues.length)];
                        }
                        writeRelation(writer, kb, triple, random.nextInt(families), random.nextInt(families));
                    }
                    /* Other wise omit writing to represent missing triple */
                } else {
                    writeRelation(writer, kb, triple, i, i);
                }
            }
        }
        writer.close();
        kb.dump(basePath);
    }

    /**
     * Create a numerated KB and write down a meta file for the configurations.
     *
     * @throws IOException KB directory creation failed.
     */
    protected static NumeratedKb createKbAndWriteMeta(String basePath, String kbName, int families, double errorRate) throws IOException {
        NumeratedKb kb = new NumeratedKb(kbName);
        File kb_path_dir = NumeratedKb.getKbPath(kb.getName(), basePath).toFile();
        if (!kb_path_dir.exists() && !kb_path_dir.mkdirs()) {
            throw new IOException("KB path creation failed: " + kb_path_dir.getAbsolutePath());
        }
        PrintWriter meta_writer = new PrintWriter(Paths.get(kb_path_dir.getAbsolutePath(), "config.meta").toFile());
        meta_writer.printf("Families:\t%d\n", families);
        meta_writer.printf("Error Rate:\t%.2f\n", errorRate);
        meta_writer.close();
        return kb;
    }

    /**
     * Write down a triple into 'triples.meta' file and add to KB
     *
     * @param writer The writer to the meta file
     * @param triple The triple that should be written
     * @param subjId The id of the subject
     * @param objId The id of the object
     */
    protected static void writeRelation(PrintWriter writer, NumeratedKb kb, Triple triple, int subjId, int objId) throws KbException {
        String relation = null;
        String subject = null;
        String object = null;
        if (triple instanceof FamilyTriple) {
            FamilyTriple triple_family = (FamilyTriple)triple;
            relation = triple_family.familyRelations.name;
            subject = String.format("%s_%d", triple_family.subject.shortName, subjId);
            object = String.format("%s_%d", triple_family.object.shortName, objId);
            kb.addRecord(relation, new String[]{subject, object});
        } else if (triple instanceof GenderTripe) {
            GenderTripe triple_gender = (GenderTripe)triple;
            relation = OtherRelation.GENDER.name;
            subject = String.format("%s_%d", triple_gender.subject.shortName, subjId);
            object = triple_gender.gender.name;
        }
        writer.print(relation);
        writer.print('\t');
        writer.print(subject);
        writer.print('\t');
        writer.print(object);
        writer.print('\n');
        kb.addRecord(relation, new String[]{subject, object});
    }
}
