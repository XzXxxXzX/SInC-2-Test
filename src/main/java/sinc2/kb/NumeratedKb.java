package sinc2.kb;

import sinc2.common.Argument;
import sinc2.util.MultiSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The in-memory KB. Name strings are converted to integer numbers to reduce memory cost and improve processing efficiency.
 * A KB is a set of 'NumeratedRelation'. A KB can be dumped into the local file system. A dumped KB is a directory (named
 * by the KB name) that contains multiple files:
 *   - The numeration mapping file: Please refer to class 'sinc2.kb.NumerationMap'
 *   - The relation files: Please refer to class 'sinc2.kb.KbRelation'
 *   - Meta information files:
 *       - There may be multiple files with extension `.meta` to store arbitrary meta information of the KB.
 *       - The files are customized by other utilities and are not in a fixed format.
 *
 * Note: the arguments in a record should be encoded values by "constant()/variable" methods in the "Argument" class.
 *
 * @since 2.0
 */
public class NumeratedKb {

    /** The name of the KB */
    protected final String name;
    /** The mapping from relation name numerations to relations */
    protected final Map<Integer, KbRelation> relationMap = new HashMap<>();
    /** The numeration map */
    protected NumerationMap numMap;
    /** The set of all constants in the KB */
    protected MultiSet<Integer> constants = new MultiSet<>();

    /**
     * Get the path for the files where the KB is dumped.
     *
     * @param kbName The name of the KB
     * @param basePath The path to the dir of KB files
     * @return The path for the files where the KB is dumped.
     */
    public static Path getKbPath(String kbName, String basePath) {
        return Paths.get(basePath, kbName);
    }

    /**
     * Create an empty KB.
     *
     * @param name The name of the KB
     */
    public NumeratedKb(String name) {
        this.name = name;
        this.numMap = new NumerationMap();
    }

    /**
     * Load a KB from files. The loaded records are not checked.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    public NumeratedKb(String name, String basePath) throws IOException, KbException {
        this.name = name;
        File kb_dir = getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        this.numMap = new NumerationMap(kb_dir_path);
        loadAllRelationsHandler(kb_dir, false);
    }

    /**
     * Load a KB from files.
     *
     * @param name The name of the KB
     * @param basePath The base path to the dir of the KB
     * @param check Whether the records are checked when loaded
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    public NumeratedKb(String name, String basePath, boolean check) throws IOException, KbException {
        this.name = name;
        File kb_dir = getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        this.numMap = new NumerationMap(kb_dir_path);
        loadAllRelationsHandler(kb_dir, check);
    }

    /**
     * Load all relations in the directory of the KB.
     *
     * @throws IOException When file I/O errors occur
     * @throws KbException When the record check fails
     */
    protected void loadAllRelationsHandler(File kbDir, boolean check) throws IOException, KbException {
        String kb_dir_path = kbDir.getAbsolutePath();
        File[] files = kbDir.listFiles();
        if (null != files) {
            for (File f: files) {
                KbRelation.RelationInfo rel_info = KbRelation.parseRelFilePath(f.getName());
                if (null != rel_info) {
                    KbRelation relation = new KbRelation(
                            rel_info.name, numMap.mapName(rel_info.name), rel_info.arity, rel_info.totalRecords,
                            kb_dir_path, check ? numMap : null
                    );
                    relationMap.put(relation.getNumeration(), relation);

                    /* Load Constants */
                    for (Record record: relation) {
                        for (int argument: record.args) {
                            constants.add(Argument.decode(argument));
                        }
                    }
                }
            }
        }
    }

