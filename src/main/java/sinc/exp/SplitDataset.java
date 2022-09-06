package sinc.exp;

import sinc.common.Argument;
import sinc.common.Constant;
import sinc.common.Dataset;
import sinc.common.Predicate;
import sinc.impl.cached.MemKB;
import sinc.util.MultiSet;
import sinc.util.graph.BaseGraphNode;
import sinc.util.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.ToIntFunction;

public class SplitDataset extends MemKB {

    static final int CONST_ID = 0;

//    static class ColumnInfo {
//        final String functor;
//        final int index;
//
//        public ColumnInfo(String functor, int index) {
//            this.functor = functor;
//            this.index = index;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            ColumnInfo that = (ColumnInfo) o;
//            return index == that.index && functor.equals(that.functor);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(functor, index);
//        }
//    }

    static class ConnectivityInfo {
        final double threshold;
//        final Integer[] sccSizes;
        final Set<String>[] sccs;

        public ConnectivityInfo(double threshold, Set<String>[] sccs) {
            this.threshold = threshold;
            this.sccs = sccs;
        }
    }

    Map<BaseGraphNode<String>, Set<BaseGraphNode<String>>> connectivityGraph;  // Nodes representing functors

    void loadKB(Dataset dataset) throws IOException {
        System.out.println("Load Dataset...");
        BufferedReader reader = new BufferedReader(new FileReader(dataset.getPath()));
        String line;
        while (null != (line = reader.readLine())) {
            final String[] components = line.split("\t");
            final Predicate predicate = new Predicate(components[0], components.length - 1);
            for (int i = 1; i < components.length; i++) {
                predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
            }
            addFact(predicate);
        }
    }

    void loadKB(String datasetPath) throws IOException {
        System.out.println("Load Dataset...");
        BufferedReader reader = new BufferedReader(new FileReader(datasetPath));
        String line;
        while (null != (line = reader.readLine())) {
            final String[] components = line.split("\t");
            final Predicate predicate = new Predicate(components[0], components.length - 1);
            for (int i = 1; i < components.length; i++) {
                predicate.args[i - 1] = new Constant(CONST_ID, components[i]);
            }
            addFact(predicate);
        }
    }

    List<ConnectivityInfo> constructConnectivityGraph(int steps) {
        System.out.println("Construct Connectivity Graph...");
        /* Initiate Graph */
        connectivityGraph = new HashMap<>();
        Map<String, BaseGraphNode<String>> node_map = new HashMap<>();
        for (String functor: functor2ArityMap.keySet()) {
            BaseGraphNode<String> node = new BaseGraphNode<>(functor);
            node_map.put(functor, node);
            connectivityGraph.put(node, new HashSet<>());
        }

        /* Calculate Similarities */
        System.out.println("\tcalculate similarities...");
        class SimInfo {
            final String func1, func2;
            final double similarity;

            public SimInfo(String func1, String func2, double similarity) {
                this.func1 = func1;
                this.func2 = func2;
                this.similarity = similarity;
            }
        }
        List<SimInfo> similarities = new ArrayList<>();
        Map.Entry<String, MultiSet<String>[]>[] entries = functor2ArgSetsMap.entrySet().toArray(new Map.Entry[0]);
        for (int i = 0; i < entries.length; i++) {
            String functor1 = entries[i].getKey();
            MultiSet<String>[] arg_sets1 = entries[i].getValue();
            for (int j = i + 1; j < entries.length; j++) {
                String functor2 = entries[j].getKey();
                MultiSet<String>[] arg_sets2 = entries[j].getValue();
                double max_sim = 0;
                for (MultiSet<String> art_set1 : arg_sets1) {
                    for (MultiSet<String> arg_set2 : arg_sets2) {
                        double similarity = art_set1.jaccardSimilarity(arg_set2);
//                        max_sim = Math.max(max_sim, similarity);
                        max_sim = (max_sim >= similarity) ? max_sim : similarity;
                    }
                }
                similarities.add(new SimInfo(functor1, functor2, max_sim));
            }
        }
        similarities.sort(Comparator.comparingDouble(s -> s.similarity));

        /* Add Edges */
        System.out.println("\tadd edges...");
        List<ConnectivityInfo> connectivity_info_list = new ArrayList<>();
        connectivity_info_list.add(new ConnectivityInfo(0, new Set[]{new HashSet(functor2ArityMap.keySet())}));  // At the beginning, there is always one SCC containing all nodes

        final double max_sim = similarities.get(similarities.size() - 1).similarity;
        final double step_size = max_sim / steps;
        double threshold = step_size;  // Start from the first step

        for (SimInfo sim_info : similarities) {
            if (threshold <= sim_info.similarity) {
                BaseGraphNode<String> node1 = node_map.get(sim_info.func1);
                BaseGraphNode<String> node2 = node_map.get(sim_info.func2);
                connectivityGraph.get(node1).add(node2);
                connectivityGraph.get(node2).add(node1);
            }
        }

        int edge_idx = 0;
        while (threshold < max_sim) {
            System.out.printf("\tuse threshold: %f\n", threshold);

            /* Calculate on the graph structure */
            Tarjan<BaseGraphNode<String>> tarjan = new Tarjan<>(connectivityGraph);
            List<Set<BaseGraphNode<String>>> sccs = tarjan.run();
            Set<String>[] scc_set = new Set[sccs.size()];
            int scc_idx = 0;
            for (Set<BaseGraphNode<String>> scc: sccs) {
                Set<String> scc_relations = new HashSet<>();
                for (BaseGraphNode<String> node: scc) {
                    scc_relations.add(node.content);
                }
                scc_set[scc_idx] = scc_relations;
                scc_idx++;
            }
//            Integer[] scc_sizes = new Integer[sccs.size()];
//            for (int i = 0; i < scc_sizes.length; i++) {
//                scc_sizes[i] = sccs.get(i).size();
//            }
            Arrays.sort(scc_set, Comparator.comparingInt((ToIntFunction<Set<String>>) Set::size).reversed());
            connectivity_info_list.add(new ConnectivityInfo(threshold, scc_set));

            /* Update threshold & remove edges */
            threshold += step_size;
            for (; edge_idx < similarities.size() && similarities.get(edge_idx).similarity < threshold; edge_idx++) {
                SimInfo sim_info = similarities.get(edge_idx);
                BaseGraphNode<String> node1 = node_map.get(sim_info.func1);
                BaseGraphNode<String> node2 = node_map.get(sim_info.func2);
                connectivityGraph.get(node1).remove(node2);
                connectivityGraph.get(node2).remove(node1);
            }

            /* clear node marks */
            for (BaseGraphNode<String> node: node_map.values()) {
                node.index = BaseGraphNode.NO_TARJAN_INDEX;
                node.lowLink = BaseGraphNode.NO_TARJAN_LOW_LINK;
                node.onStack = false;
            }
        }

        return connectivity_info_list;
    }

