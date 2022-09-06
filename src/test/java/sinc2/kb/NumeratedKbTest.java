package sinc2.kb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumeratedKbTest {

    static TestKbManager testKbManager;

    @BeforeAll
    static void setupKb() throws IOException {
        testKbManager = new TestKbManager();
    }

    @AfterAll
    static void removeKb() {
        testKbManager.cleanUpKb();
    }


    @Test
    void testCreateEmpty() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        assertEquals("test", kb.getName());
        assertEquals(0, kb.totalMappings());
        assertEquals(0, kb.totalRelations());
        assertEquals(0, kb.totalRecords());

        kb.addRecord("family", new String[]{"alice", "bob", "catherine"});
        assertEquals(1, kb.getRelation("family").getNumeration());
        kb.addRecord(1, new String[]{"diana", "erick", "frederick"});
        assertEquals(8, kb.mapName("gabby"));
        assertEquals(9, kb.mapName("harry"));
        assertEquals(10, kb.mapName("isaac"));
        assertEquals(11, kb.mapName("jena"));
        assertEquals(12, kb.mapName("kyle"));
        assertEquals(13, kb.mapName("lily"));
        kb.addRecord("family", new Record(new int[]{8, 9, 10}));
        kb.addRecord(1, new Record(new int[]{11, 12, 13}));

        assertEquals(13, kb.totalMappings());
        assertEquals(1, kb.totalRelations());
        assertEquals(4, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord(1, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord(1, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord(1, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord(1, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord("family", new Record(new int[]{2, 3, 4})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{5, 6, 7})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{8, 9, 10})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{11, 12, 13})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{2, 3, 4})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{5, 6, 7})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{8, 9, 10})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{11, 12, 13})));
    }

    @Test
    void testRead() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(12, kb.totalRecords());

        assertTrue(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord(1, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb.hasRecord(1, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb.hasRecord(1, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb.hasRecord(1, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord("family", new Record(new int[]{13, 14, 15})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{4, 5, 6})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{7, 8, 9})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{10, 11, 12})));
        assertTrue(kb.hasRecord(1, new Record(new int[]{13, 14, 15})));

        assertTrue(kb.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord(2, new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord(2, new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord(2, new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord(2, new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{13, 15})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{13, 15})));

        assertTrue(kb.hasRecord("father", new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord("father", new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord("father", new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord(3, new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord(3, new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord(3, new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord(3, new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord("father", new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{16, 17})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{16, 17})));
    }

    @Test
    void testWrite() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, false);
        String tmp_dir_path = testKbManager.createTmpDir();
        kb.dump(tmp_dir_path);
        NumeratedKb kb2 = new NumeratedKb(kb.getName(), tmp_dir_path, false);

        assertEquals(testKbManager.getKbName(), kb2.getName());
        assertEquals(17, kb2.totalMappings());
        assertEquals(3, kb2.totalRelations());
        assertEquals(12, kb2.totalRecords());

        assertTrue(kb2.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb2.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb2.hasRecord("family", new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb2.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb2.hasRecord(1, new String[]{"alice", "bob", "catherine"}));
        assertTrue(kb2.hasRecord(1, new String[]{"diana", "erick", "frederick"}));
        assertTrue(kb2.hasRecord(1, new String[]{"gabby", "harry", "isaac"}));
        assertTrue(kb2.hasRecord(1, new String[]{"jena", "kyle", "lily"}));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertTrue(kb2.hasRecord("family", new Record(new int[]{13, 14, 15})));
        assertTrue(kb2.hasRecord(1, new Record(new int[]{4, 5, 6})));
        assertTrue(kb2.hasRecord(1, new Record(new int[]{7, 8, 9})));
        assertTrue(kb2.hasRecord(1, new Record(new int[]{10, 11, 12})));
        assertTrue(kb2.hasRecord(1, new Record(new int[]{13, 14, 15})));

        assertTrue(kb2.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb2.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb2.hasRecord(2, new String[]{"alice", "catherine"}));
        assertTrue(kb2.hasRecord(2, new String[]{"diana", "frederick"}));
        assertTrue(kb2.hasRecord(2, new String[]{"gabby", "isaac"}));
        assertTrue(kb2.hasRecord(2, new String[]{"jena", "lily"}));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb2.hasRecord("mother", new Record(new int[]{13, 15})));
        assertTrue(kb2.hasRecord(2, new Record(new int[]{4, 6})));
        assertTrue(kb2.hasRecord(2, new Record(new int[]{7, 9})));
        assertTrue(kb2.hasRecord(2, new Record(new int[]{10, 12})));
        assertTrue(kb2.hasRecord(2, new Record(new int[]{13, 15})));

        assertTrue(kb2.hasRecord("father", new String[]{"bob", "catherine"}));
        assertTrue(kb2.hasRecord("father", new String[]{"erick", "frederick"}));
        assertTrue(kb2.hasRecord("father", new String[]{"harry", "isaac"}));
        assertTrue(kb2.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertTrue(kb2.hasRecord(3, new String[]{"bob", "catherine"}));
        assertTrue(kb2.hasRecord(3, new String[]{"erick", "frederick"}));
        assertTrue(kb2.hasRecord(3, new String[]{"harry", "isaac"}));
        assertTrue(kb2.hasRecord(3, new String[]{"marvin", "nataly"}));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{5, 6})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{8, 9})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{11, 12})));
        assertTrue(kb2.hasRecord("father", new Record(new int[]{16, 17})));
        assertTrue(kb2.hasRecord(3, new Record(new int[]{5, 6})));
        assertTrue(kb2.hasRecord(3, new Record(new int[]{8, 9})));
        assertTrue(kb2.hasRecord(3, new Record(new int[]{11, 12})));
        assertTrue(kb2.hasRecord(3, new Record(new int[]{16, 17})));
    }

    @Test
    void testCreateRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, false);
        KbRelation relation = kb.createRelation("rel", 2);
        assertEquals(18, kb.totalMappings());
        assertEquals(4, kb.totalRelations());
        assertEquals(12, kb.totalRecords());
        assertEquals("rel", relation.getName());
        assertEquals(2, relation.getArity());
        assertEquals(18, relation.getNumeration());
        assertEquals(0, relation.totalRecords());
        assertSame(relation, kb.getRelation("rel"));
        assertSame(relation, kb.getRelation(18));

        kb.addRecord("rel2", new String[]{"a", "b", "c"});
        kb.addRecord("rel2", new Record(new int[]{4, 5, 6}));
        assertEquals(22, kb.totalMappings());
        assertEquals(5, kb.totalRelations());
        assertEquals(14, kb.totalRecords());
        assertNotNull(kb.getRelation("rel2"));
        assertNotNull(kb.getRelation(19));
    }

    @Test
    void testLoadRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR);
        KbRelation relation = new KbRelation("reflex", 0, 2);
        relation.addRecord(new Record(new int[]{4, 4}));
        relation.addRecord(new Record(new int[]{5, 5}));
        relation.addRecord(new Record(new int[]{6, 6}));
        relation.dump(TestKbManager.MEM_DIR);
        testKbManager.appendTmpFile(KbRelation.getRelFilePath(TestKbManager.MEM_DIR, "reflex", 2, 3).toAbsolutePath().toString());

        relation = kb.loadRelation(TestKbManager.MEM_DIR, "reflex", 2, 3, true);
        assertEquals(18, kb.totalMappings());
        assertEquals(4, kb.totalRelations());
        assertEquals(15, kb.totalRecords());
        assertEquals("reflex", relation.getName());
        assertEquals(2, relation.getArity());
        assertEquals(18, relation.getNumeration());
        assertEquals(3, relation.totalRecords());
        assertNotNull(kb.getRelation("reflex"));
        assertNotNull(kb.getRelation(18));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{4, 4})));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{5, 5})));
        assertTrue(kb.hasRecord("reflex", new Record(new int[]{6, 6})));

        relation = new KbRelation("reflex2", 0, 2);
        relation.addRecord(new Record(new int[]{7, 7}));
        relation.addRecord(new Record(new int[]{99, 99}));
        relation.addRecord(new Record(new int[]{8, 8}));
        relation.dump(TestKbManager.MEM_DIR);
        testKbManager.appendTmpFile(KbRelation.getRelFilePath(TestKbManager.MEM_DIR, "reflex2", 2, 3).toAbsolutePath().toString());
        assertThrows(KbException.class, () -> kb.loadRelation(TestKbManager.MEM_DIR, "reflex2", 2, 3, true));
    }

    @Test
    void testDeleteRelation() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.deleteRelation(1);

        assertEquals(testKbManager.getKbName(), kb.getName());
        assertEquals(17, kb.totalMappings());
        assertEquals(2, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        assertFalse(kb.hasRecord("family", new String[]{"alice", "bob", "catherine"}));
        assertFalse(kb.hasRecord("family", new String[]{"diana", "erick", "frederick"}));
        assertFalse(kb.hasRecord("family", new String[]{"harry", "gabby", "isaac"}));
        assertFalse(kb.hasRecord("family", new String[]{"jena", "kyle", "lily"}));
        assertFalse(kb.hasRecord(1, new String[]{"alice", "bob", "catherine"}));
        assertFalse(kb.hasRecord(1, new String[]{"diana", "erick", "frederick"}));
        assertFalse(kb.hasRecord(1, new String[]{"harry", "gabby", "isaac"}));
        assertFalse(kb.hasRecord(1, new String[]{"jena", "kyle", "lily"}));
        assertFalse(kb.hasRecord("family", new Record(new int[]{4, 5, 6})));
        assertFalse(kb.hasRecord("family", new Record(new int[]{7, 8, 9})));
        assertFalse(kb.hasRecord("family", new Record(new int[]{10, 11, 12})));
        assertFalse(kb.hasRecord("family", new Record(new int[]{13, 14, 15})));
        assertFalse(kb.hasRecord(1, new Record(new int[]{4, 5, 6})));
        assertFalse(kb.hasRecord(1, new Record(new int[]{7, 8, 9})));
        assertFalse(kb.hasRecord(1, new Record(new int[]{10, 11, 12})));
        assertFalse(kb.hasRecord(1, new Record(new int[]{13, 14, 15})));

        assertTrue(kb.hasRecord("mother", new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord("mother", new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord("mother", new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord("mother", new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord(2, new String[]{"alice", "catherine"}));
        assertTrue(kb.hasRecord(2, new String[]{"diana", "frederick"}));
        assertTrue(kb.hasRecord(2, new String[]{"gabby", "isaac"}));
        assertTrue(kb.hasRecord(2, new String[]{"jena", "lily"}));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord("mother", new Record(new int[]{13, 15})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{4, 6})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{7, 9})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{10, 12})));
        assertTrue(kb.hasRecord(2, new Record(new int[]{13, 15})));

        assertTrue(kb.hasRecord("father", new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord("father", new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord("father", new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord("father", new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord(3, new String[]{"bob", "catherine"}));
        assertTrue(kb.hasRecord(3, new String[]{"erick", "frederick"}));
        assertTrue(kb.hasRecord(3, new String[]{"harry", "isaac"}));
        assertTrue(kb.hasRecord(3, new String[]{"marvin", "nataly"}));
        assertTrue(kb.hasRecord("father", new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord("father", new Record(new int[]{16, 17})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{5, 6})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{8, 9})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{11, 12})));
        assertTrue(kb.hasRecord(3, new Record(new int[]{16, 17})));

        assertNull(kb.deleteRelation("family"));
    }

    @Test
    void testAddRecord() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.addRecord("family", new String[]{"o", "p", "q"});
        kb.addRecord(1, new String[]{"o", "o", "o"});
        assertEquals(19, kb.name2Num("p"));
        kb.addRecord("family", new int[]{19, 19, 19});
        assertEquals(20, kb.name2Num("q"));
        kb.addRecord(1, new int[]{20, 20, 20});

        assertEquals(20, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(16, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"o", "p", "q"}));
        assertTrue(kb.hasRecord("family", new String[]{"o", "o", "o"}));
        assertTrue(kb.hasRecord("family", new String[]{"p", "p", "p"}));
        assertTrue(kb.hasRecord("family", new String[]{"q", "q", "q"}));

        kb.addRecord("family", new String[]{"o", "o", "o"});
        assertThrows(KbException.class, () -> kb.addRecord("family", new String[]{"o"}));
        assertThrows(KbException.class, () -> kb.addRecord(1, new String[]{"o"}));
        assertThrows(KbException.class, () -> kb.addRecord("family", new int[]{18}));
        assertThrows(KbException.class, () -> kb.addRecord(1, new int[]{18}));
        assertThrows(KbException.class, () -> kb.addRecord(1, new int[]{200, 20, 20}));

        assertEquals(20, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(16, kb.totalRecords());
    }

    @Test
    void testAddRecords() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.addRecords("family", new String[][]{
                new String[]{"o", "o", "o"}, new String[]{"oo", "oo", "oo"}
        });
        kb.addRecords(1, new String[][]{
                new String[]{"p", "p", "p"}, new String[]{"pp", "pp", "pp"}
        });
        assertEquals(22, kb.mapName("q"));
        assertEquals(23, kb.mapName("qq"));
        kb.addRecords("family", new int[][]{new int[]{22, 22, 22}, new int[]{23, 23, 23}});
        assertEquals(24, kb.mapName("r"));
        assertEquals(25, kb.mapName("rr"));
        kb.addRecords(1, new int[][]{new int[]{24, 24, 24}, new int[]{25, 25, 25}});

        assertEquals(25, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(20, kb.totalRecords());
        assertTrue(kb.hasRecord("family", new String[]{"o", "o", "o"}));
        assertTrue(kb.hasRecord("family", new String[]{"oo", "oo", "oo"}));
        assertTrue(kb.hasRecord("family", new String[]{"p", "p", "p"}));
        assertTrue(kb.hasRecord("family", new String[]{"pp", "pp", "pp"}));
        assertTrue(kb.hasRecord("family", new String[]{"q", "q", "q"}));
        assertTrue(kb.hasRecord("family", new String[]{"qq", "qq", "qq"}));
        assertTrue(kb.hasRecord("family", new String[]{"r", "r", "r"}));
        assertTrue(kb.hasRecord("family", new String[]{"rr", "rr", "rr"}));

        kb.addRecords("family", new String[][]{new String[]{"o", "o", "o"}});
        assertThrows(KbException.class, () -> kb.addRecords("family", new String[][]{new String[]{"o"}}));
        assertThrows(KbException.class, () -> kb.addRecords(1, new String[][]{new String[]{"o"}}));
        assertThrows(KbException.class, () -> kb.addRecords("family", new int[][]{new int[]{18}}));
        assertThrows(KbException.class, () -> kb.addRecords(1, new int[][]{new int[]{18}}));
        assertThrows(KbException.class, () -> kb.addRecords(1, new int[][]{new int[]{200, 20, 20}}));

        assertEquals(25, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(20, kb.totalRecords());

        kb.addRecords("rel", new String[][]{new String[]{"o", "o"}, new String[]{"p", "p"}});
        kb.addRecords("rel2", new int[][]{new int[]{1}, new int[]{2}});

        assertEquals(27, kb.totalMappings());
        assertEquals(5, kb.totalRelations());
        assertEquals(24, kb.totalRecords());
    }

    @Test
    void testRemoveRecord() throws KbException, IOException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.removeRecord("mother", new String[]{"alice", "catherine"});
        kb.removeRecord(2, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xb, 0xc});
        kb.removeRecord(3, new int[]{0x10, 0x11});

        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        kb.removeRecord("mother", new String[]{"alice", "catherine"});
        kb.removeRecord(2, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xb, 0xc});
        kb.removeRecord(3, new int[]{0x10, 0x11});

        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());

        kb.removeRecord("rel", new String[]{"alice", "catherine"});
        kb.removeRecord(5, new String[]{"diana", "frederick"});
        kb.removeRecord("father", new int[]{0xafa, 0xc});
        kb.removeRecord(28, new int[]{0x11, 0x123});

        assertEquals(17, kb.totalMappings());
        assertEquals(3, kb.totalRelations());
        assertEquals(8, kb.totalRecords());
    }

    @Test
    void testEntailment() throws IOException, KbException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.setAsEntailed(1, new int[]{4, 5, 6});
        kb.setAsEntailed(1, new int[]{4, 4, 4});
        kb.setAsEntailed(2, new int[]{7, 9});
        assertTrue(kb.recordIsEntailed(1, new int[]{4, 5, 6}));
        assertFalse(kb.recordIsEntailed(1, new int[]{4, 4, 4}));
        assertFalse(kb.recordIsEntailed(1, new int[]{7, 8, 9}));
        assertFalse(kb.recordIsEntailed(1, new int[]{0xa, 0xb, 0xc}));
        assertFalse(kb.recordIsEntailed(1, new int[]{0xd, 0xe, 0xf}));
        assertTrue(kb.recordIsEntailed(2, new int[]{7, 9}));
        assertFalse(kb.recordIsEntailed(2, new int[]{4, 6}));
        assertFalse(kb.recordIsEntailed(2, new int[]{0xa, 0xc}));
        assertFalse(kb.recordIsEntailed(2, new int[]{0xd, 0xf}));
        assertFalse(kb.recordIsEntailed(3, new int[]{5, 6}));
        assertFalse(kb.recordIsEntailed(3, new int[]{8, 9}));
        assertFalse(kb.recordIsEntailed(3, new int[]{0xb, 0xc}));
        assertFalse(kb.recordIsEntailed(3, new int[]{0x10, 0x11}));
    }

    @Test
    void testPromisingConstants() throws IOException, KbException {
        NumeratedKb kb = new NumeratedKb(testKbManager.getKbName(), TestKbManager.MEM_DIR, true);
        kb.addRecord(1, new int[]{4, 4, 4});
        kb.addRecord(2, new int[]{8, 9});
        kb.addRecord(2, new int[]{9, 9});
        KbRelation.MIN_CONSTANT_COVERAGE = 0.4;
        kb.updatePromisingConstants();
        assertArrayEquals(new int[][]{new int[]{4}, new int[0], new int[0]}, kb.getPromisingConstants(1));
        assertArrayEquals(new int[][]{new int[0], new int[]{9}}, kb.getPromisingConstants(2));
        assertArrayEquals(new int[][]{new int[0], new int[0]}, kb.getPromisingConstants(3));

        KbRelation.MIN_CONSTANT_COVERAGE = 0.5;
        kb.updatePromisingConstants();
        assertArrayEquals(new int[][]{new int[0], new int[0], new int[0]}, kb.getPromisingConstants(1));
        assertArrayEquals(new int[][]{new int[0], new int[]{9}}, kb.getPromisingConstants(2));
        assertArrayEquals(new int[][]{new int[0], new int[0]}, kb.getPromisingConstants(3));
    }

    @Test
    void testConstants() throws KbException {
        NumeratedKb kb = new NumeratedKb("test");
        kb.addRecord("rel", new String[]{"a", "b", "a"});
        kb.addRecords("rel", new String[][]{new String[]{"a", "b", "c"}, new String[]{"a", "d", "c"}});

        assertEquals(
                new HashSet<>(List.of(kb.name2Num("a"), kb.name2Num("b"), kb.name2Num("c"), kb.name2Num("d"))),
                new HashSet<>(kb.getAllConstants())
        );

        kb.removeRecord("rel", new String[]{"a", "d", "c"});

        assertEquals(
                new HashSet<>(List.of(kb.name2Num("a"), kb.name2Num("b"), kb.name2Num("c"))),
                new HashSet<>(kb.getAllConstants())
        );
    }
}