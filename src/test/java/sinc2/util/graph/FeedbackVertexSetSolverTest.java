package sinc2.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackVertexSetSolverTest {

    @Test
    void test1() {
        /* 2 vertices cycle */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        graph.put(n1, new HashSet<>(Collections.singleton(n2)));
        graph.put(n2, new HashSet<>(Collections.singleton(n1)));

        Set<GraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2));
    }

    @Test
    void test2() {
        /* 4 vertices cycle(with redundancy) */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        GraphNode<String> n6 = new GraphNode<>("n6");
        GraphNode<String> n7 = new GraphNode<>("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n1)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n4)));

        Set<GraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3) || cover.contains(n4));
    }

    @Test
    void test3() {
        /* 3 vertices cycle(with 2 self loops and redundancy) */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        GraphNode<String> n6 = new GraphNode<>("n6");
        GraphNode<String> n7 = new GraphNode<>("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n1, n2)));
        graph.put(n2, new HashSet<>(Arrays.asList(n2, n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n6, n1)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n7)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n6)));

        Set<GraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n1));
        assertTrue(cover.contains(n2));
    }

    @Test
    void test4() {
        /* 6 vertices, 3 cycles */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        GraphNode<String> n6 = new GraphNode<>("n6");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n6)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));

        Set<GraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4, n5, n6));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n4));
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3));
    }

    @Test
    void test5() {
        /* 4 vertices complete graph */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n1, n3, n4)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n2, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n1, n2, n3)));

        Set<GraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(3, cover.size());
        assertEquals(3, (cover.contains(n1)?1:0) + (cover.contains(n2)?1:0) +
                (cover.contains(n3)?1:0) + (cover.contains(n4)?1:0));
    }

    @Test
    void test6() {
        /* 1 vertex self loop */
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        graph.put(n1, new HashSet<>(List.of(n1)));

        Set<GraphNode<String>> scc = new HashSet<>(List.of(n1));

        FeedbackVertexSetSolver<GraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<GraphNode<String>> cover = solver.run();

        assertEquals(new HashSet<>(List.of(n1)), cover);
    }

    @Test
    void testMultipleInOneGraph() {
        Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
        GraphNode<String> n1 = new GraphNode<>("n1");
        GraphNode<String> n2 = new GraphNode<>("n2");
        GraphNode<String> n3 = new GraphNode<>("n3");
        GraphNode<String> n4 = new GraphNode<>("n4");
        GraphNode<String> n5 = new GraphNode<>("n5");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n1)));
        graph.put(n4, new HashSet<>(Arrays.asList(n1, n2, n3, n5)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n4)));

        Set<GraphNode<String>> scc1 = new HashSet<>(Arrays.asList(n1, n2, n3));
        FeedbackVertexSetSolver<GraphNode<String>> solver1 = new FeedbackVertexSetSolver<>(graph, scc1);
        Set<GraphNode<String>> cover1 = solver1.run();
        assertEquals(1, cover1.size());
        assertTrue(cover1.contains(n1) || cover1.contains(n2) || cover1.contains(n3));

        Set<GraphNode<String>> scc2 = new HashSet<>(Arrays.asList(n4, n5));
        FeedbackVertexSetSolver<GraphNode<String>> solver2 = new FeedbackVertexSetSolver<>(graph, scc2);
        Set<GraphNode<String>> cover2 = solver2.run();
        assertEquals(1, cover2.size());
        assertTrue(cover2.contains(n4) || cover2.contains(n5));
    }
}