
public class Visualizer extends Thread {
    private final Cluster cluster;

    public Visualizer(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void run() {
        while (true) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            System.out.println("Cluster Status");
            System.out.printf("%-4s %-12s %-8s %-10s%n", "ID", "Role", "Leader", "Status");
            for (Node n : cluster.getAllNodes()) {
                String status = n.isFailed() ? "FAILED" : "OK";
                String color = n.isFailed() ? "\u001B[31m" : (n.getRole() == Node.Role.LEADER ? "\u001B[32m" : "\u001B[0m");
                System.out.printf("%s%-4d %-12s %-8d %-10s\u001B[0m%n",
                        color, n.getId(), n.getRole(), n.getCurrentLeaderId(), status);
            }
            System.out.println("--------------------");
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
    }
}
