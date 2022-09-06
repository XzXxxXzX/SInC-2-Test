package sinc2.util.graph;

import java.util.*;

/**
 * The Tarjan algorithm. The algorithm returns Strongly Connected Components (SCCs) that contains at least an edge. That
 * is, either the SCC contains more than one node or is a single node with self-loops.
 *
 * @param <T> The type of graph node.
 *
 * @since 1.0
 */
public class Tarjan<T extends GraphNode<?>> {
    private int index = 0;
    private final Stack<T> stack = new Stack<>();
    private final List<Set<T>> result = new ArrayList<>();
    private final Map<T, Set<T>> graph;

    /**
     * Initialize the algorithm with a graph.
     *
     * @param graph The graph as a adjacent list.
     * @param clearMarkers Whether the marks on the nodes should be cleared before the algorithm runs. This parameter should
     *                   be true if the graph has undergone the Tarjan before. Otherwise, the output may be incorrect.
     */
    public Tarjan(Map<T, Set<T>> graph, boolean clearMarkers) {
        this.graph = graph;
        if (clearMarkers) {
            clearMarkers();
        }
    }

    /**
     * Clear the markers on the graph nodes that are used by Tarjan algorithm.
     */
    private void clearMarkers() {
        for (Map.Entry<T, Set<T>> entry: graph.entrySet()) {
            T source_node = entry.getKey();
            Set<T> neighbours = entry.getValue();
            source_node.index = GraphNode.NO_TARJAN_INDEX;
            source_node.lowLink = GraphNode.NO_TARJAN_LOW_LINK;
            source_node.onStack = false;
            for (T neighbour: neighbours) {
                neighbour.index = GraphNode.NO_TARJAN_INDEX;
                neighbour.lowLink = GraphNode.NO_TARJAN_LOW_LINK;
                neighbour.onStack = false;
            }
        }
    }

    /**
     * Run the Tarjan algorithm.
     *
     * @return A list of strongly connected components (SCCs). An SCC is a set of graph nodes.
     */
    public List<Set<T>> run() {
        for (T node : graph.keySet()) {
            if (-1 == node.index) {
                strongConnect(node);
            }
        }
        return result;
    }

    private void strongConnect(T node) {
        node.index = index;
        node.lowLink = index;
        node.onStack = true;
        index++;
        stack.push(node);

        Set<T> neighbours = graph.get(node);
        if (null != neighbours) {
            for (T neighbour : neighbours) {
                if (-1 == neighbour.index) {
                    strongConnect(neighbour);
                    node.lowLink = Math.min(node.lowLink, neighbour.lowLink);
                } else if (neighbour.onStack) {
                    node.lowLink = Math.min(node.lowLink, neighbour.index);
                }
            }
        }

        if (node.lowLink == node.index) {
            Set<T> scc = new HashSet<>();
            T top;
            do {
                top = stack.pop();
                top.onStack = false;
                scc.add(top);
            } while (!node.equals(top));

            /* 只返回非平凡的强连通分量 */
            if (1 < scc.size()) {
                result.add(scc);
            } else if (null != neighbours && neighbours.contains(node)) {
                /* graph中可能包含自环 */
                result.add(scc);
            }
        }
    }
}
