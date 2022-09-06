package sinc2.util.graph;

import java.util.Objects;

/**
 * Base class for graph nodes. The information in base graph nodes is used for graph algorithms, such as Tarjan and Minimum
 * Feedback Vertex Set (MFVS). The class can be specified by a user defined content. The comparison between two graph
 * nodes are determined by the user defined content.
 *
 * @param <T> User defined content type
 *
 * @since 1.0
 */
public class GraphNode<T> {
    /** Initial Tarjan index */
    public static final int NO_TARJAN_INDEX = -1;

    /** Initial Tarjan lowLink */
    public static final int NO_TARJAN_LOW_LINK = -1;

    /** Initial MFVS index */
    public static final int NO_FVS_INDEX = -1;

    /* parameters for Tarjan */
    /** Tarjan index */
    public int index = NO_TARJAN_INDEX;

    /** Tarjan lowLink */
    public int lowLink = NO_TARJAN_LOW_LINK;

    /** Tarjan onStack */
    public boolean onStack = false;

    /* parameters for fvs */
    /** FVS index */
    public int fvsIdx = NO_FVS_INDEX;

    /** User defined content */
    public final T content;

    /**
     * Initialize by the user defined content.
     */
    public GraphNode(T content) {
        this.content = content;
    }

    /**
     * The equivalence comparison relies only on the user defined content.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode<?> that = (GraphNode<?>) o;
        return Objects.equals(content, that.content);
    }

    /**
     * The hashing relies only on the user defined content.
     */
    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return '[' + content.toString() + ']';
    }
}
