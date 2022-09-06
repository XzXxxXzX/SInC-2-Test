package sinc.impl.cached;

import sinc.common.Eval;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CachedQueryMonitor {
    private static final int DENOMINATOR = 1000000;

    public long preComputingCostInNano = 0;
    public long allEntailQueryCostInNano = 0;
    public long posEntailQueryCostInNano = 0;

    public long boundExistVarCostInNano = 0;
    public long boundExistVarInNewPredCostInNano = 0;
    public long boundNewVarCostInNano = 0;
    public long boundNewVarInNewPredCostInNano = 0;
    public long boundConstCostInNano = 0;

    public long cloneCostInNano = 0;
    public int totalClones = 0;

    public static class CacheStat {
        public final int headCachedEntries;
        public final int bodyCachedEntries;
        public final int cartesianOperations;

        public CacheStat(int headCachedEntries, int bodyCachedEntries, int cartesianOperations) {
            this.headCachedEntries = headCachedEntries;
            this.bodyCachedEntries = bodyCachedEntries;
            this.cartesianOperations = cartesianOperations;
        }
    }
    public final List<CacheStat> cacheStats = new ArrayList<>();
    public final List<Eval> evalStats = new ArrayList<>();

    public void show(PrintWriter writer) {
        writer.println("### Cached Query Monitored Info ###\n");
        writer.println("--- Time Cost ---");
        writer.printf(
                "T(ms) %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n",
                "[Pre", "AllEntail", "+Entail]", "[EBV", "EBV+", "NBV", "NBV+", "Const]", "#clone", "clone"
        );
        writer.printf(
                "      %10d %10d %10d %10d %10d %10d %10d %10d %10d %10d\n\n",
                preComputingCostInNano / DENOMINATOR,
                allEntailQueryCostInNano / DENOMINATOR,
                posEntailQueryCostInNano / DENOMINATOR,
                boundExistVarCostInNano / DENOMINATOR,
                boundExistVarInNewPredCostInNano / DENOMINATOR,
                boundNewVarCostInNano / DENOMINATOR,
                boundNewVarInNewPredCostInNano / DENOMINATOR,
                boundConstCostInNano / DENOMINATOR,
                totalClones,
                cloneCostInNano / DENOMINATOR
        );

        writer.println("--- Cache Entry Statistics ---");
        CacheStat max_head = cacheStats.get(0);
        CacheStat max_body = cacheStats.get(0);
        CacheStat max_cart_opt = cacheStats.get(0);
        int[] head_entries = new int[cacheStats.size()];
        int[] body_entries = new int[cacheStats.size()];
        int[] cartesian_operations = new int[cacheStats.size()];
        for (int i = 0; i < cacheStats.size(); i++) {
            CacheStat cache_stat = cacheStats.get(i);
            head_entries[i] = cache_stat.headCachedEntries;
            body_entries[i] = cache_stat.bodyCachedEntries;
            cartesian_operations[i] = cache_stat.cartesianOperations;
            max_head = (cache_stat.headCachedEntries > max_head.headCachedEntries) ? cache_stat : max_head;
            max_body = (cache_stat.bodyCachedEntries > max_body.bodyCachedEntries) ? cache_stat : max_body;
            max_cart_opt = (cache_stat.cartesianOperations > max_cart_opt.cartesianOperations) ? cache_stat : max_cart_opt;
        }
        writer.printf(
                "- Max Head Cache Entries: %d (%d in body, %d cartesian operations)\n",
                max_head.headCachedEntries, max_head.bodyCachedEntries, max_head.cartesianOperations
        );
        writer.printf(
                "- Max Body Cache Entries: %d (%d in head, %d cartesian operations)\n",
                max_body.bodyCachedEntries, max_body.headCachedEntries, max_body.cartesianOperations
        );
        writer.printf(
                "- Max Cartesian Operations: %d (%d cached entries in head, %d in body)\n",
                max_cart_opt.cartesianOperations, max_cart_opt.headCachedEntries, max_cart_opt.bodyCachedEntries
        );
        writer.print("- Head Cache Entries: ");
        writer.println(Arrays.toString(head_entries));
        writer.print("- Body Cache Entries: ");
        writer.println(Arrays.toString(body_entries));
        writer.print("- Cartesian Operations: ");
        writer.println(Arrays.toString(cartesian_operations));
        writer.println();

        writer.println("--- Evaluation Statistics ---");
        double max_pos_ent = 0;
        double total_pos_ent = 0;
        double max_neg_ent = 0;
        double total_neg_ent = 0;
        double max_ent = 0;
        double total_ent = 0;
        for (Eval eval: evalStats) {
            max_pos_ent = Math.max(max_pos_ent, eval.getPosCnt());
            total_pos_ent += eval.getPosCnt();
            max_neg_ent = Math.max(max_neg_ent, eval.getNegCnt());
            total_neg_ent += eval.getNegCnt();
            max_ent = Math.max(max_ent, eval.getAllCnt());
            total_ent += eval.getAllCnt();
        }
        writer.printf(
                "# %10s %10s %10s %10s %10s %10s\n",
                "max(+Ent)", "avg(+Ent)", "max(-Ent)", "avg(-Ent)", "max(Ent)", "avg(Ent)"
        );
        writer.printf(
                "  %10.0f %10.0f %10.0f %10.0f %10.0f %10.0f\n\n",
                max_pos_ent, total_pos_ent / evalStats.size(),
                max_neg_ent, total_neg_ent / evalStats.size(),
                max_ent, total_ent / evalStats.size()
        );
    }
}
