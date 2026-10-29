public final class QueueModelTest {
    private static int checks;

    public static void main(String[] args) {
        testDynamicArray();
        testLinkedQueue();
        testStack();
        testHashTable();
        testBinarySearchTree();
        testGraphAlgorithms();
        System.out.println("QueueModelTest passed: " + checks + " checks");
    }

    private static void testDynamicArray() {
        QueueModel.DynamicArray<Integer> values = new QueueModel.DynamicArray<>();
        check(values.isEmpty(), "new array is empty");
        for (int i = 0; i < 10; i++) values.add(i);
        check(values.size() == 10, "array grows");
        check(values.get(9) == 9, "array reads last item");
        check(values.remove(0) == 0, "array removes first item");
        check(values.get(0) == 1, "array shifts after removal");
        expect(IndexOutOfBoundsException.class, () -> values.get(99));
    }

    private static void testLinkedQueue() {
        QueueModel.LinkedQueue<String> queue = new QueueModel.LinkedQueue<>();
        expect(java.util.NoSuchElementException.class, queue::dequeue);
        queue.enqueue("first");
        queue.enqueue("second");
        check(queue.peek().equals("first"), "queue peeks FIFO head");
        check(queue.dequeue().equals("first"), "queue removes FIFO head");
        check(queue.dequeue().equals("second"), "queue preserves FIFO order");
        check(queue.isEmpty(), "queue empties correctly");
    }

    private static void testStack() {
        QueueModel.Stack<Integer> stack = new QueueModel.Stack<>();
        expect(java.util.NoSuchElementException.class, stack::pop);
        stack.push(1);
        stack.push(2);
        check(stack.pop() == 2, "stack removes LIFO top");
        check(stack.pop() == 1, "stack preserves LIFO order");
    }

    private static void testHashTable() {
        QueueModel.HashTable<Integer, String> table = new QueueModel.HashTable<>(2);
        for (int i = 0; i < 20; i++) table.put(i, "value-" + i);
        check(table.size() == 20, "hash table stores entries");
        check(table.capacity() >= 32, "hash table rehashes as it grows");
        check(table.loadFactor() <= 0.75, "hash table maintains load factor");
        table.put(5, "updated");
        check(table.get(5).equals("updated"), "hash table updates an existing key");
        check(table.remove(5).equals("updated"), "hash table removes an entry");
        check(!table.containsKey(5), "hash table no longer contains removed key");
    }

    private static void testBinarySearchTree() {
        QueueModel.BinarySearchTree<Integer> tree = new QueueModel.BinarySearchTree<>();
        check(tree.insert(8), "tree inserts root");
        tree.insert(3);
        tree.insert(10);
        tree.insert(1);
        check(!tree.insert(8), "tree ignores duplicate values");
        check(tree.contains(1), "tree searches recursively");
        check(tree.remove(8), "tree removes a two-child node");
        QueueModel.DynamicArray<Integer> sorted = tree.inOrder();
        check(sorted.size() == 3 && sorted.get(0) == 1 && sorted.get(2) == 10,
                "tree traversal is sorted");
        check(tree.height() == 3, "tree reports height");
    }

    private static void testGraphAlgorithms() {
        QueueModel.Graph<String> graph = new QueueModel.Graph<>();
        graph.addEdge("A", "B", 2);
        graph.addEdge("B", "C", 3);
        graph.addVertex("ISOLATED");
        check(graph.breadthFirstSearch("A").size() == 3, "BFS visits connected vertices");
        check(graph.depthFirstSearch("A").size() == 3, "DFS visits connected vertices");
        QueueModel.DynamicArray<String> backtracked = graph.findPathBacktracking("A", "C");
        check(backtracked.size() == 3 && backtracked.get(0).equals("A")
                        && backtracked.get(2).equals("C"),
                "backtracking finds a path");
        QueueModel.Graph.Path<String> shortest = graph.shortestPath("A", "C");
        check(shortest.distance() == 5 && shortest.vertices().size() == 3,
                "Dijkstra finds the shortest path");
        check(!graph.shortestPath("A", "ISOLATED").isReachable(),
                "shortest path reports unavailable routes");
        expect(IllegalArgumentException.class, () -> graph.addEdge("A", "C", -1));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName());
        } catch (Throwable error) {
            if (!type.isInstance(error)) throw new AssertionError("Expected " + type.getSimpleName(), error);
            checks++;
        }
    }
}
