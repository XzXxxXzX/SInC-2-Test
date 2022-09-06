package sinc.common;

import sinc.util.MultiSet;

import java.util.*;

/**
 * Todo: 当前的fingerprint会有误判，使得本来不同的rule判定为相同，例如：
 *
 *   1. p(X,Y) :- f(X,X), f(?,Y)
 *   2. p(X,Y) :- f(X,Y), f(?,X)
 *
 *   3. p(X,Y) :- f(X,?), f(Z,Y), f(?,Z)
 *   4. p(X,Y) :- f(X,Z), f(?,Y), f(Z,?)
 *
 * 上面两组rule在当前Fingerprint框架下会被判定为相同，但是这种情况比较特殊，只会在rule中出现多个相同functor的predicate时出现
 */
public class RuleFingerPrint {
    private final String headFunctor;
    private final MultiSet<ArgIndicator>[] headEquivClasses;
    /* 'otherEquivClasses'可以不必是Multiset，可以用Set代替，因为Extension操作中不会引入Independent Fragment */
    private final MultiSet<MultiSet<ArgIndicator>> otherEquivClasses;

    public final List<Predicate> rule;

    public RuleFingerPrint(List<Predicate> rule) {
        this.rule = rule;
        final Predicate head_predicate = rule.get(0);
        headFunctor = head_predicate.functor;
        headEquivClasses = new MultiSet[head_predicate.arity()];
        otherEquivClasses = new MultiSet<>();
        final Map<Integer, MultiSet<ArgIndicator>> bounded_equiv_classes = new HashMap<>();
        final Set<Integer> body_bv_ids = new HashSet<>();

        /* 先处理Head */
        for (int arg_idx = 0; arg_idx < head_predicate.arity(); arg_idx++) {
            final Argument argument = head_predicate.args[arg_idx];
            if (null == argument) {
                /* Free Var */
                headEquivClasses[arg_idx] = new MultiSet<>();
                headEquivClasses[arg_idx].add(new VarIndicator(head_predicate.functor, arg_idx));
            } else {
                if (argument.isVar) {
                    final int tmp_idx = arg_idx;
                    bounded_equiv_classes.compute(argument.id, (id, mset) -> {
                        if (null == mset) {
                            mset = new MultiSet<>();
                        }
                        mset.add(new VarIndicator(head_predicate.functor, tmp_idx));
                        headEquivClasses[tmp_idx] = mset;
                        return mset;
                    });
                } else {
                    /* Constant */
                    headEquivClasses[arg_idx] = new MultiSet<>();
                    headEquivClasses[arg_idx].add(new VarIndicator(head_predicate.functor, arg_idx));
                    headEquivClasses[arg_idx].add(new ConstIndicator(argument.name));
                }
            }
        }

        /* 再处理剩余的Body */
        for (int pred_idx = 1; pred_idx < rule.size(); pred_idx++) {
            Predicate body_predicate = rule.get(pred_idx);
            for (int arg_idx = 0; arg_idx < body_predicate.arity(); arg_idx++) {
                final Argument argument = body_predicate.args[arg_idx];
                if (null == argument) {
                    /* Free Var */
                    final MultiSet<ArgIndicator> new_equiv_class = new MultiSet<>();
                    new_equiv_class.add(new VarIndicator(body_predicate.functor, arg_idx));
                    otherEquivClasses.add(new_equiv_class);
                } else {
                    if (argument.isVar) {
                        final int tmp_idx = arg_idx;
                        bounded_equiv_classes.compute(argument.id, (id, mset) -> {
                            if (null == mset) {
                                mset = new MultiSet<>();
                                // otherEquivClasses.add(mset);
                                /* 这里不能直接添加mest，因为这个mest中的元素后面可能会变，导致其hash改变，从而使得equal()函数
                                 * 的结果错误。
                                 * Body中的所有BV对应的等价类应该等到完全构造完毕之后统一加入Multiset
                                 */
                                body_bv_ids.add(id);  // 标记body bv
                            }
                            mset.add(new VarIndicator(body_predicate.functor, tmp_idx));
                            return mset;
                        });
                    } else {
                        /* Constant */
                        MultiSet<ArgIndicator> new_equiv_class = new MultiSet<>();
                        new_equiv_class.add(new VarIndicator(body_predicate.functor, arg_idx));
                        new_equiv_class.add(new ConstIndicator(argument.name));
                        otherEquivClasses.add(new_equiv_class);
                    }
                }
            }
        }

        for (int id : body_bv_ids) {
            otherEquivClasses.add(bounded_equiv_classes.get(id));
        }
    }

    public String getHeadFunctor() {
        return headFunctor;
    }

    public MultiSet<ArgIndicator>[] getHeadEquivClasses() {
        return headEquivClasses;
    }

    public MultiSet<MultiSet<ArgIndicator>> getOtherEquivClasses() {
        return otherEquivClasses;
    }

    public boolean predecessorOf(RuleFingerPrint another) {
        final Set<MultiSet<ArgIndicator>> this_eqv_classes = new HashSet<>(Arrays.asList(headEquivClasses));
        this_eqv_classes.addAll(otherEquivClasses.distinctValues());
        final Set<MultiSet<ArgIndicator>> another_eqv_classes = new HashSet<>(Arrays.asList(another.headEquivClasses));
        another_eqv_classes.addAll(another.otherEquivClasses.distinctValues());
        for (MultiSet<ArgIndicator> this_eqv_class: this_eqv_classes) {
            boolean found_superset = false;
            for (MultiSet<ArgIndicator> another_eqv_class: another_eqv_classes) {
                if (this_eqv_class.subsetOf(another_eqv_class)) {
                    found_superset = true;
                    break;
                }
            }
            if (!found_superset) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleFingerPrint that = (RuleFingerPrint) o;
        return Objects.equals(headFunctor, that.headFunctor) &&
                Arrays.equals(headEquivClasses, that.headEquivClasses) &&
                Objects.equals(otherEquivClasses, that.otherEquivClasses);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(headFunctor, otherEquivClasses);
        result = 31 * result + Arrays.hashCode(headEquivClasses);
        return result;
    }
}
