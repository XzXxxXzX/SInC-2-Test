package sinc2;

import sinc2.common.Predicate;
import sinc2.kb.*;
import sinc2.rule.Rule;
import sinc2.util.graph.FeedbackVertexSetSolver;
import sinc2.util.graph.GraphNode;
import sinc2.util.graph.Tarjan;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * The abstract class for SInC. The overall compression procedure is implemented here.
 *
 * @since 1.0
 */
public abstract class SInC {

    /** The log file name in the compressed KB */
    public static final String LOG_FILE_NAME = "log.meta";
    /** The output content (stdout/stderr) file name */
    public static final String STD_OUTPUT_FILE_NAME = "std.meta";
    /** The command string to interrupt the compression workflow */
    public static final String INTERRUPT_CMD = "stop";

    /**
     * A flag denoting the interruption of the workflow. It is shared read-only by all worker threads and shared
     * write-only to the daemon thread.
     */
    public static boolean interrupted = false;

    /** The axiom node refers to the "⊥" node in the dependency graph */
    protected static final GraphNode<Predicate> AXIOM_NODE = new GraphNode<>(new Predicate(0, 0));

    /* Runtime configurations */
    /** SInC configuration */
    protected final SincConfig config;
    /** The logger */
    protected final PrintWriter logger;

    /* Compression related data */
    /** The input KB */
    protected NumeratedKb kb;
    /** The compressed KB */
    protected CompressedKb compressedKb;
    /** A mapping from predicates to the nodes in the dependency graph */
    protected final Map<Predicate, GraphNode<Predicate>> predicate2NodeMap = new HashMap<>();
    /**
     * The dependency graph, in the form of an adjacent list.
     * Note: In the implementation, the set of neighbours in the adjacent list refers to the in-neighbours instead of
     * out-neighbours.
     */
    protected final Map<GraphNode<Predicate>, Set<GraphNode<Predicate>>> dependencyGraph = new HashMap<>();

    /**
     * Create a SInC object with configurations.
     *
     * @param config The configurations
     */
    public SInC(SincConfig config) {
        this.config = config;

        /* Create writer objects to log and std output files */
        PrintWriter writer;
        try {
            writer = new PrintWriter(Paths.get(config.dumpPath, config.dumpName, LOG_FILE_NAME).toFile());
        } catch (IOException e) {
            writer = new PrintWriter(System.out);
        }
        this.logger = writer;
        PrintStream stream;
        try {
            stream = new PrintStream(Paths.get(config.dumpPath, config.dumpName, STD_OUTPUT_FILE_NAME).toFile());
        } catch (IOException e) {
            stream = null;
        }
        if (null != stream) {
            System.setOut(stream);
            System.setErr(stream);
        }

        Rule.MIN_FACT_COVERAGE = config.minFactCoverage;
        KbRelation.MIN_CONSTANT_COVERAGE = config.minConstantCoverage;
    }

    /**
     * Load a KB (in the format of Numerated KB) and return the KB
     */
    protected NumeratedKb loadKb() throws KbException, IOException {
        NumeratedKb kb =  new NumeratedKb(config.kbName, config.basePath);
        kb.updatePromisingConstants();
        return kb;
    }

    /**
     * The relations that will be the targets of rule mining procedures. By default, all relations are the targets.
     * This function can be overridden to customize the target list.
     */
    protected List<Integer> getTargetRelations() {
        List<Integer> relations = new ArrayList<>();
        for (KbRelation relation: kb.getRelations()) {
            relations.add(relation.getNumeration());
        }
        return relations;
    }

