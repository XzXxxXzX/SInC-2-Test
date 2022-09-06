package sinc.util.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackVertexSetSolverTest {

    @Test
    void test1() {
        /* 2 vertices cycle */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        graph.put(n1, new HashSet<>(Collections.singleton(n2)));
        graph.put(n2, new HashSet<>(Collections.singleton(n1)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2));
    }

    @Test
    void test2() {
        /* 4 vertices cycle(with redundancy) */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<>("n5");
        BaseGraphNode<String> n6 = new BaseGraphNode<>("n6");
        BaseGraphNode<String> n7 = new BaseGraphNode<>("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n1)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n4)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(1, cover.size());
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3) || cover.contains(n4));
    }

    @Test
    void test3() {
        /* 3 vertices cycle(with 2 self loops and redundancy) */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<>("n5");
        BaseGraphNode<String> n6 = new BaseGraphNode<>("n6");
        BaseGraphNode<String> n7 = new BaseGraphNode<>("n7");
        graph.put(n1, new HashSet<>(Arrays.asList(n1, n2)));
        graph.put(n2, new HashSet<>(Arrays.asList(n2, n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n6, n1)));
        graph.put(n4, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n7)));
        graph.put(n7, new HashSet<>(Collections.singletonList(n6)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n1));
        assertTrue(cover.contains(n2));
    }

    @Test
    void test4() {
        /* 6 vertices, 3 cycles */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<>("n5");
        BaseGraphNode<String> n6 = new BaseGraphNode<>("n6");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n2, n5)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n6)));
        graph.put(n6, new HashSet<>(Collections.singletonList(n4)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4, n5, n6));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(2, cover.size());
        assertTrue(cover.contains(n4));
        assertTrue(cover.contains(n1) || cover.contains(n2) || cover.contains(n3));
    }

    @Test
    void test5() {
        /* 4 vertices complete graph */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<>("n4");
        graph.put(n1, new HashSet<>(Arrays.asList(n2, n3, n4)));
        graph.put(n2, new HashSet<>(Arrays.asList(n1, n3, n4)));
        graph.put(n3, new HashSet<>(Arrays.asList(n1, n2, n4)));
        graph.put(n4, new HashSet<>(Arrays.asList(n1, n2, n3)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(Arrays.asList(n1, n2, n3, n4));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(3, cover.size());
        assertEquals(3, (cover.contains(n1)?1:0) + (cover.contains(n2)?1:0) +
                (cover.contains(n3)?1:0) + (cover.contains(n4)?1:0));
    }

    @Test
    void test6() {
        /* 1 vertex self loop */
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        graph.put(n1, new HashSet<>(List.of(n1)));

        Set<BaseGraphNode<String>> scc = new HashSet<>(List.of(n1));

        FeedbackVertexSetSolver<BaseGraphNode<String>> solver = new FeedbackVertexSetSolver<>(graph, scc);
        Set<BaseGraphNode<String>> cover = solver.run();

        assertEquals(new HashSet<>(List.of(n1)), cover);
    }

    @Test
    void testMultipleInOneGraph() {
        Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> graph = new HashMap<>();
        BaseGraphNode<String> n1 = new BaseGraphNode<>("n1");
        BaseGraphNode<String> n2 = new BaseGraphNode<>("n2");
        BaseGraphNode<String> n3 = new BaseGraphNode<>("n3");
        BaseGraphNode<String> n4 = new BaseGraphNode<>("n4");
        BaseGraphNode<String> n5 = new BaseGraphNode<>("n5");
        graph.put(n1, new HashSet<>(Collections.singletonList(n2)));
        graph.put(n2, new HashSet<>(Collections.singletonList(n3)));
        graph.put(n3, new HashSet<>(Collections.singletonList(n1)));
        graph.put(n4, new HashSet<>(Arrays.asList(n1, n2, n3, n5)));
        graph.put(n5, new HashSet<>(Collections.singletonList(n4)));

        Set<BaseGraphNode<String>> scc1 = new HashSet<>(Arrays.asList(n1, n2, n3));
        FeedbackVertexSetSolver<BaseGraphNode<String>> solver1 = new FeedbackVertexSetSolver<>(graph, scc1);
        Set<BaseGraphNode<String>> cover1 = solver1.run();
        assertEquals(1, cover1.size());
        assertTrue(cover1.contains(n1) || cover1.contains(n2) || cover1.contains(n3));

        Set<BaseGraphNode<String>> scc2 = new HashSet<>(Arrays.asList(n4, n5));
        FeedbackVertexSetSolver<BaseGraphNode<String>> solver2 = new FeedbackVertexSetSolver<>(graph, scc2);
        Set<BaseGraphNode<String>> cover2 = solver2.run();
        assertEquals(1, cover2.size());
        assertTrue(cover2.contains(n4) || cover2.contains(n5));
    }
}