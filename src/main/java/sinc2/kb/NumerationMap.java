package sinc2.kb;

import sinc2.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The class for the numeration map between name strings and numerations. The applicable integers are the positive ones.
 *
 * The numeration map can be dumped into local storage as multiple regular files, each of which contains no more than
 * 'MAX_MAP_ENTRIES' (default 1M) entries. The files are named by `map<#num>.tsv` by default. `#num` is the order of the
 * files, starting from 'MAP_FILE_NUMERATION_START' (default 1).
 *
 * The file contains two columns separated by the tabular char (`'\t'`):
 *   - The 1st column is the string for relation/entity names;
 *   - The 2nd column is an integer (in Hexadecimal) that is assigned as the identifier for the string;
 *   - Each row denotes a mapping between the name and the integer;
 *   - The mapping should be bijective, and the integers are continuous starting from 1.
 *
 * @since 2.0
 */
public class NumerationMap {
    /** The starting number of map files. */
    public static final int MAP_FILE_NUMERATION_START = 1;

    /** The maximum number of map entries in one map file. */
    public static final int MAX_MAP_ENTRIES = 1000000;

    /** The number that should not be mapped to any name string */
    public static final int NUM_NULL = 0;

    /* The map from name strings to integers */
    protected Map<String, Integer> numMap = new HashMap<>();

    /* The map from integers to name strings */
    protected List<String> numArray;

    /* The set of numbers which are smaller than the maximum mapped integer but are not mapped yet, organized as a min-heap */
    protected PriorityQueue<Integer> freeNums = new PriorityQueue<>();

    /**
     * Get the map file path.
     *
     * @param kbPath The base path of the KB where the map files locate.
     * @param num The number of the map file.
     * @return The path to the map file.
     */
    public static Path getMapFilePath(String kbPath, int num) {
        return Paths.get(kbPath, String.format("map%d.tsv", num));
    }

    /**
     * Create an empty numeration map.
     */
    public NumerationMap() {
        loadHandler(null);
    }

    /**
     * Initialize a numeration map by an external mapping.
     *
     * @param map A mapping from names to numerations.
     */
    public NumerationMap(Map<String, Integer> map) {
        /* Load the string-to-integer map */
        this.numMap = new HashMap<>(map);

        /* Create the integer-to-string map */
        int capacity = Collections.max(map.values()) + 1;
        numArray = new ArrayList<>(capacity);
        while (numArray.size() < capacity) {
            numArray.add(null);
        }
        for (Map.Entry<String, Integer> entry: numMap.entrySet()) {
            numArray.set(entry.getValue(), entry.getKey());
        }

        /* Find the free integers */
        for (int i = 1; i < capacity; i++) {
            if (null == numArray.get(i)) {
                freeNums.add(i);
            }
        }
    }

    /**
     * Load the numeration map from map files in the KB path.
     *
     * @param kbPath The base path of the KB where the map files locate.
     */
    public NumerationMap(String kbPath) {
        /* Load the string-to-integer map */
        File kb_dir = new File(kbPath);
        File[] map_files = kb_dir.listFiles((dir, name) -> name.matches("map[0-9]+.tsv$"));
        loadHandler(map_files);
    }

    /**
     * Load the map from a specific file.
     *
     * @param kbPath The base path of the KB where the map files locate.
     * @param fileName The name of the file
     */
    public NumerationMap(String kbPath, String fileName) {
        loadHandler(new File[]{Paths.get(kbPath, fileName).toFile()});
    }

    /**
     * Load the numeration map from files. If no file is given, create an empty map.
     *
     * @param mapFiles The files that should be loaded
     */
    public void loadHandler(File[] mapFiles) {
        if (null == mapFiles || 0 == mapFiles.length) {
            /* Initialize as an empty map */
            numArray = new ArrayList<>();
            numArray.add(null);  // The object at index 0 should not be used.
            return;
        }
        int max_num = 0;
        for (File map_file: mapFiles) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(map_file));
                String line;
                while (null != (line = reader.readLine())) {
                    String[] components = line.split("\t");
                    String name = components[0];
                    int num = Integer.parseInt(components[1], 16);
                    numMap.put(name, num);
                    max_num = Math.max(max_num, num);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Create the integer-to-string map */
        int capacity = max_num + 1;
        numArray = new ArrayList<>(capacity);
        while (numArray.size() < capacity) {
            numArray.add(null);
        }
        for (Map.Entry<String, Integer> entry: numMap.entrySet()) {
            numArray.set(entry.getValue(), entry.getKey());
        }

        /* Find the free integers */
        for (int i = 1; i < capacity; i++) {
            if (null == numArray.get(i)) {
                freeNums.add(i);
            }
        }
    }

