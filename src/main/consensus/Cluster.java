import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Cluster {
    private final Map<Integer, Node> nodes = new ConcurrentHashMap<>();
    private final int nodeCount;
    private final List<Thread> threads = new ArrayList<>();

    public Cluster(int nodeCount) {
        this.nodeCount = nodeCount;
        for (int i = 1; i <= nodeCount; i++) {
            Node node = new Node(i, this);
            nodes.put(i, node);
        }
    }

    public void start() {
        System.out.println("Кластер из " + nodeCount + " узлов запущен.");
        for (Node node : nodes.values()) {
            Thread t = new Thread(node, "Node-" + node.getId());
            threads.add(t);
            t.start();
        }
    }

    public void shutdown() {
        for (Node node : nodes.values()) {
            node.stop();
        }
        for (Thread t : threads) {
            try {
                t.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Кластер остановлен.");
    }

    public Node getNode(int id) { return nodes.get(id); }
    public Collection<Node> getAllNodes() { return nodes.values(); }

    public void failNode(int id) {
        Node n = nodes.get(id);
        if (n != null) n.fail();
    }

    public void recoverNode(int id) {
        Node n = nodes.get(id);
        if (n != null) n.recover();
    }

    public void gracefulShutdown(int id) {
        Node n = nodes.get(id);
        if (n != null) n.gracefulShutdown();
    }
}
