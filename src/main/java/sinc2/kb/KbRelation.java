package sinc2.kb;

import sinc2.common.Argument;
import sinc2.util.ArrayOperation;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class for the numeration representation of the records in a relation.
 *
 * A relation can be dumped into local file system as a regular file. The complies with the following format:
 *   - The name of the files should be `<relation name>_<arity>_<#records>.rel`.
 *   - The files are binary files, each of which only contains `arity`x`#records` integers.
 *   - The integers stored in one `.rel` file is row oriented, each row corresponds to one record in the relation. The
 *     records are stored in the file in order, i.e., in the order of: 1st row 1st col, 1st row 2nd col, ..., ith row
 *     jth col, ith row (j+1)th col, ...
 *
 * Todo: The <#record> should be canceled as it is not necessary for loading records
 *
 * @since 2.0
 */
public class KbRelation implements Iterable<Record> {

    /** A regex pattern used to parse the relation file name */
    protected static final Pattern REL_FILE_NAME_PATTERN = Pattern.compile("(.+)_([0-9]+)_([0-9]+).rel$");

    /** The threshold for pruning useful constants */
    public static double MIN_CONSTANT_COVERAGE = 0.25;

    /** The name of the relation */
    protected final String name;
    /** The numeration of the relation name */
    protected final int numeration;
    /** The arity of the relation */
    protected final int arity;
    /** The set of the records */
    protected final Set<Record> records = new HashSet<>();
    /** The set of entailed records */
    protected final Set<Record> entailedRecords = new HashSet<>();
    /** The index of each argument value */
    protected final Map<Integer, Set<Record>>[] argumentIndices;
    /** Promising constants for each argument */
    protected int[][] promisingConstants = null;

    /**
     * Get the relation file path.
     *
     * @param kbPath The base path of the KB where the relation file locates.
     * @param relName The name of the relation
     * @param arity The arity of the relation
     * @param records the number of records in the relation
     * @return The path to the relation file
     */
    public static Path getRelFilePath(String kbPath, String relName, int arity, int records) {
        return Paths.get(kbPath, String.format("%s_%d_%d.rel", relName, arity, records));
    }

    /**
     * The class for the parsed information of a relation file name
     */
    static class RelationInfo {
        public final String name;
        public final int arity;
        public final int totalRecords;

