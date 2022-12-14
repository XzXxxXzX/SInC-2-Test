package sinc2.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TarjanTest {

    static class MapWithAppointedKeySet implements Map<GraphNode<String>, Set<GraphNode<String>>> {
        private final Map<GraphNode<String>, Set<GraphNode<String>>> actualMap = new HashMap<>();
        private final Set<GraphNode<String>> appointedKeySet = new HashSet<>();
//        private final Set<Entry<GraphNode<String>, Set<GraphNode<String>>>> appointedEntrySet = new HashSet<>();

        public void addAppointedKey(GraphNode<String> key) {
            appointedKeySet.add(key);
        }

        @Override
        public int size() {
            return actualMap.size();
        }

        @Override
        public Set<GraphNode<String>> get(Object o) {
            return actualMap.get(o);
        }

        @Override
        public Set<GraphNode<String>> put(GraphNode<String> key, Set<GraphNode<String>> value) {
            return actualMap.put(key, value);
        }

        @Override
        public boolean containsKey(Object o) {
            return actualMap.containsKey(o);
        }

        @Override
        public boolean containsValue(Object o) {
            return actualMap.containsValue(o);
        }

        @Override
        public Collection<Set<GraphNode<String>>> values() {
            return actualMap.values();
        }

        @Override
        public boolean isEmpty() {
            return actualMap.isEmpty();
        }

        @Override
        public Set<Entry<GraphNode<String>, Set<GraphNode<String>>>> entrySet() {
            Set<Entry<GraphNode<String>, Set<GraphNode<String>>>> entry_set = new HashSet<>();
            for (Entry<GraphNode<String>, Set<GraphNode<String>>> entry: actualMap.entrySet()) {
                if (appointedKeySet.contains(entry.getKey())) {
                    entry_set.add(entry);
                }
            }
            return entry_set;
        }

        @Override
        public Set<GraphNode<String>> keySet() {
            return appointedKeySet;
        }

        @Override
        public Set<GraphNode<String>> remove(Object o) {
            return actualMap.remove(o);
        }

        @Override
        public void clear() {
            actualMap.clear();
            appointedKeySet.clear();
        }

        @Override
        public void putAll(Map<? extends GraphNode<String>, ? extends Set<GraphNode<String>>> map) {
            actualMap.putAll(map);
        }
    }

    @Test
    public void testAppointedMap() {
        MapWithAppointedKeySet map = new MapWithAppointedKeySet();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        map.put(n1, new HashSet<>(Collections.singletonList(n2)));
        map.put(n2, new HashSet<>(Collections.singletonList(n3)));
        map.addAppointedKey(n2);

        assertEquals(1, map.entrySet().size());
        for (Map.Entry<GraphNode<String>, Set<GraphNode<String>>> entry: map.entrySet()) {
            assertEquals(n2, entry.getKey());
            assertEquals(new HashSet<>(Collections.singletonList(n3)), entry.getValue());
        }

        assertEquals(1, map.keySet().size());
        for (GraphNode<String> node: map.keySet()) {
            assertEquals(n2, node);
        }
    }

    @Test
    public void testRun() {
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String>n1 = new GraphNode<>("n1");
        GraphNode<String>n2 = new GraphNode<>("n2");
        GraphNode<String>n3 = new GraphNode<>("n3");
        GraphNode<String>n4 = new GraphNode<>("n4");
        GraphNode<String>n5 = new GraphNode<>("n5");
        GraphNode<String>n6 = new GraphNode<>("n6");
        GraphNode<String>n7 = new GraphNode<>("n7");
        GraphNode<String>n8 = new GraphNode<>("n8");
        graph.put(n1, new HashSet<>(List.of(n2, n4)));
        graph.put(n2, new HashSet<>(List.of(n3, n5)));
        graph.put(n3, new HashSet<>(List.of(n1)));
        graph.put(n4, new HashSet<>(List.of(n3)));
        graph.put(n5, new HashSet<>(List.of(n6, n7)));
        graph.put(n6, new HashSet<>(List.of(n5)));
        graph.put(n7, new HashSet<>(List.of(n5)));
        graph.put(n8, new HashSet<>());

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        for (Set<GraphNode<String>> scc: sccs) {
            System.out.print("SCC: ");
            for (GraphNode<String>n: scc) {
                System.out.print(n + ", ");
            }
            System.out.println();
        }
        assertEquals(2, sccs.size());
        for (int i = 0; i < 2; i++) {
            Set<GraphNode<String>> scc = sccs.get(i);
            switch (scc.size()) {
                case 3:
                    assertTrue(scc.contains(n5));
                    assertTrue(scc.contains(n6));
                    assertTrue(scc.contains(n6));
                    break;
                case 4:
                    assertTrue(scc.contains(n1));
                    assertTrue(scc.contains(n2));
                    assertTrue(scc.contains(n3));
                    assertTrue(scc.contains(n4));
                    break;
                default:
                    fail();
            }
        }
    }

    @Test
    public void testRun2() {
        GraphNode<String>n1 = new GraphNode<>("n1");
        GraphNode<String>n2 = new GraphNode<>("n2");
        GraphNode<String>n3 = new GraphNode<>("n3");
        GraphNode<String>n4 = new GraphNode<>("n4");
        GraphNode<String>n5 = new GraphNode<>("n5");
        GraphNode<String>n6 = new GraphNode<>("n6");
        GraphNode<String>n7 = new GraphNode<>("n7");
        GraphNode<String>n8 = new GraphNode<>("n8");
        GraphNode<String>n9 = new GraphNode<>("n9");
        GraphNode<String>n10 = new GraphNode<>("n10");
        GraphNode<String>n11 = new GraphNode<>("n11");
        GraphNode<String>n12 = new GraphNode<>("n12");
        GraphNode<String>n13 = new GraphNode<>("n13");
        GraphNode<String>n14 = new GraphNode<>("n14");
        GraphNode<String>n15 = new GraphNode<>("n15");
        GraphNode<String>n16 = new GraphNode<>("n16");
        GraphNode<String>n17 = new GraphNode<>("n17");
        GraphNode<String>n18 = new GraphNode<>("n18");
        GraphNode<String>n19 = new GraphNode<>("n19");
        GraphNode<String>n20 = new GraphNode<>("n20");
        GraphNode<String>n21 = new GraphNode<>("n21");
        GraphNode<String>n22 = new GraphNode<>("n22");
        GraphNode<String>n23 = new GraphNode<>("n23");
        GraphNode<String>n24 = new GraphNode<>("n24");

        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(List.of(n9, n15)));
        graph.put(n2, new HashSet<>(List.of(n9, n16)));
        graph.put(n3, new HashSet<>(List.of(n10, n17)));
        graph.put(n4, new HashSet<>(List.of(n11, n18)));
        graph.put(n5, new HashSet<>(List.of(n12, n19)));
        graph.put(n6, new HashSet<>(List.of(n12, n20)));
        graph.put(n7, new HashSet<>(List.of(n13, n21)));
        graph.put(n8, new HashSet<>(List.of(n14, n22)));
        graph.put(n23, new HashSet<>());
        graph.put(n24, new HashSet<>());

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testRun3() {
        GraphNode<String>n1 = new GraphNode<>("n1");
        GraphNode<String>n2 = new GraphNode<>("n2");
        GraphNode<String>n3 = new GraphNode<>("n3");

        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        graph.put(n1, new HashSet<>(List.of(n2, n3)));

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(0, sccs.size());
    }

    @Test
    public void testRun4() {
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String>n1 = new GraphNode<>("n1");
        GraphNode<String>n2 = new GraphNode<>("n2");
        graph.put(n1, new HashSet<>(List.of(n1)));
        graph.put(n2, new HashSet<>(List.of(n2)));

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        for (Set<GraphNode<String>> scc: sccs) {
            System.out.print("SCC: ");
            for (GraphNode<String>n: scc) {
                System.out.print(n + ", ");
            }
            System.out.println();
        }
        assertEquals(2, sccs.size());
        assertEquals(new HashSet<>(List.of(
                new HashSet<>(List.of(n1)),
                new HashSet<>(List.of(n2))
        )), new HashSet<>(sccs));
    }

    @Test
    public void testRun5() {
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String>n1 = new GraphNode<>("n1");
        GraphNode<String>n2 = new GraphNode<>("n2");
        GraphNode<String>n3 = new GraphNode<>("n3");
        graph.put(n1, new HashSet<>(List.of(n1)));
        graph.put(n2, new HashSet<>(List.of(n2, n3)));
        graph.put(n3, new HashSet<>(List.of(n2)));

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        for (Set<GraphNode<String>> scc: sccs) {
            System.out.print("SCC: ");
            for (GraphNode<String>n: scc) {
                System.out.print(n + ", ");
            }
            System.out.println();
        }
        assertEquals(2, sccs.size());
        assertEquals(new HashSet<>(List.of(
                new HashSet<>(List.of(n1)),
                new HashSet<>(List.of(n2, n3))
        )), new HashSet<>(sccs));
    }

    @Test
    public void testAppointedStartPoints1() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n1);

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(List.of(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints2() {
        MapWithAppointedKeySet graph = new MapWithAppointedKeySet();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n2)));
        graph.addAppointedKey(n2);

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(List.of(n2, n3))));
    }

    @Test
    public void testAppointedStartPoints3() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(List.of(n2, n4)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph1, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(List.of(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints4() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        GraphNode<String> n6 = new GraphNode<>("n6");
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(List.of(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph1, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(List.of(n2, n3, n4))));
    }

    @Test
    public void testAppointedStartPoints5() {
        MapWithAppointedKeySet graph1 = new MapWithAppointedKeySet();
        GraphNode<String> n0 = new GraphNode<>("n0");
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        GraphNode<String> n6 = new GraphNode<>("n6");
        graph1.put(n0, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph1.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph1.put(n3, new HashSet<>(List.of(n2, n4, n5, n6)));
        graph1.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph1.addAppointedKey(n1);
        graph1.addAppointedKey(n0);

        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph1, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        assertEquals(1, sccs.size());
        assertTrue(sccs.contains(new HashSet<>(List.of(n2, n3, n4))));
    }
}