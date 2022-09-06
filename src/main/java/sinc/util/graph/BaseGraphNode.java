package sinc.util.graph;

import java.util.Objects;

public class BaseGraphNode<T> {
    public static final int NO_TARJAN_INDEX = -1;
    public static final int NO_TARJAN_LOW_LINK = -1;
    public static final int NO_FVS_INDEX = -1;

    /* parameters for Tarjan */
    public int index = NO_TARJAN_INDEX;
    public int lowLink = NO_TARJAN_LOW_LINK;
    public boolean onStack = false;

    /* parameters for fvs */
    public int fvsIdx = NO_FVS_INDEX;

    public final T content;

    public BaseGraphNode(T content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseGraphNode<?> that = (BaseGraphNode<?>) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return '[' + content.toString() + ']';
    }
}
