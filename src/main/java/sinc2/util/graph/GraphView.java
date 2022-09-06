package sinc2.util.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Todo: The debugger helper which synchronize the graph to a Neo4j database to intuitively view the graph.
 *
 * @since 1.0
 */
public class GraphView {
    public static interface RedundancyChecker<T> {
        boolean isRedundant(T fact);
    }

    static public <T> void draw(
            Iterator<T> nodeItr, Map<GraphNode<T>, Set<GraphNode<T>>> edges,
            RedundancyChecker<T> checker
    ) {
        System.out.print("Printing Dependency Graph in Neo4j...");

        /* Connect to Neo4j */
        Driver driver = GraphDatabase.driver(
                "bolt://localhost:7687", AuthTokens.basic("neo4j", "kbcore")
        );

        /* Clear DB */
        Session session = driver.session();
        session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));
        session.writeTransaction(tx -> tx.run("CALL apoc.schema.assert({},{},true) YIELD label, key"));

        /* Create Nodes */
        while (nodeItr.hasNext()) {
            T node = nodeItr.next();
            if (checker.isRedundant(node)) {
                session.writeTransaction(tx -> tx.run(String.format("CREATE (e:`proved`{name:\"%s\"})", node.toString())));
            } else {
                session.writeTransaction(tx -> tx.run(String.format("CREATE (e:`remained`{name:\"%s\"})", node.toString())));
            }
        }

        /* Create Edges */
        for (Map.Entry<GraphNode<T>, Set<GraphNode<T>>> entry: edges.entrySet()) {
            GraphNode<T> src = entry.getKey();
            for (GraphNode<T> dst: entry.getValue()) {
                session.writeTransaction(tx -> tx.run(
                        String.format(
                                "MATCH (s{name:\"%s\"}), (d{name:\"%s\"}) CREATE (s)-[r:`rel`]->(d)",
                                src.content.toString(), dst.content.toString()
                        )
                ));
            }
        }

        session.close();
        driver.close();
        System.out.println("Done");
    }
}
