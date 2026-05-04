import java.util.*;

public class TestRunner {
    private static final int NODE_COUNT = 5;
    private static final int EXPERIMENT_RUNS = 5; // повторов для каждого таймаута
    private static final long[] TIMEOUTS = {1000, 3000, 5000};

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Время перевыборов");
        System.out.println("Узлов: " + NODE_COUNT + ", повторов для каждого таймаута: " + EXPERIMENT_RUNS);
        System.out.println("Случайные отказы отключены, тестируется только отказ лидера.\n");

        // Отключаем случайные отказы
        Node.currentFailProbability = 0.0;

        for (long timeout : TIMEOUTS) {
            Node.currentPingTimeout = timeout;

            List<Long> recoveryTimes = new ArrayList<>();

            for (int run = 0; run < EXPERIMENT_RUNS; run++) {
                Cluster cluster = new Cluster(NODE_COUNT);
                cluster.start();

                Thread.sleep(3000);

                Node leader = findLeader(cluster);
                if (leader == null) {
                    System.err.println("Прогон " + (run+1) + " для " + timeout + "мс: лидер не найден, пропускаем.");
                    cluster.shutdown();
                    continue;
                }
                int oldLeaderId = leader.getId();

                // Засекаем время отказа
                long failTime = System.currentTimeMillis();
                cluster.failNode(oldLeaderId);

                // Ожидаем, когда все живые узлы зафиксируют нового лидера
                long deadline = failTime + 15000;
                boolean success = false;
                long recoveryTime = -1;

                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                    if (consensusReached(cluster, oldLeaderId)) {
                        recoveryTime = System.currentTimeMillis() - failTime;
                        success = true;
                        break;
                    }
                }

                if (success) {
                    recoveryTimes.add(recoveryTime);
                } else {
                    System.err.println("Прогон " + (run+1) + " для " + timeout + "мс: консенсус не достигнут вовремя.");
                }

                cluster.shutdown();
                Thread.sleep(500); // даём потокам завершиться
            }

            if (recoveryTimes.isEmpty()) {
                System.out.printf("| %-17d | %-18s | %-12s |%n", timeout, "нет данных", "—");
            } else {
                double avg = recoveryTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                System.out.printf("| %-17d | %-18.0f | %-12s |%n", timeout, avg, "нет");
            }
        }

        System.out.println("\nЭксперимент завершён. Скопируйте таблицу в отчёт.");
    }

    // Проверяет, что все живые узлы видят одного лидера
    private static boolean consensusReached(Cluster cluster, int oldLeaderId) {
        Integer commonLeader = null;
        boolean allOkay = true;
        int aliveCount = 0;

        for (Node n : cluster.getAllNodes()) {
            if (n.isFailed()) continue;
            aliveCount++;
            if (n.getRole() == Node.Role.LEADER) {
                int id = n.getId();
                if (commonLeader == null) commonLeader = id;
                else if (commonLeader != id) allOkay = false;
            } else {
                int seen = n.getCurrentLeaderId();
                if (seen == -1 || seen == oldLeaderId) allOkay = false;
                if (commonLeader == null) commonLeader = seen;
                else if (commonLeader != seen) allOkay = false;
            }
        }

        return allOkay && commonLeader != null && commonLeader != oldLeaderId && aliveCount > 1;
    }

    private static Node findLeader(Cluster cluster) {
        return cluster.getAllNodes().stream()
                .filter(n -> n.getRole() == Node.Role.LEADER)
                .findFirst().orElse(null);
    }
}