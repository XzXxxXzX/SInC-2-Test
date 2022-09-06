package sinc2.util;

import java.util.*;

/**
 * The multi-set class.
 *
 * @param <T> The type of elements in the set
 *
 * @since 1.0
 */
public class MultiSet<T> {
    private final Map<T, Integer> cntMap;
    private int size = 0;

    public MultiSet() {
        cntMap = new HashMap<>();
    }

    public MultiSet(MultiSet<T> another) {
        this.cntMap = new HashMap<>(another.cntMap);
        this.size = another.size;
    }

    public MultiSet(T[] elements) {
        cntMap = new HashMap<>();
        size = elements.length;
        for (T t: elements) {
            cntMap.compute(t, (k, v) -> (null == v) ? 1 : v + 1);
        }
    }

    public void add(T element) {
        cntMap.compute(element, (k, v) -> (null == v) ? 1 : v + 1);
        size++;
    }

    public void addAll(T[] elements) {
        for (T element: elements) {
            cntMap.compute(element, (k, v) -> (null == v) ? 1 : v + 1);
        }
        size += elements.length;
    }

    public void addAll(MultiSet<T> another) {
        for (Map.Entry<T, Integer> entry: another.cntMap.entrySet()) {
            this.cntMap.compute(entry.getKey(), (k, v) -> (null == v) ? entry.getValue() : v + entry.getValue());
            this.size += entry.getValue();
        }
    }

    public void remove(T element) {
        cntMap.computeIfPresent(element, (k, v) -> {
            if (1 <= v) {
                size--;
            }
            return (1 < v) ? v - 1 : null;
        });
    }

    public int size() {
        return size;
    }

    /**
     * Calculate the Jaccard similarity to another multi-set.
     */
    public double jaccardSimilarity(MultiSet<T> another) {
        MultiSet<T> intersection = this.intersection(another);
        MultiSet<T> union = this.union(another);
        double intersection_size = intersection.size();
        double union_sieze = union.size();
        return intersection_size / union_sieze;
    }

    /**
     * Calculate the intersection with another multi-set.
     */
    public MultiSet<T> intersection(MultiSet<T> another) {
        Set<Map.Entry<T, Integer>> entry_set;
        Map<T, Integer> compared_map;
        if (this.cntMap.keySet().size() <= another.cntMap.keySet().size()) {
            entry_set = this.cntMap.entrySet();
            compared_map = another.cntMap;
        } else {
            entry_set = another.cntMap.entrySet();
            compared_map = this.cntMap;
        }

        MultiSet<T> intersection = new MultiSet<>();
        for (Map.Entry<T, Integer> entry: entry_set) {
            Integer compared_cnt = compared_map.get(entry.getKey());
            if (null != compared_cnt) {
                int i = Math.min(entry.getValue(), compared_cnt);
                intersection.cntMap.put(entry.getKey(), i);
                intersection.size += i;
            }
        }
        return intersection;
    }

    /**
     * Calculate the union with another multi-set.
     */
    public MultiSet<T> union(MultiSet<T> another) {
        Set<Map.Entry<T, Integer>> entry_set;
        MultiSet<T> union;
        if (this.cntMap.keySet().size() <= another.cntMap.keySet().size()) {
            entry_set = this.cntMap.entrySet();
            union = new MultiSet<>(another);
        } else {
            entry_set = another.cntMap.entrySet();
            union = new MultiSet<>(this);
        }

        for (Map.Entry<T, Integer> entry: entry_set) {
            union.cntMap.compute(entry.getKey(), (k, v) -> {
                if (null != v) {
                    if (entry.getValue() > v) {
                        union.size += entry.getValue() - v;
                        return entry.getValue();
                    }
                    return v;
                } else {
                    union.size += entry.getValue();
                    return entry.getValue();
                }
            });
        }
        return union;
    }

    /**
     * Enumerate the element that is above a proportion threshold.
     */
    public List<T> elementsAboveProportion(double proportion) {
        List<T> result = new ArrayList<>();
        int threshold = (int)(this.size * proportion);
        for (Map.Entry<T, Integer> entry: cntMap.entrySet()) {
            if (entry.getValue() > threshold) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public int differentValues() {
        return cntMap.size();
    }

    public Set<T> distinctValues() {
        return cntMap.keySet();
    }

    /**
     * Check whether this set is a subset of another.
     */
    public boolean subsetOf(MultiSet<T> another) {
        if (this.cntMap.size() > another.cntMap.size() || this.size > another.size) {
            return false;
        }
        for (Map.Entry<T, Integer> entry: this.cntMap.entrySet()) {
            final Integer another_cnt = another.cntMap.get(entry.getKey());
            if (null == another_cnt || another_cnt < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSet<?> multiSet = (MultiSet<?>) o;
        return size == multiSet.size && Objects.equals(cntMap, multiSet.cntMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cntMap, size);
    }
}
