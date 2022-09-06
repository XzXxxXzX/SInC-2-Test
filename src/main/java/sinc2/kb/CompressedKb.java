package sinc2.kb;

import sinc2.common.Argument;
import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.common.Predicate;
import sinc2.rule.BareRule;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;
import sinc2.util.LittleEndianIntIO;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is for the compressed KB. It extends the numerated KB in three perspectives:
 *   1. The compressed KB contains counterexample relations:
 *      - The counterexamples are stored into '.ceg' files. The file format is the same as '.rel' files. The names of the
 *        files are '<relation>.ceg'.
 *      - If there is no counterexample in a relation, the counterexample relation file is not created.
 *   2. The compressed KB contains a hypothesis set:
 *      - The hypothesis set is stored into a 'rules.hyp' file. The rules are written in the form of plain text, one per
 *        line.
 *      - If there is no rule in the hypothesis set, the file will not be created.
 *   3. The compressed KB contains a supplementary constant set:
 *      - The supplementary constant set is stored into a 'supplementary.cst' file. Constant numerations in the mapping
 *        are stored in the file.
 *        Note: The numeration mapping in a compressed KB contains all mappings as the original KB does.
 *      - If there is no element in the supplementary set, the file will not be created.
 * The necessary facts are stored as is in the original KB.
 *
 * @since 2.0
 */
public class CompressedKb extends NumeratedKb {

    /** A regex pattern used to parse the counterexample file name */
    protected static final Pattern COUNTEREXAMPLE_FILE_NAME_PATTERN = Pattern.compile("(.+).ceg$");
    /** The name of the hypothesis file */
    protected static final String HYPOTHESIS_FILE_NAME = "rules.hyp";
    /** The name of the second mapping file for the supplementary constants */
    protected static final String SUPPLEMENTARY_CONSTANTS_FILE_NAME = "supplementary.cst";

    /** The reference to the original KB. The original KB is used for determining the missing constants. */
    protected final NumeratedKb originalKb;
    /** The hypothesis set, i.e., a list of rules */
    protected List<Rule> hypothesis = new ArrayList<>();
    /** The facts that are counterexamples. The necessary facts are stored as is in the NumerationKb */
    protected Map<Integer, KbRelation> counterexampleRelationMap = new HashMap<>();
    /** The constants marked in a supplementary set. Otherwise they are lost due to removal of facts. */
    protected Set<Integer> supplementaryConstants = new HashSet<>();

    public static String getCounterexampleFileName(String relName) {
        return relName + ".ceg";
    }

    /**
     * Create an empty compressed KB.
     *
     * @param name The name of the KB
     * @param originalKb The original KB this compressed one is compressed from
     */
    public CompressedKb(String name, NumeratedKb originalKb) {
        super(name);
        this.originalKb = originalKb;
        this.numMap = new NumerationMap(originalKb.numMap.numMap);
        /* Create relations */
        for (KbRelation relation: originalKb.getRelations()) {
            KbRelation necessary_relation = new KbRelation(relation.getName(), relation.getNumeration(), relation.getArity());
            KbRelation counterexample_relation = new KbRelation(relation.getName(), relation.getNumeration(), relation.getArity());
            relationMap.put(relation.getNumeration(), necessary_relation);
            counterexampleRelationMap.put(relation.getNumeration(), counterexample_relation);
        }
    }

    /**
     * Load a compressed KB from files. The loaded records are not checked.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     * @param originalKb The original KB this compressed one is compressed from
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     * @throws RuleParseException When a hypothesis rule parsing failed
     */
    public CompressedKb(String name, String basePath, NumeratedKb originalKb) throws IOException, KbException, RuleParseException {
        super(name, basePath);
        this.originalKb = originalKb;
        /* Create relations */
        for (KbRelation relation: originalKb.getRelations()) {
            relationMap.putIfAbsent(
                    relation.getNumeration(), new KbRelation(
                            relation.getName(), relation.getNumeration(), relation.getArity()
                    )
            );
            counterexampleRelationMap.put(
                    relation.getNumeration(), new KbRelation(
                            relation.getName(), relation.getNumeration(), relation.getArity()
                    )
            );
        }
        File kb_dir = getKbPath(name, basePath).toFile();
        loadHypothesisHandler(kb_dir, false);
        loadCounterexamplesHandler(kb_dir, false);
        loadSupplementaryConstantsHandler(kb_dir, false);
    }

