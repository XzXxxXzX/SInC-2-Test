package sinc.impl.pruned.tabu;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabuMonitor {
    /* Statistics Monitor */
    public int totalTabus = 0;
    public int totalCategories = 0;
    public List<Integer> tabusInDiffHeadFunctor = new ArrayList<>();
    public List<Integer> categoriesInDiffHeadFunctor = new ArrayList<>();

    public void show(PrintWriter writer) {
        writer.println("### Tabu Performance Info ###\n");
        writer.println("--- Statistics ---");
        writer.printf(" %10s %10s %10s\n", "#Total", "#/Head", "#Cat/Head");
        writer.printf(" %10d %10d %10d\n\n", totalTabus, totalTabus / tabusInDiffHeadFunctor.size(), totalCategories / categoriesInDiffHeadFunctor.size());
        writer.print("- #Tabu In Different Head Functors: ");
        writer.println(Arrays.toString(tabusInDiffHeadFunctor.toArray(new Integer[0])));
        writer.print("- #Category In Different Head Functors: ");
        writer.println(Arrays.toString(categoriesInDiffHeadFunctor.toArray(new Integer[0])));
    }
}
