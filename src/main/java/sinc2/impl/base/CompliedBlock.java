package sinc2.impl.base;

import sinc2.kb.Record;

import java.util.Set;

/**
 * The Complied Block (CB) structure. Every member is read only, as operations on the cache should follow "copy-on-write"
 * strategy.
 *
 * @since 2.0
 */
public class CompliedBlock {
    /** Relation Numeration */
    public final int relNum;
    /** Partially Assigned Record (PAR) */
    public final int[] partAsgnRecord;
    /** Compliance Set (CS) */
    public final Set<Record> complSet;

    public CompliedBlock(int relNum, int[] partAsgnRecord, Set<Record> complSet) {
        this.relNum = relNum;
        this.partAsgnRecord = partAsgnRecord;
        this.complSet = complSet;
    }
}