    /**
     * Load a compressed KB from files.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     * @param originalKb The original KB this compressed one is compressed from
     * @param check Whether the records are checked when loaded
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     * @throws RuleParseException When a hypothesis rule parsing failed
     */
    public CompressedKb(String name, String basePath, NumeratedKb originalKb, boolean check)
            throws IOException, KbException, RuleParseException {
        super(name, basePath, check);
        this.originalKb = originalKb;
        /* Create relations */
        for (KbRelation relation: originalKb.getRelations()) {
            relationMap.putIfAbsent(
                    relation.getNumeration(), new KbRelation(
                            relation.getName(), relation.getNumeration(), relation.getArity()
                    )
            );
            counterexampleRelationMap.put(
                    relation.getNumeration(), new KbRelation(
                            relation.getName(), relation.getNumeration(), relation.getArity()
                    )
            );
        }
        File kb_dir = getKbPath(name, basePath).toFile();
        loadHypothesisHandler(kb_dir, check);
        loadCounterexamplesHandler(kb_dir, check);
        loadSupplementaryConstantsHandler(kb_dir, check);
    }

    /**
     * Load hypothesis rules from the local file system. The relations are automatically registered in the KB if not by
     * the necessary facts. The loaded rules are "BareRule" instances.
     *
     * @param kbDir The KB directory file
     * @param check Whether the constants are checked when loaded
     * @throws IOException When file I/O errors occur
     * @throws KbException When a relation shows different arity
     * @throws RuleParseException When a hypothesis rule parsing failed
     */
    protected void loadHypothesisHandler(File kbDir, boolean check) throws IOException, KbException, RuleParseException {
        File hypo_file = Paths.get(kbDir.getAbsolutePath(), HYPOTHESIS_FILE_NAME).toFile();
        if (hypo_file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(hypo_file));
            String line;
            if (check) {
                while (null != (line = reader.readLine())) {
                    List<ParsedPred> str_rule_structure = Rule.parseStructure(line);

                    /* Register relation and constants information from rules */
                    /* Construct a BareRule structure */
                    List<Predicate> rule_structure = new ArrayList<>(str_rule_structure.size());
                    for (ParsedPred parsed_pred : str_rule_structure) {
                        /* Check the relation arity */
                        KbRelation relation = getRelation(parsed_pred.functor);
                        if (null == relation) {
                            relation = createRelation(parsed_pred.functor, parsed_pred.args.length);
                        }
                        if (relation.getArity() != parsed_pred.args.length) {
                            throw new KbException(String.format(
                                    "Relation arity (%s/%d) in the rule is different from that in the KB (%s/%d): %s",
                                    parsed_pred.functor, parsed_pred.args.length, relation.getName(), relation.getArity(),
                                    line
                            ));
                        }

                        Predicate predicate = new Predicate(relation.getNumeration(), relation.getArity());
                        for (int i = 0; i < predicate.arity(); i++) {
                            ParsedArg parsed_arg = parsed_pred.args[i];
                            if (null == parsed_arg) {
                                predicate.args[i] = Argument.EMPTY_VALUE;
                            } else if (parsed_arg.isVariable()) {
                                predicate.args[i] = Argument.variable(parsed_arg.id);
                            } else {
                                int constant = numMap.name2Num(parsed_arg.name);
                                if (NumerationMap.NUM_NULL == constant) {
                                    reader.close();
                                    throw new KbException(String.format("Loaded constant is not mapped: %s", parsed_arg.name));
                                }
                                predicate.args[i] = Argument.constant(constant);
                            }
                        }
                        rule_structure.add(predicate);
                    }
                    hypothesis.add(new BareRule(rule_structure, new HashSet<>(), new HashMap<>()));
                }
            } else {
                while (null != (line = reader.readLine())) {
                    List<ParsedPred> str_rule_structure = Rule.parseStructure(line);

                    /* Register relation and constants information from rules */
                    /* Construct a BareRule structure */
                    List<Predicate> rule_structure = new ArrayList<>(str_rule_structure.size());
                    for (ParsedPred parsed_pred : str_rule_structure) {
                        /* Check the relation arity */
                        KbRelation relation = getRelation(parsed_pred.functor);
                        if (null == relation) {
                            relation = createRelation(parsed_pred.functor, parsed_pred.args.length);
                        }
                        if (relation.getArity() != parsed_pred.args.length) {
                            throw new KbException(String.format(
                                    "Relation arity (%s/%d) in the rule is different from that in the KB (%s/%d): %s",
                                    parsed_pred.functor, parsed_pred.args.length, relation.getName(), relation.getArity(),
                                    line
                            ));
                        }

                        Predicate predicate = new Predicate(relation.getNumeration(), relation.getArity());
                        for (int i = 0; i < predicate.arity(); i++) {
                            ParsedArg parsed_arg = parsed_pred.args[i];
                            if (null == parsed_arg) {
                                predicate.args[i] = Argument.EMPTY_VALUE;
                            } else if (parsed_arg.isVariable()) {
                                predicate.args[i] = Argument.variable(parsed_arg.id);
                            } else {
                                int constant = numMap.name2Num(parsed_arg.name);
                                predicate.args[i] = Argument.constant(constant);
                            }
                        }
                        rule_structure.add(predicate);
                    }
                    hypothesis.add(new BareRule(rule_structure, new HashSet<>(), new HashMap<>()));
                }
            }
            reader.close();
        }
    }

    /**
     * Load all counterexamples in the compressed KB.
     *
     * @param kbDir The directory file of the KB
     * @param check Whether the constants are checked when loaded
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    protected void loadCounterexamplesHandler(File kbDir, boolean check) throws IOException, KbException {
        String kb_dir_path = kbDir.getAbsolutePath();
        File[] files = kbDir.listFiles();
        if (null != files) {
            for (File f: files) {
                Matcher matcher = COUNTEREXAMPLE_FILE_NAME_PATTERN.matcher(f.getName());
                if (matcher.find()) {
                    String rel_name = matcher.group(1);
                    /* The relation must have been registered, either by the necessary facts or by the hypothesis rules.
                     * Therefore, if a relation has not been registered in the KB, the counterexamples should not be
                     * loaded. */
                    KbRelation relation = getRelation(rel_name);
                    if (null != relation) {
                        /* Constants in the counterexamples should not be checked because some of them may not appear in
                         * the necessary facts and rules */
                        KbRelation ceg_relation = new KbRelation(
                                rel_name, relation.getNumeration(), relation.getArity(),
                                getCounterexampleFileName(rel_name), kb_dir_path, check ? numMap : null
                        );
                        counterexampleRelationMap.put(ceg_relation.getNumeration(), ceg_relation);
                    }
                }
            }
        }
    }

    /**
     * Load the supplementary constant numerations.
     * @throws IOException When file I/O errors occur
     * @throws KbException When "check=true" and a constant numeration is not mapped
     */
    protected void loadSupplementaryConstantsHandler(File kbDir, boolean check) throws KbException, IOException {
        File sup_const_file = Paths.get(kbDir.getAbsolutePath(), SUPPLEMENTARY_CONSTANTS_FILE_NAME).toFile();
        if (sup_const_file.exists()) {
            FileInputStream fis = new FileInputStream(sup_const_file);
            byte[] buffer = new byte[Integer.BYTES];
            if (check) {
                while (Integer.BYTES == fis.read(buffer)) {
                    int constant = LittleEndianIntIO.byteArray2LeInt(buffer);
                    if (null == numMap.num2Name(constant)) {
                        throw new KbException(String.format(
                                "Supplementary constant numeration is not mapped in the KB: %d", constant
                        ));
                    }
                    supplementaryConstants.add(constant);
                }
            } else {
                while (Integer.BYTES == fis.read(buffer)) {
                    int constant = LittleEndianIntIO.byteArray2LeInt(buffer);
                    supplementaryConstants.add(constant);
                }
            }
        }
    }

    /**
     * Add a record where arguments are numbers to the counterexample set.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addCounterexample(int relNum, int[] record) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (record.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", record.length, arity, Arrays.toString(record)
            ));
        }

        for (int arg: record) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        KbRelation ceg_relation = counterexampleRelationMap.computeIfAbsent(
                relNum, k -> new KbRelation(relation.getName(), relation.getNumeration(), relation.getArity())
        );
        addCounterexampleHandler(ceg_relation, new Record(record));
    }

    /**
     * Add records to the counterexample set.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addCounterexamples(int relNum, int[][] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        KbRelation ceg_relation = counterexampleRelationMap.computeIfAbsent(
                relNum, k -> new KbRelation(relation.getName(), relation.getNumeration(), relation.getArity())
        );
        for (int[] record: records) {
            if (record.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.length, arity, Arrays.toString(record)
                ));
            }

            for (int arg : record) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addCounterexampleHandler(ceg_relation, new Record(record));
        }
    }

    /**
     * Add a record to a counterexample relation.
     *
     * @throws KbException Record arity does not match the relation
     * any string
     */
    protected void addCounterexampleHandler(KbRelation relation, Record record) throws KbException {
        relation.addRecord(record);
    }

    /**
     * Remove a record form the counterexample set.
     */
    public void removeCounterexample(int relNum, int[] record) {
        KbRelation relation = counterexampleRelationMap.get(relNum);
        if (null != relation) {
            removeCounterexampleHandler(relation, new Record(record));
        }
    }

    /**
     * Remove a record form the necessary relation.
     */
    protected void removeCounterexampleHandler(KbRelation relation, Record record) {
        relation.removeRecord(record);
    }

    public boolean hasCounterexample(int relNum, int[] record) {
        KbRelation relation = counterexampleRelationMap.get(relNum);
        return null != relation && relation.hasRecord(new Record(record));
    }

    public void addHypothesisRule(Rule rule) {
        hypothesis.add(rule);
    }

    public void addHypothesisRules(Rule[] rules) {
        hypothesis.addAll(List.of(rules));
    }

    public void removeHypothesisRule(Rule rule) {
        hypothesis.remove(rule);
    }

    public boolean hasHypothesisRule(Rule rule) {
        return hypothesis.contains(rule);
    }

    @Override
    public void dump(String basePath) throws IOException {
        super.dump(basePath);
        String kb_dir_path = getKbPath(name, basePath).toString();

        /* Dump hypothesis */
        if (0 < hypothesis.size()) {
            PrintWriter writer = new PrintWriter(Paths.get(kb_dir_path, HYPOTHESIS_FILE_NAME).toFile());
            for (Rule rule : hypothesis) {
                writer.println(rule.toDumpString(numMap));
            }
            writer.close();
        }

        /* Dump counterexample relations */
        for (KbRelation relation: counterexampleRelationMap.values()) {
            if (0 < relation.totalRecords()) {
                /* Dump only non-empty relations */
                relation.dump(kb_dir_path, getCounterexampleFileName(relation.getName()));
            }
        }

        /* Dump supplementary constants */
        updateSupplementaryConstants();
        if (0 < supplementaryConstants.size()) {
            FileOutputStream fos = new FileOutputStream(Paths.get(kb_dir_path, SUPPLEMENTARY_CONSTANTS_FILE_NAME).toFile());
            for (int i: supplementaryConstants) {
                fos.write(LittleEndianIntIO.leInt2ByteArray(i));
            }
            fos.close();
        }
    }

    /**
     * Update the supplementary constant set.
     */
    protected void updateSupplementaryConstants() {
        /* Find all the constant symbols */
        Set<Integer> lost_constants = new HashSet<>(originalKb.getAllConstants());

        /* Find the mappings for the constants appearing in necessary facts, rules, and counterexamples */
        for (KbRelation relation: relationMap.values()) {
            for (Record record: relation) {
                for (int argument: record.args) {
                    lost_constants.remove(Argument.decode(argument));
                }
            }
        }
        for (Rule rule: hypothesis) {
            for (int pred_idx = 0; pred_idx < rule.predicates(); pred_idx++) {
                for (int argument: rule.getPredicate(pred_idx).args) {
                    if (Argument.isConstant(argument)) {
                        lost_constants.remove(Argument.decode(argument));
                    }
                }
            }
        }
        for (KbRelation relation: counterexampleRelationMap.values()) {
            for (Record record: relation) {
                for (int argument: record.args) {
                    lost_constants.remove(Argument.decode(argument));
                }
            }
        }
        supplementaryConstants = lost_constants;
    }

    public List<Rule> getHypothesis() {
        return hypothesis;
    }

    public Collection<KbRelation> getCounterexampleRelations() {
        return counterexampleRelationMap.values();
    }

    public KbRelation getCounterexampleRelation(int relNum) {
        return counterexampleRelationMap.get(relNum);
    }

    public int totalNecessaryRecords() {
        return totalRecords();
    }

    public int totalCounterexamples() {
        int cnt = 0;
        for (KbRelation relation: counterexampleRelationMap.values()) {
            cnt += relation.totalRecords();
        }
        return cnt;
    }

    public int totalHypothesisSize() {
        int cnt = 0;
        for (Rule rule: hypothesis) {
            cnt += rule.length();
        }
        return cnt;
    }

    public int totalSupplementaryConstants() {
        updateSupplementaryConstants();
        return supplementaryConstants.size();
    }
}
