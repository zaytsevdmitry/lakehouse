package lakehouse.api.graphexperemental;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// Class to represent a graph using adjacency list
class GraphBFS {
    int v;
    LinkedList<Integer>[] adjList;
    private final List<String> vertices;
    GraphBFS(List<String> vertices)
    {
        this.v = vertices.size();
        adjList = new LinkedList[vertices.size()];
        this.vertices = vertices;
        for (int i = 0; i < vertices.size(); ++i)
            adjList[i] = new LinkedList<>();
    }

    // Function to add an edge to the graph
    void addEdge(int u, int v) { adjList[u].add(v); }

    // Function to perform Breadth First Search on a graph
    // represented using adjacency list
    void bfs(int startNode)
    {
        List<GraphItem> graphItems = new ArrayList<>();
        System.out.println( " DAG-------------------");
        // Create a queue for BFS
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[v];

        // Mark the current node as visited and enqueue it
        visited[startNode] = true;
        queue.add(startNode);
        int iteration = 0;
        // Iterate over the queue
        while (!queue.isEmpty()) {
            // Dequeue a vertex from queue and print it
            int currentNode = queue.poll();
            System.out.println(vertices.get(currentNode) + " ");

            // Get all adjacent vertices of the dequeued
            // vertex currentNode If an adjacent has not
            // been visited, then mark it visited and
            // enqueue it
            for (int neighbor : adjList[currentNode]) {
               //if (!visited[neighbor]) {
                visited[neighbor] = true;
                System.out.printf("\t\t\t\t\t\t\t\t node %s level %d\n", vertices.get(neighbor), iteration);
                queue.add(neighbor);
                graphItems.add(new GraphItem(vertices.get(currentNode),vertices.get(neighbor),iteration,0));
                //}
            }
            iteration++;
        }

        System.out.println("---------------------lelvel detection");
        int lastIteration=0;
        int level=0;
        String lastFrom = "";
        String lastTo = "";

        for (GraphItem graphItem : graphItems) {
             if (lastFrom.equals(graphItem.getCurrent())
                && lastTo.equals(graphItem.getNext())){
                 continue;
             }
            if (!lastFrom.equals(graphItem.getCurrent())){
              if( !lastTo.equals(graphItem.getNext())) {
                  level++;
              }
            }
            lastIteration = graphItem.getIteration();
            graphItem.setLevel(level);
            System.out.printf("from %s      to   %s iteration %d |>> level %d%n",
                    graphItem.getCurrent(),
                    graphItem.getNext(),
                    graphItem.getIteration(),
                    graphItem.getLevel());
            lastFrom = graphItem.getCurrent();
            lastTo = graphItem.getNext();
        }


    }

}