    /**
     * Determine the necessary set.
     */
    protected void dependencyAnalysis() throws KbException {
        /* The KB has already been updated by the relation miners. Here we only need to find the nodes with no in-degree
         * and those in the MFVS solution */
        /* Find all nodes that are not entailed */
        for (KbRelation relation: kb.getRelations()) {
            for (Record record: relation.getRecords()) {
                GraphNode<Predicate> node = new GraphNode<>(new Predicate(relation.getNumeration(), record.args));
                if (!dependencyGraph.containsKey(node)) {
                    compressedKb.addRecord(relation.getNumeration(), record);
                }
            }
        }

        /* Find all SCCs */
        final Tarjan<GraphNode<Predicate>> tarjan = new Tarjan<>(dependencyGraph, false);
        final List<Set<GraphNode<Predicate>>> sccs = tarjan.run();
        for (Set<GraphNode<Predicate>> scc: sccs) {
            /* 找出FVS的一个解，并把之放入start_set */
            /* Find a solution of MFVS and add to "necessaries" */
            final FeedbackVertexSetSolver<GraphNode<Predicate>> fvs_solver =
                    new FeedbackVertexSetSolver<>(dependencyGraph, scc);
            final Set<GraphNode<Predicate>> fvs = fvs_solver.run();
            for (GraphNode<Predicate> node: fvs) {
                compressedKb.addRecord(node.content.functor, node.content.args);
            }
        }
    }

    /**
     * Recover from the compressed KB to verify the correctness of the compression.
     *
     * @return Whether the compressed KB can be recovered to the original one.
     */
    public boolean recover() {
        /* Todo: Implement Here */
        return false;
    }

    /**
     * Dump the compressed KB
     */
    protected void dumpCompressedKb() throws IOException {
        compressedKb.dump(config.dumpPath);
    }

    protected void showMonitor() {
        logger.flush();
    }

    public CompressedKb getCompressedKb() {
        return compressedKb;
    }

    protected abstract RelationMiner createRelationMiner(int targetRelationNum);

    /**
     * The compress procedure.
     */
    private void compress() {
        /* Load KB */
        try {
            kb = loadKb();
        } catch (KbException | IOException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] KB Load failed, abort.");
            return;
        }
        compressedKb = new CompressedKb(config.dumpName, kb);

        /* Run relation miners on each relation */
        try {
            final List<Integer> target_relations = getTargetRelations();
            for (Integer relation_num: target_relations) {
                RelationMiner relation_miner = createRelationMiner(relation_num);
                relation_miner.run();
                KbRelation ce_relation = compressedKb.getCounterexampleRelation(relation_num);
                ce_relation.addRecords(relation_miner.getCounterexamples());
                for (Rule r: relation_miner.getHypothesis()) {
                    compressedKb.addHypothesisRule(r);
                }
            }
        } catch (KbException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Relation Miner failed. Interrupt");
            interrupted = true;
        }

        /* Dependency analysis */
        try {
            dependencyAnalysis();
        } catch (KbException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Dependency Analysis failed. Abort.");

            /* Log the hypothesis */
            logger.println("\n### Hypothesis Found ###");
            for (Rule rule : compressedKb.getHypothesis()) {
                logger.println(rule);
            }
            logger.println();
            return;
        }

        /* Log the hypothesis */
        logger.println("\n### Hypothesis Found ###");
        for (Rule rule : compressedKb.getHypothesis()) {
            logger.println(rule);
        }
        logger.println();

        /* Dump the compressed KB */
        try {
            dumpCompressedKb();
        } catch (IOException e) {
            e.printStackTrace(logger);
            logger.println("[ERROR] Compressed KB dump failed. Abort.");
            return;
        }

        /* 检查结果 */
        if (config.validation) {
            if (!recover()) {
                logger.println("[ERROR] Validation Failed");
            }
        }

        showMonitor();

        /* Todo: Upload to neo4j in debug mode */

    }

    /**
     * Run the compression and an interruption daemon.
     */
    public final void run() {
        interrupted = false;
        Thread task = new Thread(this::compress);
        task.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (!interrupted && task.isAlive()) {
                if (System.in.available() > 0) {
                    String line = reader.readLine();
                    if (INTERRUPT_CMD.equals(line)) {
                        break;
                    }
                }
                Thread.sleep(1000);
            }
            logger.flush();
        } catch (Exception e) {
            e.printStackTrace(logger);
        } finally {
            interrupted = true;
            try {
                task.join();
                logger.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