    /**
     * Dump the KB to the local file system.
     *
     * @param basePath The path to the KB directory
     * @throws IOException Thrown when KB directory creation failed or errors occur in the dump of other files
     */
    public void dump(String basePath) throws IOException {
        /* Check & create dir */
        Path kb_dir = getKbPath(name, basePath);
        File kb_dir_file = kb_dir.toFile();
        if (!kb_dir_file.exists() && !kb_dir_file.mkdirs()) {
            throw new IOException("KB directory creation failed: " + kb_dir_file.getAbsolutePath());
        }

        /* Dump */
        String kb_dir_path = kb_dir_file.getAbsolutePath();
        numMap.dump(kb_dir_path);
        for (KbRelation relation: relationMap.values()) {
            if (0 < relation.totalRecords()) {
                /* Dump only non-empty relations */
                relation.dump(kb_dir_path);
            }
        }
    }

    /**
     * Create an empty relation in the KB. If the name 'relName' has been used, raise a KbException.
     *
     * @param relName The name of the relation
     * @param arity The arity of the relation
     * @return The created relation object
     * @throws KbException The relation name has already been used
     */
    public KbRelation createRelation(String relName, int arity) throws KbException {
        int num = numMap.name2Num(relName);
        if (NumerationMap.NUM_NULL == num) {
            num = numMap.mapName(relName);
        }
        KbRelation relation = relationMap.get(num);
        if (null != relation) {
            throw new KbException("The relation name has already been used: " + relName);
        }
        relation = new KbRelation(relName, num, arity);
        relationMap.put(num, relation);
        return relation;
    }

    /**
     * Load a relation into the KB from a '.rel' file.  If the name 'relName' has been used, raise a KbException.
     *
     * @param relBasePath The path where the file is stored.
     * @param check Whether loaded records are checked in the mapping
     * @return The loaded relation object
     * @throws KbException The name 'relName' has already been used; numerations in loaded records are not mapped if check=true.
     * @throws IOException File I/O errors
     */
    public KbRelation loadRelation(String relBasePath, String relName, int arity, int totalRecords, boolean check)
            throws KbException, IOException {
        int num = numMap.name2Num(relName);
        if (NumerationMap.NUM_NULL != num) {
            if (!relationMap.containsKey(num)) {
                throw new KbException("The relation name has already been used: " + relName);
            }
        } else {
            num = numMap.mapName(relName);
        }
        KbRelation relation = new KbRelation(relName, num, arity, totalRecords, relBasePath, check ? numMap:null);
        relationMap.put(num, relation);

        /* Load Constants */
        for (Record record: relation) {
            for (int argument: record.args) {
                constants.add(Argument.decode(argument));
            }
        }
        return relation;
    }

    /**
     * Remove a relation from the KB by the numeration of the relation name.
     *
     * @return The removed relation, or NULL if no such relation
     */
    public KbRelation deleteRelation(int relNum) {
        return deleteRelationHandler(relNum);
    }

    /**
     * Remove a relation from the KB by the relation name.
     *
     * @return The removed relation, or NULL if no such relation
     */
    public KbRelation deleteRelation(String relName) {
        return deleteRelationHandler(numMap.name2Num(relName));
    }

    protected KbRelation deleteRelationHandler(int relNum) {
        KbRelation relation = relationMap.remove(relNum);
        if (null != relation) {
            /* Remove Constants */
            for (Record record: relation) {
                for (int argument: record.args) {
                    constants.remove(Argument.decode(argument));
                }
            }
        }
        return relation;
    }

    /**
     * Fetch the relation object from the KB by relation name. NULL if no such relation.
     */
    public KbRelation getRelation(String relName) {
        return relationMap.get(numMap.name2Num(relName));
    }

    /**
     * Fetch the relation object from the KB by relation name numeration. NULL if no such relation.
     */
    public KbRelation getRelation(int relNum) {
        return relationMap.get(relNum);
    }

    /**
     * Check the existence of the relation by the name.
     */
    public boolean hasRelation(String relName) {
        return relationMap.containsKey(numMap.name2Num(relName));
    }

    /**
     * Check the existence of the relation by the name numeration.
     */
    public boolean hasRelation(int relNum) {
        return relationMap.containsKey(relNum);
    }