    /**
     * Add a name string into the map and assign the name a unique number. The returned integer will always be the smallest
     * one available for use. If the name has already been mapped, return the mapped numeration.
     *
     * @param name The new name string
     * @return The mapped integer for the name
     */
    public int mapName(String name) {
        Integer num = numMap.get(name);
        if (null != num) {
            return num;
        }

        if (!freeNums.isEmpty()) {
            num = freeNums.poll();
            numArray.set(num, name);
        } else {
            num = numArray.size();
            numArray.add(name);
        }
        numMap.put(name, num);
        return num;
    }

    /**
     * Remove the mapping of a name string in the map.
     *
     * @param name The name string that should be removed
     * @return The number that was mapped to the name. 0 if the name is not mapped in the map.
     */
    public int unmapName(String name) {
        Integer num = numMap.remove(name);
        if (null == num) {
            return NUM_NULL;
        }
        numArray.set(num, null);
        freeNums.add(num);
        return num;
    }

    /**
     * Remove the mapping of an integer in the map.
     *
     * @param num The integer that should be unmapped
     * @return The mapped name string of the integer. NULL if the number is not mapped in the map.
     */
    public String unmapNumeration(int num) {
        if (0 < num && numArray.size() > num && null != numArray.get(num)) {
            String name = numArray.get(num);
            numArray.set(num, null);
            numMap.remove(name);
            freeNums.add(num);
            return name;
        }
        return null;
    }

    /**
     * Get the mapped name string of an integer.
     *
     * @return The mapped name of the number, 'None' if the number is not mapped in the KB.
     */
    public String num2Name(int num) {
        if (0 < num && numArray.size() > num) {
            return numArray.get(num);
        }
        return null;
    }

    /**
     * Get the mapped integer of a name string.
     *
     * @return The mapped number for the name. 0 if the name is not mapped in the KB.
     */
    public int name2Num(String name) {
        Integer num = numMap.get(name);
        return (null == num) ? NumerationMap.NUM_NULL : num;
    }

    /**
     * Dump the numeration map to local files.
     *
     * @param kbPath The path where the map files will be stored.
     * @throws FileNotFoundException Thrown when the map files failed to be created
     */
    public void dump(String kbPath) throws FileNotFoundException {
        dump(kbPath, MAP_FILE_NUMERATION_START, MAX_MAP_ENTRIES);
    }

    /**
     * Dump the numeration map to local files.
     *
     * @param kbPath The path where the map files will be stored
     * @param startMapNum The start number of the map files
     * @param maxEntries The maximum number of entries a map file contains
     * @throws FileNotFoundException Thrown when the map files failed to be created
     */
    public void dump(String kbPath, final int startMapNum, final int maxEntries) throws FileNotFoundException {
        int map_num = startMapNum;
        PrintWriter writer = new PrintWriter(getMapFilePath(kbPath, map_num).toFile());
        int records_cnt = 0;
        for (Map.Entry<String, Integer> entry: numMap.entrySet()) {
            if (maxEntries <= records_cnt) {
                writer.close();
                map_num++;
                records_cnt = 0;
                writer = new PrintWriter(getMapFilePath(kbPath, map_num).toFile());
            }
            writer.printf("%s\t%x\n", entry.getKey(), entry.getValue());
            records_cnt++;
        }
        writer.close();
    }

    /**
     * Dump the numeration mapping into a single file, the name of which is "fileName".
     * @param kbPath The path where the map files will be stored
     * @param fileName The mapping file name
     * @throws FileNotFoundException Thrown when the map files failed to be created
     */
    public void dump(String kbPath, String fileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(Paths.get(kbPath, fileName).toFile());
        for (Map.Entry<String, Integer> entry: numMap.entrySet()) {
            writer.printf("%s\t%x\n", entry.getKey(), entry.getValue());
        }
        writer.close();
    }

    /**
     * Return the total number of mapping entries.
     */
    public int totalMappings() {
        return numMap.size();
    }

    /**
     * Get an iterator that iterates over mapping entries from name strings to mapped integers.
     */
    public Iterator<Map.Entry<String, Integer>> iterName2Num() {
        return numMap.entrySet().iterator();
    }

    /**
     * Get an iterator that iterates over mapping entries from integers to mapped name strings.
     */
    public Iterator<Pair<Integer, String>> iterNum2Name() {
        class Int2NameItr implements Iterator<Pair<Integer, String>> {
            protected final List<String> nameArray;
            protected int idx = 0;

            public Int2NameItr(List<String> nameArray) {
                this.nameArray = nameArray;
                updateIdx();
            }

            protected void updateIdx() {
                while (nameArray.size() > idx && null == nameArray.get(idx)) {
                    idx++;
                }
            }

            @Override
            public boolean hasNext() {
                return nameArray.size() > idx;
            }

            @Override
            public Pair<Integer, String> next() {
                Pair<Integer, String> pair = new Pair<>(idx, nameArray.get(idx));
                idx++;
                updateIdx();
                return pair;
            }
        }
        return new Int2NameItr(numArray);
    }
}
