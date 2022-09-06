package sinc2.impl.base;

import sinc2.kb.Record;

import java.util.*;

/**
 * This class is used for a single cache entry. If the entry is in the cache of all entailments (E-cache), the first
 * element should be null to keep the same length as the cache of positive entailments (E+-cache), as the head should
 * be removed in E-cache.
 *
 * The indices at each index of the list correspond to the CB in the entry.
 *
 * @since 2.0
 */
public class CacheEntry {
    /** The list of CBs. CBs shall only be replaced, but not modified. */
    public final List<CompliedBlock> entry;
    /** The list of argument indices of each CB. Indices shall only be replaced, but not modified. */
    public final List<Map<Integer, Set<Record>>[]> argIndicesList;

    public CacheEntry(List<CompliedBlock> entry, List<Map<Integer, Set<Record>>[]> argIndicesList) {
        this.entry = entry;
        this.argIndicesList = argIndicesList;
    }

    public CacheEntry(CacheEntry another) {
        this.entry = new ArrayList<>(another.entry);
        this.argIndicesList = new ArrayList<>(another.argIndicesList);
    }

    /**
     * Update the indices of the CBs. Every NULL element will be replaced with a batch of new indices. The others will
     * remain the same.
     *
     * Note: The first element of the indices lists in the E-cache should be made non-null to skip the update.
     */
    public void updateIndices() {
        for (int cb_idx = 0; cb_idx < entry.size(); cb_idx++) {
            Map<Integer, Set<Record>>[] arg_indices = argIndicesList.get(cb_idx);
            if (null == arg_indices) {
                CompliedBlock cb = entry.get(cb_idx);
                arg_indices = new Map[cb.partAsgnRecord.length];
                for (int i = 0; i < arg_indices.length; i++) {
                    arg_indices[i] = new HashMap<>();
                }
                for (Record record : cb.complSet) {
                    for (int arg_idx = 0; arg_idx < arg_indices.length; arg_idx++) {
                        arg_indices[arg_idx].computeIfAbsent(record.args[arg_idx], k -> new HashSet<>()).add(record);
                    }
                }
                argIndicesList.set(cb_idx, arg_indices);
            }
        }
    }
}
