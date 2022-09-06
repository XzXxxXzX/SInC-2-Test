package sinc.impl.cached;

import sinc.common.Predicate;
import sinc.util.MultiSet;

import java.util.*;

public class MemKB {
    static class ColumnPairInfo {
        final String functor1;
        final int idx1;
        final String functor2;
        final int idx2;

        public ColumnPairInfo(String functor1, int idx1, String functor2, int idx2) {
            this.functor1 = functor1;
            this.idx1 = idx1;
            this.functor2 = functor2;
            this.idx2 = idx2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnPairInfo that = (ColumnPairInfo) o;
            return idx1 == that.idx1 && idx2 == that.idx2 && Objects.equals(functor1, that.functor1) && Objects.equals(functor2, that.functor2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(functor1, idx1, functor2, idx2);
        }
    }

    protected final Set<Predicate> originalKB = new HashSet<>();
    protected final Map<String, Set<Predicate>> functor2Facts = new HashMap<>();
    protected final Map<String, Integer> functor2ArityMap = new HashMap<>();
    protected final Map<String, Map<String, Set<Predicate>>[]> functor2ArgIdx = new HashMap<>();
    protected final Set<String> constants = new HashSet<>();
    protected final Set<Predicate> provedFacts = new HashSet<>();
    protected final Map<String, MultiSet<String>[]> functor2ArgSetsMap = new HashMap<>();
    protected final Map<String, List<String>[]> functor2PromisingConstMap = new HashMap<>();
    protected final Set<ColumnPairInfo> similarColumnPairs = new HashSet<>();

    public void declareFunctor(String functor, int arity) {
        functor2Facts.computeIfAbsent(functor, k -> new HashSet<>());
        functor2ArityMap.putIfAbsent(functor, arity);
        functor2ArgIdx.computeIfAbsent(functor, k -> {
            Map<String, Set<Predicate>>[] _arg_indices = new Map[arity];
            for (int i = 0; i < arity; i++) {
                _arg_indices[i] = new HashMap<>();
            }
            return _arg_indices;
        });
        functor2ArgSetsMap.computeIfAbsent(functor, k -> {
            MultiSet<String>[] _arg_set_list = new MultiSet[arity];
            for (int i = 0; i < _arg_set_list.length; i++) {
                _arg_set_list[i] = new MultiSet<>();
            }
            return _arg_set_list;
        });
        functor2PromisingConstMap.computeIfAbsent(functor, k -> {
            List<String>[] _const_lists = new List[arity];
            for (int i = 0; i < _const_lists.length; i++) {
                _const_lists[i] = new ArrayList<>();
            }
            return _const_lists;
        });
    }

    public boolean addFact(Predicate predicate) {
        /* 添加到functor索引 */
        if (!originalKB.add(predicate)) {
            return false;
        }
        functor2Facts.compute(predicate.functor, (func, set) -> {
            if (null == set) {
                set = new HashSet<>();
                functor2ArityMap.put(func, predicate.arity());
            }
            set.add(predicate);
            return set;
        });

        /* 添加到argument索引 */
        final Map<String, Set<Predicate>>[] arg_indices = functor2ArgIdx.computeIfAbsent(
                predicate.functor, k -> {
                    Map<String, Set<Predicate>>[] _arg_indices = new Map[predicate.arity()];
                    for (int i = 0; i < predicate.arity(); i++) {
                        _arg_indices[i] = new HashMap<>();
                    }
                    return _arg_indices;
                }
        );
        final MultiSet<String>[] arg_sets =  functor2ArgSetsMap.computeIfAbsent(
                predicate.functor, k -> {
                    MultiSet<String>[] _arg_set_list = new MultiSet[predicate.arity()];
                    for (int i = 0; i < _arg_set_list.length; i++) {
                        _arg_set_list[i] = new MultiSet<>();
                    }
                    return _arg_set_list;
                }
        );
        for (int i = 0; i < predicate.arity(); i++) {
            final String constant_symbol = predicate.args[i].name;
            arg_indices[i].compute(constant_symbol, (const_sym, set) -> {
                if (null == set) {
                    set = new HashSet<>();
                }
                set.add(predicate);
                return set;
            });
            arg_sets[i].add(constant_symbol);
            constants.add(constant_symbol);
        }
        return true;
    }

