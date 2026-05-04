public class Main {
    public static void main(String[] args) throws InterruptedException {
        Cluster cluster = new Cluster(5);
        cluster.start();

        // Запускаем визуализатор состояния
        Visualizer vis = new Visualizer(cluster);
        vis.setDaemon(true);
        vis.start();

        System.out.println("Ожидание первоначальных выборов...");
        Thread.sleep(5000);
        printStatus(cluster);

        System.out.println("\n>>> Отказ лидера (узел 5) <<<");
        cluster.failNode(5);
        Thread.sleep(7000);
        printStatus(cluster);

        System.out.println("\n>>> Восстановление узла 5 <<<");
        cluster.recoverNode(5);
        Thread.sleep(7000);
        printStatus(cluster);

        System.out.println("\n>>> Наблюдение случайных отказов в течение 20 секунд... <<<");
        Thread.sleep(20000);
        printStatus(cluster);

        System.out.println("\n>>> Плановый уход лидера (graceful shutdown) <<<");
        cluster.gracefulShutdown(5);
        Thread.sleep(7000);
        printStatus(cluster);

        System.out.println("\nВсе тесты завершены.");
        cluster.shutdown();
    }

    private static void printStatus(Cluster cluster) {
        System.out.println("Состояние кластера");
        for (Node n : cluster.getAllNodes()) {
            String status = n.isFailed() ? "FAILED" : "OK";
            String roleStr = n.isFailed() ? "--------" : n.getRole().toString();
            System.out.printf("Узел %2d | %-10s | Лидер: %2d | %s%n",
                    n.getId(),
                    roleStr,
                    n.getCurrentLeaderId(),
                    status);
        }
        System.out.println("------------------");
    }
}