        public RelationInfo(String name, int arity, int totalRecords) {
            this.name = name;
            this.arity = arity;
            this.totalRecords = totalRecords;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelationInfo that = (RelationInfo) o;
            return arity == that.arity && totalRecords == that.totalRecords && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arity, totalRecords);
        }
    }

    /**
     * Parse the file name of a relation to the components: relation name, arity, total records.
     */
    public static RelationInfo parseRelFilePath(String relFileName) {
        Matcher matcher = REL_FILE_NAME_PATTERN.matcher(relFileName);
        if (matcher.find()) {
            return new RelationInfo(
                    matcher.group(1), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))
            );
        }
        return null;
    }

    /**
     * Create an empty relation
     *
     * @param numeration The numeration of the relation name
     */
    public KbRelation(String name, int numeration, int arity) {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.argumentIndices = new Map[arity];
        for (int i = 0; i < arity; i++) {
            argumentIndices[i] = new HashMap<>();
        }
    }

    /**
     * Load a single relation from the local file system. If the 'numMap' is not NULL, every loaded numeration is
     * checked for validness in the map.
     *
     * @param name The name of the relation
     * @param numeration The numeration of the relation name
     * @param arity The arity of the relation
     * @param totalRecords The total number of records in the relation
     * @param kbPtah The path to the KB, where the relation file is located
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    public KbRelation(String name, int numeration, int arity, int totalRecords, String kbPtah, NumerationMap map)
            throws IOException, KbException {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.argumentIndices = new Map[arity];
        for (int i = 0; i < arity; i++) {
            argumentIndices[i] = new HashMap<>();
        }

        File rel_file = getRelFilePath(kbPtah, name, arity, totalRecords).toFile();
        loadHandler(rel_file, map);
    }

    /**
     * Load a single relation from a local file. If the 'numMap' is not NULL, every loaded numeration is checked for
     * validness in the map.
     *
     * @param name The name of the relation
     * @param numeration The numeration of the relation name
     * @param arity The arity of the relation
     * @param fileName The name of the file
     * @param kbPtah The path to the KB, where the relation file is located
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    public KbRelation(String name, int numeration, int arity, String fileName, String kbPtah, NumerationMap map)
            throws IOException, KbException {
        this.name = name;
        this.numeration = numeration;
        this.arity = arity;
        this.argumentIndices = new Map[arity];
        for (int i = 0; i < arity; i++) {
            argumentIndices[i] = new HashMap<>();
        }

        File rel_file = Paths.get(kbPtah, fileName).toFile();
        loadHandler(rel_file, map);
    }

    /**
     * Load a single relation from the local file system. If the 'numMap' is not NULL, every loaded numeration is
     * checked for validness in the map.
     *
     * @param file The relation file
     * @param map The numeration map for validness check
     * @throws IOException File read fails
     * @throws KbException 'map' is not NULL and a loaded numeration is not mapped
     */
    protected void loadHandler(File file, NumerationMap map) throws IOException, KbException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[Integer.BYTES];
        int read_args;
        if (null == map) {
            while (true) {
                int[] args = new int[arity];
                for (read_args = 0; read_args < arity && Integer.BYTES == fis.read(buffer); read_args++) {
                    args[read_args] = LittleEndianIntIO.byteArray2LeInt(buffer);
                }
                if (read_args < arity) {
                    break;
                }
                addRecord(new Record(args));
            }
        } else {
            while (true) {
                int[] args = new int[arity];
                for (read_args = 0; read_args < arity && Integer.BYTES == fis.read(buffer); read_args++) {
                    args[read_args] = LittleEndianIntIO.byteArray2LeInt(buffer);
                    if (null == map.num2Name(Argument.decode(args[read_args]))) {
                        fis.close();
                        throw new KbException(String.format("Loaded numeration is not mapped: %d", args[read_args]));
                    }
                }
                if (read_args < arity) {
                    break;
                }
                addRecord(new Record(args));
            }
        }
        fis.close();
    }

    /**
     * Add a record to the relation.
     *
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public void addRecord(Record record) throws KbException {
        if (record.args.length != arity) {
            throw new KbException(String.format(
                    "Record arity (%d) does not match the relation (%d)", record.args.length, arity
            ));
        }
        records.add(record);

        /* Update the argument index */
        for (int arg_idx = 0; arg_idx < arity; arg_idx++) {
            Map<Integer, Set<Record>> index = argumentIndices[arg_idx];
            index.compute(record.args[arg_idx], (argument, records) -> {
                if (null == records) {
                    records = new HashSet<>();
                }
                records.add(record);
                return records;
            });
        }
    }

    /**
     * Add a batch of records to the relation.
     *
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public void addRecords(Iterable<Record> records) throws KbException {
        for (Record record: records) {
            addRecord(record);
        }
    }

    /**
     * Add a batch of records to the relation.
     *
     * @throws KbException The arity of the record does not match that of the relation.
     */
    public void addRecords(Record[] records) throws KbException {
        for (Record record: records) {
            addRecord(record);
        }
    }

    /**
     * Remove a record from the relation
     */
    public void removeRecord(Record record) {
        records.remove(record);
    }

    /**
     * Mark a record as entailed. The record will not be marked if it is not in the KB.
     */
    public void entailRecord(Record record) {
        if (hasRecord(record)) {
            entailedRecords.add(record);
        }
    }

    /**
     * Check if a record has been entailed.
     */
    public boolean recordIsEntailed(Record record) {
        return entailedRecords.contains(record);
    }

    /**
     * Update the promising constants according to current records.
     */
    public void updatePromisingConstants() {
        promisingConstants = new int[arity][];
        int threshold = (int) Math.ceil(records.size() * MIN_CONSTANT_COVERAGE);
        for (int i = 0; i < arity; i++) {
            Map<Integer, Set<Record>> argument_index = argumentIndices[i];
            List<Integer> promising_constants = new ArrayList<>();
            for (Map.Entry<Integer, Set<Record>> entry: argument_index.entrySet()) {
                if (threshold <= entry.getValue().size()) {
                    promising_constants.add(Argument.decode(entry.getKey()));
                }
            }
            promisingConstants[i] = ArrayOperation.toArray(promising_constants);
        }
    }

    public int[][] getPromisingConstants() {
        return promisingConstants;
    }

    /**
     * Write the relation to a '.rel' file.
     *
     * @param kbPath The path to the KB, where the relation file should be located.
     * @throws IOException File I/O operation error
     */
    public void dump(String kbPath) throws IOException {
        dumpHandler(getRelFilePath(kbPath, name, arity, records.size()).toFile());
    }

    /**
     * Write the relation to a file.
     *
     * @param kbPath The path to the KB, where the relation file should be located.
     * @param fileName The customized file name.
     * @throws IOException File I/O operation error
     */
    public void dump(String kbPath, String fileName) throws IOException {
        dumpHandler(Paths.get(kbPath, fileName).toFile());
    }

    protected void dumpHandler(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        for (Record record: records) {
            for (int i: record.args) {
                fos.write(LittleEndianIntIO.leInt2ByteArray(i));
            }
        }
        fos.close();
    }

    public boolean hasRecord(Record record) {
        return records.contains(record);
    }

    public Iterator<Record> iterator() {
        return records.iterator();
    }

    public String getName() {
        return name;
    }

    public int getNumeration() {
        return numeration;
    }

    public int getArity() {
        return arity;
    }

    public Set<Record> getRecords() {
        return records;
    }

    public int totalRecords() {
        return records.size();
    }

    public Map<Integer, Set<Record>>[] getArgumentIndices() {
        return argumentIndices;
    }
}
