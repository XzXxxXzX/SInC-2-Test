package sinc2.impl.base;

/**
 * This class is used for marking the location of a pre-LV (PLV) in the body and one of the corresponding LV in the head.
 *
 * @since 2.0
 */
public class PlvLoc {
    /** The index of the body predicate */
    final int bodyPredIdx;
    /** The index of the argument in the predicate */
    final int bodyArgIdx;
    /** The index of the argument in the head */
    final int headArgIdx;

    public PlvLoc(int bodyPredIdx, int bodyArgIdx, int headArgIdx) {
        this.bodyPredIdx = bodyPredIdx;
        this.bodyArgIdx = bodyArgIdx;
        this.headArgIdx = headArgIdx;
    }
}
