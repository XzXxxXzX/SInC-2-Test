package sinc.util.graph;

import java.util.*;

public class Tarjan<T extends BaseGraphNode<?>> {
    private int index = 0;
    private final Stack<T> stack = new Stack<>();
    private final List<Set<T>> result = new ArrayList<>();
    private final Map<T, Set<T>> graph;

    public Tarjan(Map<T, Set<T>> graph) {
        this.graph = graph;
    }

    private void clearMarks() {
        for (Map.Entry<T, Set<T>> entry: graph.entrySet()) {
            T source_node = entry.getKey();
            Set<T> neighbours = entry.getValue();
            source_node.index = BaseGraphNode.NO_TARJAN_INDEX;
            source_node.lowLink = BaseGraphNode.NO_TARJAN_LOW_LINK;
            source_node.onStack = false;
            for (T neighbour: neighbours) {
                neighbour.index = BaseGraphNode.NO_TARJAN_INDEX;
                neighbour.lowLink = BaseGraphNode.NO_TARJAN_LOW_LINK;
                neighbour.onStack = false;
            }
        }
    }

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
