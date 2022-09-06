package sinc.impl.cached;

import sinc.common.Predicate;
import sinc.common.Rule;
import sinc.common.RuleFingerPrint;
import sinc.common.UpdateResult;

import java.util.List;
import java.util.Set;

public abstract class CachedRule extends Rule  {
    public final CachedQueryMonitor cacheMonitor = new CachedQueryMonitor();

    public CachedRule(String headFunctor, int arity, Set<RuleFingerPrint> searchedFingerprints) {
        super(headFunctor, arity, searchedFingerprints);
    }

    public CachedRule(Rule another) {
        super(another);
    }

    /**
     * @return 只返回那些首次被entail的head对应的一个grounding
     */
    public UpdateResult updateInKb() {
        return new UpdateResult(findGroundings(), findCounterExamples());
    }

    protected abstract Set<Predicate> findCounterExamples();

    protected abstract List<Predicate[]> findGroundings();

    protected abstract void releaseCache();

    @Override
    public final UpdateStatus removeBoundedArg(int predIdx, int argIdx) {
        /* Cached Rule 不支持向前做cache */
        return UpdateStatus.INVALID;
    }

    @Override
    protected final UpdateStatus removeBoundedArgHandler(int predIdx, int argIdx) {
        /* 这里也是什么都不做 */
        return UpdateStatus.INVALID;
    }
}