    public void calculatePromisingConstants(double threshold) {
        functor2PromisingConstMap.clear();
        for (Map.Entry<String, MultiSet<String>[]> entry: functor2ArgSetsMap.entrySet()) {
            MultiSet<String>[] arg_sets = entry.getValue();
            List<String>[] arg_const_lists = new List[arg_sets.length];
            for (int i = 0; i < arg_sets.length; i++) {
                arg_const_lists[i] = arg_sets[i].elementsAboveProportion(threshold);
            }
            functor2PromisingConstMap.put(entry.getKey(), arg_const_lists);
        }
    }

    public void calculateSimilarColumnPairs(double threshold) {
        similarColumnPairs.clear();
        Map.Entry<String, MultiSet<String>[]>[] entries = functor2ArgSetsMap.entrySet().toArray(new Map.Entry[0]);

        for (int i = 0; i < entries.length; i++) {
            String functor1 = entries[i].getKey();
            MultiSet<String>[] arg_sets1 = entries[i].getValue();
            for (int j = i; j < entries.length; j++) {
                String functor2 = entries[j].getKey();
                MultiSet<String>[] arg_sets2 = entries[j].getValue();
                for (int ii = 0; ii < arg_sets1.length; ii++) {
                    for (int jj = 0; jj < arg_sets2.length; jj++) {
                        double similarity = arg_sets1[ii].jaccardSimilarity(arg_sets2[jj]);
                        if (similarity >= threshold) {
                            similarColumnPairs.add(new ColumnPairInfo(functor1,ii,functor2,jj));
                            similarColumnPairs.add(new ColumnPairInfo(functor2,jj,functor1,ii));
                        }
                    }
                }
            }
        }
    }

    public boolean columnsSimilar(String functor1, int idx1, String functor2, int idx2) {
        return similarColumnPairs.contains(new ColumnPairInfo(functor1, idx1, functor2, idx2));
    }

    public int totalConstants() {
        return constants.size();
    }

    public int totalFacts() {
        return originalKB.size();
    }

    public long totalColumnPairs() {
        long arity_cnt = 0;
        for (Map.Entry<String, Integer> entry: functor2ArityMap.entrySet()) {
            arity_cnt += entry.getValue();
        }
        return arity_cnt * arity_cnt;
    }

    public long similarColumnPairs() {
        return similarColumnPairs.size();
    }

    public int getArity(String functor) {
        /* 这里不做错误处理，有问题直接抛异常 */
        return functor2ArityMap.get(functor);
    }

    public Set<Predicate> getAllFacts(String functor) {
        /* 这里不做错误处理，有问题直接抛异常 */
        return functor2Facts.get(functor);
    }

    public Set<String> getValueSet(String functor, int argIdx) {
        return functor2ArgIdx.get(functor)[argIdx].keySet();
    }

    public Map<String, Set<Predicate>> getArgIndices(String functor, int argIdx) {
        return functor2ArgIdx.get(functor)[argIdx];
    }

    public Map<String, Set<Predicate>>[] getAllArgIndices(String functor) {
        return functor2ArgIdx.get(functor);
    }

    public void proveFact(Predicate fact) {
        provedFacts.add(fact);
    }

    public boolean hasProved(Predicate predicate) {
        return provedFacts.contains(predicate);
    }

    public boolean containsFact(Predicate predicate) {
        return originalKB.contains(predicate);
    }

    public Set<String> allConstants() {
        return constants;
    }

    public Iterator<Predicate> factIterator() {
        return originalKB.iterator();
    }

    public Set<Predicate> getOriginalKB() {
        return originalKB;
    }

    public Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
        return functor2PromisingConstMap;
    }

    public List<String> getAllFunctors() {
        return new ArrayList<>(functor2ArityMap.keySet());
    }

    public Map<String, Integer> getFunctor2ArityMap() {
        return functor2ArityMap;
    }

    public int getTotalConstantSubstitutions() {
        int cnt = 0;
        for (MultiSet<String>[] constant_sets: functor2ArgSetsMap.values()) {
            for (MultiSet<String> constant_set: constant_sets) {
                cnt += constant_set.differentValues();
            }
        }
        return cnt;
    }

    public int getActualConstantSubstitutions() {
        int cnt = 0;
        for (List<String>[] promising_constant_lists: functor2PromisingConstMap.values()) {
            for (List<String> promising_constant_list: promising_constant_lists) {
                cnt += promising_constant_list.size();
            }
        }
        return cnt;
    }

    public Set<String> getAllConstants() {
        return constants;
    }
}
