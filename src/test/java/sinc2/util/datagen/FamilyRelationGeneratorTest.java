package sinc2.util.datagen;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sinc2.kb.KbException;
import sinc2.kb.NumeratedKb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FamilyRelationGeneratorTest {
    static final String MEM_DIR = "/dev/shm";

    static String BASE_DIR = null;

    @BeforeAll
    static void createTestDir() throws IOException {
        File base_dir_file = Paths.get(MEM_DIR, "FamilyRelationGeneratorTest").toFile();
        if (!base_dir_file.exists() && !base_dir_file.mkdirs()) {
            throw new IOException("Test directory creation failed");
        }
        BASE_DIR = base_dir_file.getAbsolutePath();
    }

    @AfterAll
    static void deleteTestDir() {
        removeDir(BASE_DIR);
    }

    static void removeDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (null != files) {
            for (File f : files) {
                if (f.isDirectory()) {
                    removeDir(f.getAbsolutePath().toString());
                } else {
                    f.delete();
                }
            }
            dir.delete();
        }
    }


    @Test
    void testGenTiny() throws IOException, KbException {
        int families = 10;
        String kb_name = "Ft";
        FamilyRelationGenerator.generateTiny(BASE_DIR, kb_name, families, 0);
        NumeratedKb kb = new NumeratedKb(kb_name, BASE_DIR);
        assertEquals(3 + 4 * families + 2, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8 * families, kb.totalRecords());

        BufferedReader reader = new BufferedReader(new FileReader(Paths.get(BASE_DIR, kb_name, "triples.meta").toFile()));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t");
            assertTrue(kb.hasRecord(components[0], new String[]{components[1], components[2]}));
        }
        reader.close();
    }

    @Test
    void testGenSimple() throws IOException, KbException {
        int families = 10;
        String kb_name = "Fs";
        FamilyRelationGenerator.generateSimple(BASE_DIR, kb_name, families, 0);
        NumeratedKb kb = new NumeratedKb(kb_name, BASE_DIR);
        assertEquals(4 + 8 * families + 2, kb.totalMappings());
        assertEquals(4, kb.totalRelations());
        assertEquals(24 * families, kb.totalRecords());

        BufferedReader reader = new BufferedReader(new FileReader(Paths.get(BASE_DIR, kb_name, "triples.meta").toFile()));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t");
            assertTrue(kb.hasRecord(components[0], new String[]{components[1], components[2]}));
        }
        reader.close();
    }

    @Test
    void testGenMedium() throws IOException, KbException {
        int families = 10;
        String kb_name = "Fm";
        FamilyRelationGenerator.generateMedium(BASE_DIR, kb_name, families, 0);
        NumeratedKb kb = new NumeratedKb(kb_name, BASE_DIR);
        assertEquals(9 + 14 * families + 2, kb.totalMappings());
        assertEquals(9, kb.totalRelations());
        assertEquals(110 * families, kb.totalRecords());

        BufferedReader reader = new BufferedReader(new FileReader(Paths.get(BASE_DIR, kb_name, "triples.meta").toFile()));
        String line;
        while (null != (line = reader.readLine())) {
            String[] components = line.split("\t");
            assertTrue(kb.hasRecord(components[0], new String[]{components[1], components[2]}));
        }
        reader.close();
    }
}