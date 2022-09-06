package sinc2.util.graph;

import java.util.*;

/**
 * The Minimum Feedback Vertex Set (MFVS) algorithm. The algorithm returns a set of graph nodes that all the cycles in a
 * Strongly Connected Component (SCC) are broken if the set of nodes are removed.
 *
 * @param <T> The type of graph node.
 *
 * @since 1.0
 */
public class FeedbackVertexSetSolver<T extends GraphNode<?>> {
    /** The set of nodes in the SCC */
    protected final List<T> nodes;

    /** The connection matrix of the SCC */
    protected final int[][] matrix;

    /** The size of the SCC */
    protected final int size;

    /**
     * Initialize the solver by a graph and a SCC in the graph.
     *
     * @param graph A complete graph, in the form of a adjacent list
     * @param scc An SCC in the graph
     */
    public FeedbackVertexSetSolver(Map<T, Set<T>> graph, Set<T> scc) {
        /* Convert the SCC into a connection matrix */
        size = scc.size();
        matrix = new int[size+1][size+1];

        /* Numeration the nodes */
        nodes = new ArrayList<>(size);
        for (T node: scc) {
            node.fvsIdx = nodes.size();
            nodes.add(node);
        }

        /* Build the connection matrix */
        for (T node: nodes) {
            Set<T> successors = graph.get(node);
            for (T successor: successors) {
                if (T.NO_FVS_INDEX != successor.fvsIdx) {
                    matrix[node.fvsIdx][successor.fvsIdx] = 1;
                    matrix[node.fvsIdx][size]++;
                    matrix[size][successor.fvsIdx]++;
                }
            }
        }

        /* Cancel the numeration on the nodes */
        for (T node: nodes) {
            node.fvsIdx = T.NO_FVS_INDEX;
        }
    }

    /**
     * Run the MFVS algorithm.
     *
     * @return A set of graph nodes that breaks all the cycles in the SCC
     */
    public Set<T> run() {
        int edges = 0;
        for (int i: matrix[size]) {
            edges += i;
        }
        Set<T> result = new HashSet<>();
        while (0 < edges) {
            /* Take the node with maximum in-degree x out-degree and remove the related cycle, until there is no cycle */
            int max_score = 0;
            int max_idx = -1;
            for (int i = 0; i < size; i++) {
                int score = matrix[i][size] * matrix[size][i];
                if (score > max_score) {
                    max_score = score;
                    max_idx = i;
                }
            }
            result.add(nodes.get(max_idx));

            /* Remove the node from the SCC with related cycles */
            edges -= removeNode(max_idx);
            boolean updated = true;
            while (updated) {
                updated = false;
                for (int i = 0; i < size; i++) {
                    if (0 == matrix[i][size] ^ 0 == matrix[size][i]) {
                        edges -= removeNode(i);
                        updated = true;
                    }
                }
            }
        }
        return result;
    }

    protected int removeNode(int idx) {
        int removed_edges = matrix[idx][size] + matrix[size][idx] - matrix[idx][idx];
        for (int i = 0; i < size; i++) {
            if (1 == matrix[idx][i]) {
                matrix[idx][i] = 0;
                matrix[size][i]--;
            }
            if (1 == matrix[i][idx]) {
                matrix[i][idx] = 0;
                matrix[i][size]--;
            }
        }
        matrix[idx][size] = 0;
        matrix[size][idx] = 0;
        return removed_edges;
    }
}
