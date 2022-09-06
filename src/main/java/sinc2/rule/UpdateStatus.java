package sinc2.rule;

/**
 * The status of rule update.
 *
 * @since 1.0
 */
public enum UpdateStatus {
    /** The update is successful */
    NORMAL,

    /** The updated rule is duplicated and pruned */
    DUPLICATED,

    /** The updated rule structure is invalid (only happens when generalization is on) */
    INVALID,

    /** The updated rule is pruned due to insufficient fact coverage */
    INSUFFICIENT_COVERAGE,

    /** The updated rule is pruned by the tabu set */
    TABU_PRUNED
}