    /**
     * Get the arity of the relation. 0 if no such relation.
     */
    public int getRelationArity(String relName) {
        KbRelation relation = getRelation(relName);
        return (null == relation) ? 0 : relation.getArity();
    }

    /**
     * Get the arity of the relation. 0 if no such relation.
     */
    public int getRelationArity(int relNum) {
        KbRelation relation = getRelation(relNum);
        return (null == relation) ? 0 : relation.getArity();
    }

    /**
     * Add a record where arguments are name strings to the KB. The names will be converted to numerations (or be added
     * to the mapping first) before the record is added. A new KbRelation wil be created If the relation does not exist
     * in the KB.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     * @throws KbException Record arity does not match the relation
     */
    public void addRecord(String relName, String[] argNames) throws KbException {
        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, argNames.length);
        }

        int arity = relation.getArity();
        if (argNames.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", argNames.length, arity,
                    Arrays.toString(argNames)
            ));
        }

        int[] arg_nums = new int[argNames.length];
        for (int i = 0; i < arity; i++) {
            arg_nums[i] = Argument.constant(numMap.mapName(argNames[i]));
        }
        addRecordHandler(relation, new Record(arg_nums));
    }

    /**
     * Add a record where arguments are numbers to the KB. A new KbRelation will be created If the relation does not
     * exist in the KB. A KbException will be raised if a number is not mapped to any string in the KB.
     *
     * Note: the arguments should be encoded values by "constant()/variable" methods in the "Argument" class.
     *
     * @throws KbException Record arity does not match the relation; Number is not mapped to any string
     */
    public void addRecord(String relName, int[] record) throws KbException {
        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, record.length);
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

        addRecordHandler(relation, new Record(record));
    }

    public void addRecord(String relName, Record record) throws KbException {
        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, record.args.length);
        }

        int arity = relation.getArity();
        if (record.args.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
            ));
        }

        for (int arg: record.args) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, record);
    }

    /**
     * Add a record where arguments are name strings to the KB. The names will be converted to numerations (or be added
     * to the mapping first) before the record is added. A KbException will be raised If the relation does not exist in
     * the KB.
     *
     * @param relNum The numeration of the relation
     * @throws KbException The relation is not in the KB; Record arity does not match the relation.
     */
    public void addRecord(int relNum, String[] argNames) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (argNames.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", argNames.length, arity,
                    Arrays.toString(argNames)
            ));
        }

        int[] arg_nums = new int[argNames.length];
        for (int i = 0; i < arity; i++) {
            arg_nums[i] = Argument.constant(numMap.mapName(argNames[i]));
        }
        addRecordHandler(relation, new Record(arg_nums));
    }

    /**
     * Add a record where arguments are numbers to the KB.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecord(int relNum, int[] record) throws KbException {
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

        addRecordHandler(relation, new Record(record));
    }

    public void addRecord(int relNum, Record record) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        if (record.args.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
            ));
        }

        for (int arg: record.args) {
            if (null == numMap.num2Name(arg)) {
                throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
            }
        }

        addRecordHandler(relation, record);
    }

    /**
     * Add records. Relation will be created if not exist.
     *
     * @throws KbException Record arity does not match the relation
     */
    public void addRecords(String relName, String[][] argNamesArray) throws KbException {
        if (0 == argNamesArray.length) {
            return;
        }

        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, argNamesArray[0].length);
        }

        int arity = relation.getArity();
        for (String[] arg_names: argNamesArray) {
            if (arg_names.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", arg_names.length, arity,
                        Arrays.toString(arg_names)
                ));
            }

            int[] arg_nums = new int[arg_names.length];
            for (int i = 0; i < arity; i++) {
                arg_nums[i] = Argument.constant(numMap.mapName(arg_names[i]));
            }
            addRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Add records. Relation will be created if not exist.
     *
     * @throws KbException Record arity does not match the relation; Number is not mapped to any string
     */
    public void addRecords(String relName, int[][] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, records[0].length);
        }

        int arity = relation.getArity();
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

            addRecordHandler(relation, new Record(record));
        }
    }

    public void addRecords(String relName, Record[] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relName);
        if (null == relation) {
            relation = createRelation(relName, records[0].args.length);
        }

        int arity = relation.getArity();
        for (Record record: records) {
            if (record.args.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
                ));
            }

            for (int arg : record.args) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, record);
        }
    }

    /**
     * Add records.
     *
     * @throws KbException The relation is not in the KB; Record arity does not match the relation.
     */
    public void addRecords(int relNum, String[][] argNamesArray) throws KbException {
        if (0 == argNamesArray.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        for (String[] arg_names: argNamesArray) {
            if (arg_names.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", arg_names.length, arity,
                        Arrays.toString(arg_names)
                ));
            }

            int[] arg_nums = new int[arg_names.length];
            for (int i = 0; i < arity; i++) {
                arg_nums[i] = Argument.constant(numMap.mapName(arg_names[i]));
            }
            addRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Add records.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecords(int relNum, int[][] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
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

            addRecordHandler(relation, new Record(record));
        }
    }

    /**
     * Add records.
     *
     * @throws KbException Relation does not exist; Record arity does not match the relation; Number is not mapped to
     * any string
     */
    public void addRecords(int relNum, Record[] records) throws KbException {
        if (0 == records.length) {
            return;
        }

        KbRelation relation = getRelation(relNum);
        if (null == relation) {
            throw new KbException(String.format("Relation is not in the KB: %d", relNum));
        }

        int arity = relation.getArity();
        for (Record record: records) {
            if (record.args.length != arity) {
                throw new KbException(String.format(
                        "Record arity (%d) does not match the relation (%d): %s", record.args.length, arity, record
                ));
            }

            for (int arg : record.args) {
                if (null == numMap.num2Name(arg)) {
                    throw new KbException(String.format("Numeration not mapped in the KB: %d", arg));
                }
            }

            addRecordHandler(relation, record);
        }
    }

    /**
     * Add a record to the relation.
     *
     * @throws KbException Record arity does not match the relation
     */
    protected void addRecordHandler(KbRelation relation, Record record) throws KbException {
        relation.addRecord(record);
        for (int argument: record.args) {
            constants.add(Argument.decode(argument));
        }
    }

    /**
     * Remove a record from the relation. No exception is thrown if the relation is not in the KB nor the record is not
     * in the relation.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     */
    public void removeRecord(String relName, String[] argNames) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = Argument.constant(numMap.name2Num(argNames[i]));
            }
            removeRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(String relName, int[] record) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            removeRecordHandler(relation, new Record(record));
        }
    }

    public void removeRecord(String relName, Record record) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            removeRecordHandler(relation, record);
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(int relNum, String[] argNames) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = Argument.constant(numMap.name2Num(argNames[i]));
            }
            removeRecordHandler(relation, new Record(arg_nums));
        }
    }

    /**
     * Remove a record form the relation.
     */
    public void removeRecord(int relNum, int[] record) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            removeRecordHandler(relation, new Record(record));
        }
    }

    public void removeRecord(int relNum, Record record) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            removeRecordHandler(relation, record);
        }
    }

    protected void removeRecordHandler(KbRelation relation, Record record) {
        relation.removeRecord(record);
        for (int argument: record.args) {
            constants.remove(Argument.decode(argument));
        }
    }

    /**
     * Check whether a record is in the KB.
     *
     * @param relName The name of the relation
     * @param argNames The names of the arguments
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(String relName, String[] argNames) {
        KbRelation relation = getRelation(relName);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = Argument.constant(numMap.name2Num(argNames[i]));
            }
            return relation.hasRecord(new Record(arg_nums));
        }
        return false;
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(String relName, int[] record) {
        KbRelation relation = getRelation(relName);
        return null != relation && relation.hasRecord(new Record(record));
    }

    public boolean hasRecord(String relName, Record record) {
        KbRelation relation = getRelation(relName);
        return null != relation && relation.hasRecord(record);
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(int relNum, String[] argNames) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            int[] arg_nums = new int[argNames.length];
            for (int i = 0; i < arg_nums.length; i++) {
                arg_nums[i] = Argument.constant(numMap.name2Num(argNames[i]));
            }
            return relation.hasRecord(new Record(arg_nums));
        }
        return false;
    }

    /**
     * Check whether a record is in the KB.
     *
     * @return True if and only if the KB has the relation and the relation contains the record
     */
    public boolean hasRecord(int relNum, int[] record) {
        KbRelation relation = getRelation(relNum);
        return null != relation && relation.hasRecord(new Record(record));
    }

    public boolean hasRecord(int relNum, Record record) {
        KbRelation relation = getRelation(relNum);
        return null != relation && relation.hasRecord(record);
    }

    /**
     * Add a name string into the KB and assign the name a unique number.
     *
     * @param name The name string
     * @return Mapped number
     */
    public int mapName(String name) {
        return numMap.mapName(name);
    }

    /**
     * Remove the mapping of a name string in the KB.
     *
     * @param name The name string
     * @return The number for the name. 0 if the name is not mapped in the KB.
     */
    public int unmapName(String name) {
        return numMap.unmapName(name);
    }

    /**
     * Remove the mapping of the number 'num' in the KB.
     *
     * @param num The number
     * @return The mapped name of the number, NULL if the number is not mapped in the KB.
     */
    public String unmapNumeration(int num) {
        return numMap.unmapNumeration(num);
    }

    /**
     * Get the mapped name for number 'num'.
     *
     * @return The mapped name of the number, NULL if the number is not mapped in the KB.
     */
    public String num2Name(int num) {
        return numMap.num2Name(num);
    }

    /**
     * Get the mapped integer of a name string.
     *
     * @return The mapped number for the name. 0 if the name is not mapped in the KB.
     */
    public int name2Num(String name) {
        return numMap.name2Num(name);
    }

    public String getName() {
        return name;
    }

    public Collection<KbRelation> getRelations() {
        return relationMap.values();
    }

    public NumerationMap getNumerationMap() {
        return numMap;
    }

    public int totalMappings() {
        return numMap.totalMappings();
    }

    public int totalRelations() {
        return relationMap.size();
    }

    public int totalRecords() {
        int cnt = 0;
        for (KbRelation relation: relationMap.values()) {
            cnt += relation.totalRecords();
        }
        return cnt;
    }

    /**
     * Todo: Tidy up the mapping and records because there may be many mappings that are not used due to removal of
     * relations and records.
     */
    public void tidyUp() throws KbException {
        throw new KbException("Not Implemented");
    }

    /**
     * Set a record as entailed in the KB.
     */
    public void setAsEntailed(int relNum, int[] record) throws KbException {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            relation.entailRecord(new Record(record));
        }
    }

    /**
     * Check whether a record has been entailed in the KB.
     */
    public boolean recordIsEntailed(int relNum, int[] record) {
        KbRelation relation = getRelation(relNum);
        return null != relation && relation.recordIsEntailed(new Record(record));
    }

    /**
     * Calculate the promising constants of each relation.
     */
    public void updatePromisingConstants() {
        for (KbRelation relation: getRelations()) {
            relation.updatePromisingConstants();
        }
    }

    /**
     * Get the promising constants in the relation. NULL if the relation is not in the KB or the constants are not
     * calculated.
     */
    public int[][] getPromisingConstants(int relNum) {
        KbRelation relation = getRelation(relNum);
        if (null != relation) {
            return relation.getPromisingConstants();
        }
        return null;
    }

    /**
     * Get all constant numerations appeared in the KB.
     */
    public Set<Integer> getAllConstants() {
        return constants.distinctValues();
    }
}
