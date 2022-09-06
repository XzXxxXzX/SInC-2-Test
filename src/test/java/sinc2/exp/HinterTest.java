package sinc2.exp;

import org.junit.jupiter.api.Test;
import sinc2.util.MultiSet;
import sinc2.common.Argument;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.common.Predicate;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;
import sinc2.kb.NumerationMap;
import sinc2.rule.BareRule;
import sinc2.rule.Fingerprint;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HinterTest {

    static final String MEM_DIR = "D:/sjtu/KB_proj/mem_cache";

    @Test
    void testRun1() throws KbException, IOException, ExperimentException, RuleParseException {
        /* Hints:
         *   p(X,Y):-q(X,Y);[(p,q)]
         *   p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]
         *
         * Rules:
         *   parent(X,Y):-father(X,Y)
         *   parent(X,Y):-mother(X,Y)
         *   family(X,Y,Z):-father(X,Z),mother(Y,Z)
         */
        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String PARENT = "parent";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});
            kb.addRecord(PARENT, new String[]{dad, son});
            kb.addRecord(PARENT, new String[]{dad, daughter});
            kb.addRecord(PARENT, new String[]{mom, son});
            kb.addRecord(PARENT, new String[]{mom, daughter});
            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});
        }
        kb.dump(MEM_DIR);

        /* Create hint file */
        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("p(X,Y):-q(X,Y);[(p,q)]");
        hint_writer.println("p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]");
        hint_writer.close();

        /* Run hinter */
        Hinter hinter = new Hinter(MEM_DIR, KB_NAME, hint_file.getAbsolutePath());
        hinter.run();

        /* Check result */
        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("parent(X,Y):-father(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("parent(X,Y):-mother(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("family(X,Y,Z):-father(X,Z),mother(Y,Z)", kb.getNumerationMap(), cache, tabu));
        File rules_file = Hinter.getRulesFilePath(hint_file.getAbsolutePath(), KB_NAME).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun2() throws KbException, IOException, ExperimentException, RuleParseException {
        /* Hints:
         *   p(X,Y):-q(X,Y);[(p,q)]
         *   p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]
         *
         * Rules:
         *   parent(X,Y):-father(X,Y)
         *   parent(X,Y):-mother(X,Y)
         *   family(X,Y,Z):-father(X,Z),mother(Y,Z)
         */
        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String PARENT = "parent";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final String GRANDMOTHER = "grandmother";
        final String GRANDFATHER = "grandfather";
        final String GRANDPARENT = "grandparent";
        final String GRANDCHILD = "grandchild";

        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int GRANDMOTHER_ARITY=2;
        final int GRANDFATHER_ARITY=2;
        final int GRANDCHILD_ARITY=2;

        final int FAMILIES = 10;
        final String GDAD_D = "granddad_d";
        final String GMOM_D = "grandmom_d";
        final String GDAD_M = "granddad_m";
        final String GMOM_M = "grandmom_m";
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String gdad_d=GDAD_D+i;
            String gmom_d=GMOM_D+i;
            String gdad_m=GDAD_M+i;
            String gmom_m=GMOM_M+i;


            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(PARENT, new String[]{dad, son});
            kb.addRecord(PARENT, new String[]{mom, son});
            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{son, mom});

            kb.addRecord(FAMILY, new String[]{gdad_d, gmom_d, dad});
            kb.addRecord(FATHER, new String[]{gdad_d, dad});
            kb.addRecord(MOTHER, new String[]{gmom_d, dad});
            kb.addRecord(PARENT, new String[]{gdad_d, dad});
            kb.addRecord(PARENT, new String[]{gmom_d, dad});
            kb.addRecord(CHILD, new String[]{dad, gdad_d});
            kb.addRecord(CHILD, new String[]{dad, gmom_d});

            kb.addRecord(FAMILY, new String[]{gdad_m, gmom_m, mom});
            kb.addRecord(FATHER, new String[]{gdad_m, mom});
            kb.addRecord(MOTHER, new String[]{gmom_m, mom});
            kb.addRecord(PARENT, new String[]{gdad_m, mom});
            kb.addRecord(PARENT, new String[]{gmom_m, mom});
            kb.addRecord(CHILD, new String[]{mom, gdad_m});
            kb.addRecord(CHILD, new String[]{mom, gmom_m});

            kb.addRecord(GRANDFATHER, new String[]{gdad_d, son});
            kb.addRecord(GRANDFATHER, new String[]{gdad_m, son});
            kb.addRecord(GRANDCHILD, new String[]{son, gdad_d});
            kb.addRecord(GRANDCHILD, new String[]{son, gdad_m});
            kb.addRecord(GRANDMOTHER, new String[]{gmom_d, son});
            kb.addRecord(GRANDMOTHER, new String[]{gmom_m, son});
            kb.addRecord(GRANDPARENT, new String[]{gdad_d, son});
            kb.addRecord(GRANDPARENT, new String[]{gmom_d, son});
            kb.addRecord(GRANDPARENT, new String[]{gdad_m, son});
            kb.addRecord(GRANDPARENT, new String[]{gmom_m, son});
            kb.addRecord(GRANDCHILD, new String[]{son, gmom_d});
            kb.addRecord(GRANDCHILD, new String[]{son, gmom_m});

        }
        kb.dump(MEM_DIR);

        /* Create hint file */
        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        hint_writer.println("p(X,Y):-q(X,Y);[(p,q)]");
        hint_writer.println("p(X,Y):-q(Y,X);[]");
        hint_writer.println("p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]");
        hint_writer.println("p(X,Y):-q(X,Z),r(Z,Y);[]");
        hint_writer.close();

        /* Run hinter */
        Hinter hinter = new Hinter(MEM_DIR, KB_NAME, hint_file.getAbsolutePath());
        hinter.run();

        /* Check result */
        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("parent(X,Y):-father(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("parent(X,Y):-mother(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandparent(X,Y):-grandfather(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandparent(X,Y):-grandmother(X,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("parent(X,Y):-child(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-parent(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-father(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-mother(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandparent(X,Y):-grandchild(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandchild(X,Y):-grandparent(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandchild(X,Y):-grandfather(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandchild(X,Y):-grandmother(Y,X)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("family(X,Y,Z):-father(X,Z),mother(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandparent(X,Y):-parent(X,Z),parent(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandfather(X,Y):-father(X,Z),father(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandfather(X,Y):-father(X,Z),mother(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandfather(X,Y):-father(X,Z),parent(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandmother(X,Y):-mother(X,Z),father(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandmother(X,Y):-mother(X,Z),mother(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandmother(X,Y):-mother(X,Z),parent(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("grandchild(X,Y):-child(X,Z),child(Z,Y)", kb.getNumerationMap(), cache, tabu));
        File rules_file = Hinter.getRulesFilePath(hint_file.getAbsolutePath(), KB_NAME).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    @Test
    void testRun3() throws KbException, IOException, ExperimentException, RuleParseException {
        /* Hints:
         *   p(X,Y):-q(X,Y);[(p,q)]
         *   p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]
         *
         * Rules:
         *   parent(X,Y):-father(X,Y)
         *   parent(X,Y):-mother(X,Y)
         *   family(X,Y,Z):-father(X,Z),mother(Y,Z)
         */
        final String KB_NAME = "HinterTest-" + UUID.randomUUID();
        final String FAMILY = "family";
        final String PARENT = "parent";
        final String FATHER = "father";
        final String MOTHER = "mother";
        final String CHILD = "child";
        final String COUPLE="couple";

        final int FAMILY_ARITY = 3;
        final int PARENT_ARITY = 2;
        final int FATHER_ARITY = 2;
        final int MOTHER_ARITY = 2;
        final int CHILD_ARITY = 2;
        final int COUPLE_ARITY = 2;

        final int FAMILIES = 10;
        final String DAD = "dad";
        final String MOM = "mom";
        final String SON = "son";
        final String DAUGHTER = "daughter";
        final String HINT_FILE_NAME = "template.hint";

        /* Create KB */
        NumeratedKb kb = new NumeratedKb(KB_NAME);
        for (int i = 0; i < FAMILIES; i++) {
            String dad = DAD + i;
            String mom = MOM + i;
            String son = SON + i;
            String daughter = DAUGHTER + i;
            kb.addRecord(FAMILY, new String[]{dad, mom, son});
            kb.addRecord(FAMILY, new String[]{dad, mom, daughter});
            kb.addRecord(FATHER, new String[]{dad, son});
            kb.addRecord(FATHER, new String[]{dad, daughter});
            kb.addRecord(MOTHER, new String[]{mom, son});
            kb.addRecord(MOTHER, new String[]{mom, daughter});
            //kb.addRecord(PARENT, new String[]{dad, son});
            //kb.addRecord(PARENT, new String[]{dad, daughter});
            //kb.addRecord(PARENT, new String[]{mom, son});
            //kb.addRecord(PARENT, new String[]{mom, daughter});
            kb.addRecord(CHILD, new String[]{son, dad});
            kb.addRecord(CHILD, new String[]{daughter, dad});
            kb.addRecord(CHILD, new String[]{son, mom});
            kb.addRecord(CHILD, new String[]{daughter, mom});
            kb.addRecord(COUPLE,new String[]{dad, mom});
        }
        kb.dump(MEM_DIR);

        /* Create hint file */
        File hint_file = Paths.get(MEM_DIR, HINT_FILE_NAME).toFile();
        PrintWriter hint_writer = new PrintWriter(hint_file);
        hint_writer.println(0.2);
        hint_writer.println(0.8);
        //hint_writer.println("p(X,Y):-q(X,Y);[(p,q)]");
        hint_writer.println("p(X,Y,Z):-q(X,Z),r(Y,Z);[(p,r),(q,r)]");
        hint_writer.println("p(X,Y):-q(X,Z),r(Y,Z);[]");
        hint_writer.println("p(X,Y):-q(Z,X),r(Z,Y);[]");
        //hint_writer.println("p(X,Y):-q(Z,X),r(Y,Z);[]");
        hint_writer.close();

        /* Run hinter */
        Hinter hinter = new Hinter(MEM_DIR, KB_NAME, hint_file.getAbsolutePath());
        hinter.run();

        /* Check result */
        Set<Fingerprint> cache = new HashSet<>();
        Map<MultiSet<Integer>, Set<Fingerprint>> tabu = new HashMap<>();
        Set<Rule> expected_rules = new HashSet<>();
        expected_rules.add(parseBareRule("family(X,Y,Z):-father(X,Z),mother(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("couple(X,Y):-father(X,Z),mother(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("father(X,Y):-couple(X,Z),child(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-child(X,Z),couple(Y,Z)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("child(X,Y):-father(Z,X),couple(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("mother(X,Y):-couple(Z,X),father(Z,Y)", kb.getNumerationMap(), cache, tabu));
        expected_rules.add(parseBareRule("couple(X,Y):-child(Z,X),child(Z,Y)", kb.getNumerationMap(), cache, tabu));
        //expected_rules.add(parseBareRule("child(X,Y):-mother(Z,X),couple(Y,Z)", kb.getNumerationMap(), cache, tabu));
        //expected_rules.add(parseBareRule("father(X,Y):-couple(X,Z),mother(Z,Y)", kb.getNumerationMap(), cache, tabu));
        File rules_file = Hinter.getRulesFilePath(hint_file.getAbsolutePath(), KB_NAME).toFile();
        BufferedReader reader = new BufferedReader(new FileReader(rules_file));
        String line = reader.readLine();    // read the title line
        Set<Rule> actual_rules = new HashSet<>();
        while (null != (line = reader.readLine())) {
            actual_rules.add(parseBareRule(line.split("\t")[0], kb.getNumerationMap(), cache, tabu));
        }
        assertEquals(expected_rules, actual_rules);

        /* Remove test files */
        deleteDir(Paths.get(MEM_DIR, KB_NAME).toFile());
        hint_file.delete();
        rules_file.delete();
    }

    BareRule parseBareRule(String str, NumerationMap numMap, Set<Fingerprint> cache, Map<MultiSet<Integer>, Set<Fingerprint>> tabu) throws RuleParseException {
        List<ParsedPred> parsed_structure = Rule.parseStructure(str);
        List<Predicate> structure = new ArrayList<>();
        for (ParsedPred parsed_pred: parsed_structure) {
            Predicate predicate = new Predicate(numMap.name2Num(parsed_pred.functor), parsed_pred.args.length);
            for (int arg_idx = 0; arg_idx < parsed_pred.args.length; arg_idx++) {
                ParsedArg parsed_arg = parsed_pred.args[arg_idx];
                if (null == parsed_arg) {
                    predicate.args[arg_idx] = Argument.EMPTY_VALUE;
                } else if (null == parsed_arg.name) {
                    predicate.args[arg_idx] = Argument.variable(parsed_arg.id);
                } else {
                    predicate.args[arg_idx] = Argument.constant(numMap.name2Num(parsed_arg.name));
                }
            }
            structure.add(predicate);
        }
        return new BareRule(structure, cache, tabu);
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