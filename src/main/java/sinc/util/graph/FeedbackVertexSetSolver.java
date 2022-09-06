package sinc.util.graph;

import java.util.*;

public class FeedbackVertexSetSolver<T extends BaseGraphNode<?>> {

    protected final List<T> nodes;
    protected final int[][] matrix;
    protected final int size;

    public FeedbackVertexSetSolver(Map<T, Set<T>> graph, Set<T> scc) {
        /* 在这里把SCC转成邻接矩阵的形式 */
        size = scc.size();
        matrix = new int[size+1][size+1];

        /* 先把每个点编号 */
        nodes = new ArrayList<>(size);
        for (T node: scc) {
            node.fvsIdx = nodes.size();
            nodes.add(node);
        }

        /* 截取graph中属于SCC的部分 */
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

        /* 将节点的编号取消，否则处理同一张图的其他scc时可能会引起混乱 */
        for (T node: nodes) {
            node.fvsIdx = T.NO_FVS_INDEX;
        }
    }

    public Set<T> run() {
        int edges = 0;
        for (int i: matrix[size]) {
            edges += i;
        }
        Set<T> result = new HashSet<>();
        while (0 < edges) {
            /* 每次取|in|x|out|最大的点，然后把相关的环删掉，直到最后没有环 */
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

            /* 从SCC中删除这个点以及相关的环 */
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