    void dumpObservation(List<ConnectivityInfo> connectivityList, String fname) throws IOException {
        System.out.println("Dump result...");
        PrintWriter writer = new PrintWriter(fname);
        writer.print('[');
        ConnectivityInfo info = connectivityList.get(0);
        writer.printf("{\"%s\":%f,\"%s\":%s}", "threshold", info.threshold, "sccSizes", buildSizeString(info.sccs));
        for (int i = 1; i < connectivityList.size(); i++) {
            info = connectivityList.get(i);
            writer.printf(",\n{\"%s\":%f,\"%s\":%s}", "threshold", info.threshold, "sccSizes", buildSizeString(info.sccs));
        }
        writer.print(']');
        writer.close();
    }

    String buildSizeString(Set<String>[] sets) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (0 < sets.length) {
            builder.append(sets[0].size());
            for (int i = 1; i < sets.length; i++) {
                builder.append(',').append(sets[i].size());
            }
        }
        builder.append(']');
        return builder.toString();
    }

    int totalSccSize(Set<String>[] sets) {
        int cnt = 0;
        for (Set<String> scc: sets) {
            cnt += scc.size();
        }
        return cnt;
    }

    void dumpSplitDatasets(List<ConnectivityInfo> connectivityList, String fnamePrefix) throws IOException {
        /* Find the pivot point */
        int delta_scc_cnt = 0;
//        int total_scc_size = functor2ArgIdx.size();
        int pivot_idx = -1;
        for (int i = 1; i < connectivityList.size(); i++) {
            Set<String>[] scc_sets = connectivityList.get(i).sccs;
            int delta_scc_cnt2 = scc_sets.length - connectivityList.get(i-1).sccs.length;
//            int total_scc_size2 = totalSccSize(scc_sets);
            if (delta_scc_cnt2 < delta_scc_cnt) {
                pivot_idx = i-1;
                break;
            }
            delta_scc_cnt = delta_scc_cnt2;
//            total_scc_size = total_scc_size2;
        }

        /* Dump the scc at the pivot point */
        ConnectivityInfo ci = connectivityList.get(pivot_idx);
        Set<String> remaining_functors = new HashSet<>(functor2ArityMap.keySet());
        int scc_idx = 0;
        for (; scc_idx < ci.sccs.length; scc_idx++) {
            Set<String> scc = ci.sccs[scc_idx];
            PrintWriter writer = new PrintWriter(String.format("%s_split_%d.tsv", fnamePrefix, scc_idx));
            for (String functor : scc) {
                Set<Predicate> facts = functor2Facts.get(functor);
                remaining_functors.remove(functor);
                for (Predicate fact: facts) {
                    writer.print(fact.functor);
                    for (Argument argument: fact.args) {
                        writer.print('\t');
                        writer.print(argument.name);
                    }
                    writer.print('\n');
                }
            }
            writer.close();
        }

        /* Dump the unrecorded functors (single point scc) */
        for (String functor : remaining_functors) {
            PrintWriter writer = new PrintWriter(String.format("%s_split_%d.tsv", fnamePrefix, scc_idx));
            scc_idx++;
            Set<Predicate> facts = functor2Facts.get(functor);
            for (Predicate fact: facts) {
                writer.print(fact.functor);
                for (Argument argument: fact.args) {
                    writer.print('\t');
                    writer.print(argument.name);
                }
                writer.print('\n');
            }
            writer.close();
        }
    }

    void split(String datasetPath, int steps) throws IOException {
        long time_start = System.currentTimeMillis();
        loadKB(datasetPath);
        List<ConnectivityInfo> connectivityInfos = constructConnectivityGraph(steps);
        dumpObservation(connectivityInfos, datasetPath.replaceAll("[.]tsv$", "_connectivity.json"));
        String path_prefix = datasetPath.replaceAll("[.]tsv$", "");
        dumpSplitDatasets(connectivityInfos, path_prefix);
        long time_done = System.currentTimeMillis();
        System.out.printf("Time: %d ms", time_done - time_start);
    }

    public static void main(String[] args) throws IOException {
        if (2 > args.length) {
            System.out.println("Usage: <data file path> <steps>");
        }
        String data_file_path = args[0];
        int steps = Integer.parseInt(args[1]);
        SplitDataset splitDataset = new SplitDataset();
        splitDataset.split(data_file_path, steps);
    }
}
