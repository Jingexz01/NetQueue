import java.util.NoSuchElementException;


public final class QueueModel {
    
    private QueueModel() {
    }

    
    public record Customer(String name, String type, int durationSeconds) {
        public Customer(String name, String type) {
            this(name, type, 60 * 60);
        }

        public Customer {
            
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer name is required");
            }
            name = name.trim();
            type = "VIP".equals(type) ? "VIP" : "Standard";
            if (durationSeconds < 60 || durationSeconds > 24 * 60 * 60) {
                throw new IllegalArgumentException("Session duration must be between 1 minute and 24 hours");
            }
        }
    }

    
    public static final class DynamicArray<T> {
        
        private Object[] items = new Object[4];
        private int size;

        public void add(T value) {
            
            ensureCapacity(size + 1);
            items[size++] = value;
        }

        public T get(int index) {
            
            checkIndex(index);
            @SuppressWarnings("unchecked") T value = (T) items[index];
            return value;
        }

        public T remove(int index) {
            
            checkIndex(index);
            @SuppressWarnings("unchecked") T removed = (T) items[index];
            System.arraycopy(items, index + 1, items, index, size - index - 1);
            items[--size] = null;
            return removed;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private void ensureCapacity(int required) {
            
            if (required <= items.length) return;
            Object[] expanded = new Object[items.length * 2];
            System.arraycopy(items, 0, expanded, 0, size);
            items = expanded;
        }

        private void checkIndex(int index) {
            
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
            }
        }
    }

    
    public static final class LinkedQueue<T> {
        
        private Node<T> first;
        private Node<T> last;
        private int size;

        public void enqueue(T value) {
            
            Node<T> node = new Node<>(value);
            if (last == null) first = node;
            else last.next = node;
            last = node;
            size++;
        }

        public T dequeue() {
            
            if (first == null) throw new NoSuchElementException("Queue is empty");
            T value = first.value;
            first = first.next;
            if (first == null) last = null;
            size--;
            return value;
        }

        public T peek() {
            
            if (first == null) throw new NoSuchElementException("Queue is empty");
            return first.value;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public DynamicArray<T> snapshot() {
            
            DynamicArray<T> copy = new DynamicArray<>();
            for (Node<T> node = first; node != null; node = node.next) copy.add(node.value);
            return copy;
        }

        private static final class Node<T> {
            
            private final T value;
            private Node<T> next;

            private Node(T value) {
                this.value = value;
            }
        }
    }

    
    public static final class Stack<T> {
        
        private Node<T> top;
        private int size;

        public void push(T value) {
            
            Node<T> node = new Node<>(value);
            node.next = top;
            top = node;
            size++;
        }

        public T pop() {
            
            if (top == null) throw new NoSuchElementException("Stack is empty");
            T value = top.value;
            top = top.next;
            size--;
            return value;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public int size() {
            return size;
        }

        private static final class Node<T> {
            
            private final T value;
            private Node<T> next;

            private Node(T value) {
                this.value = value;
            }
        }
    }

    





    public static final class HashTable<K, V> {
        
        private static final int DEFAULT_CAPACITY = 16;
        private static final double LOAD_FACTOR = 0.75;

        private Entry<K, V>[] buckets;
        private int size;

        @SuppressWarnings("unchecked")
        public HashTable() {
            
            buckets = new Entry[DEFAULT_CAPACITY];
        }

        @SuppressWarnings("unchecked")
        public HashTable(int initialCapacity) {
            if (initialCapacity < 1) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            int capacity = 1;
            while (capacity < initialCapacity) capacity *= 2;
            buckets = new Entry[capacity];
        }

        public void put(K key, V value) {
            
            if (key == null) throw new IllegalArgumentException("Key cannot be null");
            if ((double) (size + 1) / buckets.length > LOAD_FACTOR) resize();
            int index = indexFor(key, buckets.length);
            for (Entry<K, V> current = buckets[index]; current != null; current = current.next) {
                
                if (current.key.equals(key)) {
                    current.value = value;
                    return;
                }
            }
            buckets[index] = new Entry<>(key, value, buckets[index]);
            size++;
        }

        public V get(K key) {
            
            if (key == null) return null;
            int index = indexFor(key, buckets.length);
            for (Entry<K, V> current = buckets[index]; current != null; current = current.next) {
                if (current.key.equals(key)) return current.value;
            }
            return null;
        }

        public boolean containsKey(K key) {
            if (key == null) return false;
            int index = indexFor(key, buckets.length);
            for (Entry<K, V> current = buckets[index]; current != null; current = current.next) {
                if (current.key.equals(key)) return true;
            }
            return false;
        }

        public V remove(K key) {
            
            if (key == null) return null;
            int index = indexFor(key, buckets.length);
            Entry<K, V> current = buckets[index];
            Entry<K, V> previous = null;
            while (current != null) {
                if (current.key.equals(key)) {
                    if (previous == null) buckets[index] = current.next;
                    else previous.next = current.next;
                    size--;
                    return current.value;
                }
                previous = current;
                current = current.next;
            }
            return null;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public double loadFactor() {
            return (double) size / buckets.length;
        }

        public int capacity() {
            return buckets.length;
        }

        private int indexFor(K key, int length) {
            
            int hash = key.hashCode();
            hash ^= (hash >>> 16);
            return (hash & 0x7fffffff) % length;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            
            Entry<K, V>[] old = buckets;
            buckets = new Entry[old.length * 2];
            size = 0;
            for (Entry<K, V> head : old) {
                for (Entry<K, V> current = head; current != null; current = current.next) {
                    put(current.key, current.value);
                }
            }
        }

        private static final class Entry<K, V> {
            
            private final K key;
            private V value;
            private Entry<K, V> next;

            private Entry(K key, V value, Entry<K, V> next) {
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }
    }

    



    public static final class BinarySearchTree<T extends Comparable<T>> {
        private Node<T> root;
        private int size;

        public boolean insert(T value) {
            if (value == null) throw new IllegalArgumentException("Value cannot be null");
            if (contains(value)) return false;
            root = insert(root, value);
            size++;
            return true;
        }

        private Node<T> insert(Node<T> node, T value) {
            if (node == null) return new Node<>(value);
            if (value.compareTo(node.value) < 0) node.left = insert(node.left, value);
            else node.right = insert(node.right, value);
            return node;
        }

        public boolean contains(T value) {
            return value != null && contains(root, value);
        }

        private boolean contains(Node<T> node, T value) {
            if (node == null) return false;
            int comparison = value.compareTo(node.value);
            if (comparison == 0) return true;
            return comparison < 0 ? contains(node.left, value) : contains(node.right, value);
        }

        public boolean remove(T value) {
            if (!contains(value)) return false;
            root = remove(root, value);
            size--;
            return true;
        }

        private Node<T> remove(Node<T> node, T value) {
            if (node == null) return null;
            int comparison = value.compareTo(node.value);
            if (comparison < 0) node.left = remove(node.left, value);
            else if (comparison > 0) node.right = remove(node.right, value);
            else if (node.left == null) return node.right;
            else if (node.right == null) return node.left;
            else {
                Node<T> successor = smallest(node.right);
                node.value = successor.value;
                node.right = remove(node.right, successor.value);
            }
            return node;
        }

        private Node<T> smallest(Node<T> node) {
            return node.left == null ? node : smallest(node.left);
        }

        public DynamicArray<T> inOrder() {
            DynamicArray<T> values = new DynamicArray<>();
            inOrder(root, values);
            return values;
        }

        private void inOrder(Node<T> node, DynamicArray<T> values) {
            if (node == null) return;
            inOrder(node.left, values);
            values.add(node.value);
            inOrder(node.right, values);
        }

        public int size() {
            return size;
        }

        public int height() {
            return height(root);
        }

        private int height(Node<T> node) {
            if (node == null) return 0;
            return 1 + Math.max(height(node.left), height(node.right));
        }

        private static final class Node<T> {
            private T value;
            private Node<T> left;
            private Node<T> right;

            private Node(T value) {
                this.value = value;
            }
        }
    }

    
    public static final class Graph<V> {
        public record Edge<V>(V destination, double weight) {
        }

        public record Path<V>(DynamicArray<V> vertices, double distance) {
            public boolean isReachable() {
                return !vertices.isEmpty();
            }
        }

        private final boolean directed;
        private final DynamicArray<V> vertices = new DynamicArray<>();
        private final HashTable<V, DynamicArray<Edge<V>>> adjacency = new HashTable<>();

        public Graph() {
            this(false);
        }

        public Graph(boolean directed) {
            this.directed = directed;
        }

        public void addVertex(V vertex) {
            if (vertex == null) throw new IllegalArgumentException("Vertex cannot be null");
            if (!adjacency.containsKey(vertex)) {
                adjacency.put(vertex, new DynamicArray<>());
                vertices.add(vertex);
            }
        }

        public void addEdge(V from, V to, double weight) {
            if (Double.isNaN(weight) || Double.isInfinite(weight) || weight < 0) {
                throw new IllegalArgumentException("Edge weight must be finite and non-negative");
            }
            addVertex(from);
            addVertex(to);
            adjacency.get(from).add(new Edge<>(to, weight));
            if (!directed) adjacency.get(to).add(new Edge<>(from, weight));
        }

        public DynamicArray<V> breadthFirstSearch(V start) {
            DynamicArray<V> order = new DynamicArray<>();
            if (!adjacency.containsKey(start)) return order;
            HashTable<V, Boolean> visited = new HashTable<>();
            LinkedQueue<V> pending = new LinkedQueue<>();
            pending.enqueue(start);
            visited.put(start, true);
            while (!pending.isEmpty()) {
                V current = pending.dequeue();
                order.add(current);
                DynamicArray<Edge<V>> edges = adjacency.get(current);
                for (int i = 0; i < edges.size(); i++) {
                    V next = edges.get(i).destination();
                    if (!visited.containsKey(next)) {
                        visited.put(next, true);
                        pending.enqueue(next);
                    }
                }
            }
            return order;
        }

        public DynamicArray<V> depthFirstSearch(V start) {
            DynamicArray<V> order = new DynamicArray<>();
            if (adjacency.containsKey(start)) depthFirstSearch(start, new HashTable<>(), order);
            return order;
        }

        private void depthFirstSearch(V current, HashTable<V, Boolean> visited, DynamicArray<V> order) {
            visited.put(current, true);
            order.add(current);
            DynamicArray<Edge<V>> edges = adjacency.get(current);
            for (int i = 0; i < edges.size(); i++) {
                V next = edges.get(i).destination();
                if (!visited.containsKey(next)) depthFirstSearch(next, visited, order);
            }
        }

        




        public DynamicArray<V> findPathBacktracking(V start, V target) {
            DynamicArray<V> path = new DynamicArray<>();
            if (!adjacency.containsKey(start) || !adjacency.containsKey(target)) return path;
            if (backtrack(start, target, new HashTable<>(), path)) return path;
            return new DynamicArray<>();
        }

        private boolean backtrack(V current, V target, HashTable<V, Boolean> visited,
                                  DynamicArray<V> path) {
            visited.put(current, true);
            path.add(current);
            if (current.equals(target)) return true;
            DynamicArray<Edge<V>> edges = adjacency.get(current);
            for (int i = 0; i < edges.size(); i++) {
                V next = edges.get(i).destination();
                if (!visited.containsKey(next) && backtrack(next, target, visited, path)) return true;
            }
            path.remove(path.size() - 1);
            return false;
        }

        
        public Path<V> shortestPath(V start, V target) {
            if (!adjacency.containsKey(start) || !adjacency.containsKey(target)) {
                return new Path<>(new DynamicArray<>(), Double.POSITIVE_INFINITY);
            }
            HashTable<V, Double> distances = new HashTable<>();
            HashTable<V, V> previous = new HashTable<>();
            HashTable<V, Boolean> visited = new HashTable<>();
            for (int i = 0; i < vertices.size(); i++) distances.put(vertices.get(i), Double.POSITIVE_INFINITY);
            distances.put(start, 0.0);
            for (int step = 0; step < vertices.size(); step++) {
                V current = closestUnvisited(distances, visited);
                if (current == null) break;
                visited.put(current, true);
                if (current.equals(target)) break;
                DynamicArray<Edge<V>> edges = adjacency.get(current);
                for (int i = 0; i < edges.size(); i++) {
                    Edge<V> edge = edges.get(i);
                    double candidate = distances.get(current) + edge.weight();
                    if (candidate < distances.get(edge.destination())) {
                        distances.put(edge.destination(), candidate);
                        previous.put(edge.destination(), current);
                    }
                }
            }
            double distance = distances.get(target);
            if (Double.isInfinite(distance)) return new Path<>(new DynamicArray<>(), distance);
            DynamicArray<V> reversed = new DynamicArray<>();
            for (V current = target; current != null; current = previous.get(current)) reversed.add(current);
            DynamicArray<V> path = new DynamicArray<>();
            for (int i = reversed.size() - 1; i >= 0; i--) path.add(reversed.get(i));
            return new Path<>(path, distance);
        }

        private V closestUnvisited(HashTable<V, Double> distances, HashTable<V, Boolean> visited) {
            V closest = null;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < vertices.size(); i++) {
                V vertex = vertices.get(i);
                Double distance = distances.get(vertex);
                if (!visited.containsKey(vertex) && distance < best) {
                    closest = vertex;
                    best = distance;
                }
            }
            return closest;
        }

        public int vertexCount() {
            return vertices.size();
        }
    }

}