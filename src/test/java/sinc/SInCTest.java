package sinc;

import org.junit.jupiter.api.Test;
import sinc.common.*;
import sinc.impl.cached.MemKB;
import sinc.impl.cached.recal.RecalculateCachedRule;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SInCTest {

    static final String FUNCTOR_FATHER = "father";
    static final String FUNCTOR_PARENT = "parent";
    static final String FUNCTOR_GRANDPARENT = "grandParent";
    static final int ARITY_FATHER = 2;
    static final int ARITY_PARENT = 2;
    static final int ARITY_GRANDPARENT = 2;
    static final int CONST_ID = -1;

    static MemKB kbFamily() {
        final MemKB kb = new MemKB();

        /* father(X, Y):
         *   f1, s1
         *   f2, s2
         *   f2, d2
         *   f3, s3
         *   f4, d4
         */
        Predicate father1 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father1.args[0] = new Constant(CONST_ID, "f1");
        father1.args[1] = new Constant(CONST_ID, "s1");
        Predicate father2 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father2.args[0] = new Constant(CONST_ID, "f2");
        father2.args[1] = new Constant(CONST_ID, "s2");
        Predicate father3 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father3.args[0] = new Constant(CONST_ID, "f2");
        father3.args[1] = new Constant(CONST_ID, "d2");
        Predicate father4 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father4.args[0] = new Constant(CONST_ID, "f3");
        father4.args[1] = new Constant(CONST_ID, "s3");
        Predicate father5 = new Predicate(FUNCTOR_FATHER, ARITY_FATHER);
        father5.args[0] = new Constant(CONST_ID, "f4");
        father5.args[1] = new Constant(CONST_ID, "d4");
        kb.addFact(father1);
        kb.addFact(father2);
        kb.addFact(father3);
        kb.addFact(father4);
        kb.addFact(father5);

        /* parent(X, Y):
         *   f1, s1
         *   f1, d1
         *   f2, s2
         *   f2, d2
         *   m2, d2
         *   g1, f1
         *   g2, f2
         *   g2, m2
         *   g3, f3
         */
        Predicate parent1 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent1.args[0] = new Constant(CONST_ID, "f1");
        parent1.args[1] = new Constant(CONST_ID, "s1");
        Predicate parent2 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent2.args[0] = new Constant(CONST_ID, "f1");
        parent2.args[1] = new Constant(CONST_ID, "d1");
        Predicate parent3 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent3.args[0] = new Constant(CONST_ID, "f2");
        parent3.args[1] = new Constant(CONST_ID, "s2");
        Predicate parent4 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent4.args[0] = new Constant(CONST_ID, "f2");
        parent4.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent5 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent5.args[0] = new Constant(CONST_ID, "m2");
        parent5.args[1] = new Constant(CONST_ID, "d2");
        Predicate parent6 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent6.args[0] = new Constant(CONST_ID, "g1");
        parent6.args[1] = new Constant(CONST_ID, "f1");
        Predicate parent7 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent7.args[0] = new Constant(CONST_ID, "g2");
        parent7.args[1] = new Constant(CONST_ID, "f2");
        Predicate parent8 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent8.args[0] = new Constant(CONST_ID, "g2");
        parent8.args[1] = new Constant(CONST_ID, "m2");
        Predicate parent9 = new Predicate(FUNCTOR_PARENT, ARITY_PARENT);
        parent9.args[0] = new Constant(CONST_ID, "g3");
        parent9.args[1] = new Constant(CONST_ID, "f3");
        kb.addFact(parent1);
        kb.addFact(parent2);
        kb.addFact(parent3);
        kb.addFact(parent4);
        kb.addFact(parent5);
        kb.addFact(parent6);
        kb.addFact(parent7);
        kb.addFact(parent8);
        kb.addFact(parent9);

        /* grandParent(X, Y):
         *   g1, s1
         *   g2, d2
         *   g4, s4
         */
        Predicate grand1 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand1.args[0] = new Constant(CONST_ID, "g1");
        grand1.args[1] = new Constant(CONST_ID, "s1");
        Predicate grand2 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand2.args[0] = new Constant(CONST_ID, "g2");
        grand2.args[1] = new Constant(CONST_ID, "d2");
        Predicate grand3 = new Predicate(FUNCTOR_GRANDPARENT, ARITY_GRANDPARENT);
        grand3.args[0] = new Constant(CONST_ID, "g4");
        grand3.args[1] = new Constant(CONST_ID, "s4");
        kb.addFact(grand1);
        kb.addFact(grand2);
        kb.addFact(grand3);

        /* Constants(16):
         *   g1, g2, g3, g4
         *   f1, f2, f3, f4
         *   m2
         *   s1, s2, s3, s4
         *   d1, d2, d4
         */

        return kb;
    }

    static class SincImpl extends SInC {

        private MemKB kb;

        public SincImpl(SincConfig config, String kbPath, String dumpPath, String logPath) {
            super(config, kbPath, dumpPath, logPath);
        }

        @Override
        protected KbStatistics loadKb() {
            kb = kbFamily();
            return new KbStatistics(
                    kb.totalFacts(),
                    kb.getFunctor2ArityMap().size(),
                    kb.totalConstants(),
                    4,
                    kb.getTotalConstantSubstitutions(),
                    kb.similarColumnPairs(),
                    kb.totalColumnPairs()
            );
        }

        @Override
        protected List<String> getTargetFunctors() {
            return new ArrayList<>(Collections.singletonList(FUNCTOR_PARENT));
        }

        @Override
        protected Rule getStartRule(String headFunctor, Set<RuleFingerPrint> cache) {
            return new RecalculateCachedRule(headFunctor, cache, kb);
        }

        @Override
        protected Map<String, Integer> getFunctor2ArityMap() {
            return kb.getFunctor2ArityMap();
        }

        @Override
        protected Map<String, List<String>[]> getFunctor2PromisingConstantMap() {
            final Map<String, List<String>[]> map = new HashMap<>();
            final List<String>[] arr_father = new List[ARITY_FATHER];
            arr_father[0] = new ArrayList<>(Collections.singleton("f2"));
            arr_father[1] = new ArrayList<>();
            map.put(FUNCTOR_FATHER, arr_father);
            final List<String>[] arr_parent = new List[ARITY_PARENT];
            arr_parent[0] = new ArrayList<>(Arrays.asList("s2", "g4"));
            arr_parent[1] = new ArrayList<>(Collections.singleton("d4"));
            map.put(FUNCTOR_PARENT, arr_parent);
            final List<String>[] arr_grand = new List[ARITY_GRANDPARENT];
            arr_grand[0] = new ArrayList<>();
            arr_grand[1] = new ArrayList<>();
            map.put(FUNCTOR_GRANDPARENT, arr_grand);
            return map;
        }

        @Override
        protected UpdateResult updateKb(Rule rule) {
            RecalculateCachedRule forward_cached_rule = (RecalculateCachedRule) rule;
            return forward_cached_rule.updateInKb();
        }

        @Override
        protected Set<Predicate> getOriginalKb() {
            return kb.getOriginalKB();
        }

        @Override
        public Set<String> getAllConstants() {
            return kb.getAllConstants();
        }

        @Override
        protected void recordRuleStatus(Rule rule, Rule.UpdateStatus updateStatus) {}

        @Override
        public String getModelName() {
            return "TestImpl";
        }

        @Override
        protected boolean columnSimilar(String functor1, int idx1, String functor2, int idx2) {
            return true;
        }
    }

//    public static void main(String[] args) {
//        SincImpl sinc = new SincImpl(new SincConfig(
//                1,
//                false,
//                false,
//                2,
//                true,
//                Eval.EvalMetric.CompressionRate,
//                -1,
//                0,
//                0,
//                false,
//                -1,
//                false,
//                false),
//                null,null,null
//        );
//        sinc.run();
//        System.out.println("Finished");
//    }

    @Test
    void testFamily1() {
        SincImpl sinc = new SincImpl(new SincConfig(
                1,
                false,
                false,
                2,
                true,
                Eval.EvalMetric.CompressionRate,
                -1,
                0,
                0,
                1,
                false,
                -1,
                false,
                false),
                null,null,null
        );
        sinc.run();
        PerformanceMonitor monitor = sinc.getPerformanceMonitor();
        List<PerformanceMonitor.BranchInfo> expected_branch_infos = new ArrayList<>();
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(0, 14, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 20, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 20, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 27, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 9, 0));
        assertEquals(expected_branch_infos, monitor.branchProgress);
        assertEquals(22, monitor.invalidSearches);
        assertEquals(2, monitor.duplications);
        assertEquals(0, monitor.fcFilteredRules);
        assertEquals(29, monitor.totalConstantSubstitutions);
        assertEquals(4, monitor.actualConstantSubstitutions);
    }

    @Test
    void testFamily2() {
        SincImpl sinc = new SincImpl(new SincConfig(
                1,
                false,
                false,
                2,
                true,
                Eval.EvalMetric.CompressionRate,
                0.22, // FC < but close to 2/9
                0,
                0,
                1,
                false,
                -1,
                false,
                false),
                null,null,null
        );
        sinc.run();
        PerformanceMonitor monitor = sinc.getPerformanceMonitor();
        List<PerformanceMonitor.BranchInfo> expected_branch_infos = new ArrayList<>();
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(0, 7, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 8, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 5, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 10, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 3, 0));
        assertEquals(expected_branch_infos, monitor.branchProgress);
        assertEquals(22, monitor.invalidSearches);
        assertEquals(2, monitor.duplications);
        assertEquals(57, monitor.fcFilteredRules);
        assertEquals(29, monitor.totalConstantSubstitutions);
        assertEquals(4, monitor.actualConstantSubstitutions);
    }

    @Test
    void testFamily3() {
        SincImpl sinc = new SincImpl(new SincConfig(
                1,
                false,
                false,
                2,
                true,
                Eval.EvalMetric.CompressionRate,
                0.33, // FC < but close to 3/9
                0,
                0,
                1,
                false,
                -1,
                false,
                false),
                null,null,null
        );
        sinc.run();
        PerformanceMonitor monitor = sinc.getPerformanceMonitor();
        List<PerformanceMonitor.BranchInfo> expected_branch_infos = new ArrayList<>();
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(0, 7, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 7, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(1, 5, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 3, 0));
        expected_branch_infos.add(new PerformanceMonitor.BranchInfo(2, 9, 0));
        assertEquals(22, monitor.invalidSearches);
        assertEquals(6, monitor.duplications);
        assertEquals(56, monitor.fcFilteredRules);
        assertEquals(29, monitor.totalConstantSubstitutions);
        assertEquals(4, monitor.actualConstantSubstitutions);
        assertEquals(expected_branch_infos, monitor.branchProgress);
    }
}