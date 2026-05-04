import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Node implements Runnable {
    public enum Role { LEADER, FOLLOWER }

    private final int id;
    private final Cluster cluster;
    private final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();

    private volatile boolean active = true;
    private volatile boolean failed = false;
    private volatile int currentLeaderId = -1;
    private volatile Role role = Role.FOLLOWER;

    // Для пингов
    private volatile long lastPingSent = 0;
    private volatile long lastPingResponse = 0;

    // Для выборов
    private volatile boolean electionInProgress = false;
    private final Set<Integer> answersReceived = ConcurrentHashMap.newKeySet();
    private volatile long electionStartTime = 0;

    // Таймауты по умолчанию
    static final long PING_INTERVAL = 1000;
    static final long DEFAULT_PING_TIMEOUT = 3000;
    static final long ELECTION_TIMEOUT = 2000;

    // Изменяемые параметры для экспериментов
    public static volatile long currentPingTimeout = DEFAULT_PING_TIMEOUT;

    // Случайные отказы
    private volatile long lastFailCheck = System.currentTimeMillis();
    private static final long FAIL_CHECK_INTERVAL = 2000;
    static final double DEFAULT_FAIL_PROBABILITY = 0.1;
    public static volatile double currentFailProbability = DEFAULT_FAIL_PROBABILITY;

    // Поток, в котором выполняется узел (для остановки)
    private volatile Thread myThread;

    public Node(int id, Cluster cluster) {
        this.id = id;
        this.cluster = cluster;
    }

    public int getId() { return id; }
    public int getCurrentLeaderId() { return currentLeaderId; }
    public Role getRole() { return role; }
    public boolean isFailed() { return failed; }

    private void log(String action) {
        System.out.printf("[%tT] Node %d: %s (leader=%d, role=%s)%n",
                System.currentTimeMillis(), id, action, currentLeaderId, role);
    }

    public void fail() {
        failed = true;
        if (role == Role.LEADER) {
            role = Role.FOLLOWER;
            currentLeaderId = -1;
        }
        log("Принудительный отказ");
    }

    public void recover() {
        failed = false;
        log("Принудительное восстановление");
        startElection();
    }

    public void stop() {
        active = false;
        if (myThread != null) {
            myThread.interrupt();
        }
    }

    @Override
    public void run() {
        myThread = Thread.currentThread();

        if (currentLeaderId == -1) {
            startElection();
        }

        while (active) {
            if (failed) {
                sleepUninterruptibly(200);
                continue;
            }
            checkRandomFailure();

            try {
                Message msg = inbox.poll(200, TimeUnit.MILLISECONDS);
                if (msg != null) handleMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            sendPingIfNeeded();
            checkLeaderHealth();
            checkElectionTimeout();
        }
    }

    private void checkRandomFailure() {
        long now = System.currentTimeMillis();
        if (now - lastFailCheck >= FAIL_CHECK_INTERVAL) {
            lastFailCheck = now;
            if (ThreadLocalRandom.current().nextDouble() < currentFailProbability) {
                failed = !failed;
                if (failed && role == Role.LEADER) {
                    role = Role.FOLLOWER;
                    currentLeaderId = -1;
                }
                log(failed ? "ОТКАЗАЛ (случайно)" : "ВОССТАНОВИЛСЯ (случайно)");
                if (!failed) startElection();
            }
        }
    }

    private void send(int receiverId, MessageType type, int leaderId) {
        Message msg = new Message(id, receiverId, type, leaderId);
        if (receiverId == -1) {
            for (Node node : cluster.getAllNodes()) {
                if (node.getId() != this.id) node.inbox.offer(msg);
            }
        } else {
            Node target = cluster.getNode(receiverId);
            if (target != null) target.inbox.offer(msg);
        }
    }

    private void handleMessage(Message msg) {
        if (failed) return;
        switch (msg.getType()) {
            case PING:
                if (role == Role.LEADER) {
                    send(msg.getSenderId(), MessageType.ANSWER, currentLeaderId);
                }
                break;
            case ANSWER:
                if (msg.getSenderId() == currentLeaderId) {
                    lastPingResponse = System.currentTimeMillis();
                }
                if (electionInProgress && msg.getSenderId() > id) {
                    answersReceived.add(msg.getSenderId());
                }
                break;
            case ELECT:
                if (msg.getSenderId() < id) {
                    send(msg.getSenderId(), MessageType.ANSWER, -1);
                    if (currentLeaderId != id) startElection();
                }
                break;
            case VICTORY:
                int newLeader = msg.getLeaderId();
                if (newLeader > currentLeaderId || currentLeaderId == -1) {
                    currentLeaderId = newLeader;
                    role = (newLeader == id) ? Role.LEADER : Role.FOLLOWER;
                    electionInProgress = false;
                    answersReceived.clear();
                    lastPingResponse = System.currentTimeMillis();
                    lastPingSent = System.currentTimeMillis();
                    log("Принят лидер: " + newLeader);
                }
                break;
            case TRANSFER_LEADERSHIP:
                if (msg.getSenderId() == currentLeaderId) {
                    log("Лидер уходит добровольно, начинаю выборы");
                    currentLeaderId = -1;
                    role = Role.FOLLOWER;
                    startElection();
                }
                break;
        }
    }

    private void sendPingIfNeeded() {
        if (role != Role.LEADER && currentLeaderId != -1) {
            long now = System.currentTimeMillis();
            if (now - lastPingSent >= PING_INTERVAL) {
                send(currentLeaderId, MessageType.PING, -1);
                lastPingSent = now;
            }
        }
    }

    private void checkLeaderHealth() {
        if (role != Role.LEADER && currentLeaderId != -1) {
            long now = System.currentTimeMillis();
            if (lastPingResponse == 0 || now - lastPingResponse > currentPingTimeout) {
                log("Лидер " + currentLeaderId + " не отвечает (таймаут). Начинаю выборы.");
                currentLeaderId = -1;
                lastPingResponse = 0;
                startElection();
            }
        }
    }

    private synchronized void startElection() {
        if (electionInProgress || failed) return;
        electionInProgress = true;
        answersReceived.clear();
        electionStartTime = System.currentTimeMillis();
        log("Начинаю выборы, рассылаю ELECT старшим");

        boolean hasHigher = false;
        for (Node node : cluster.getAllNodes()) {
            if (node.getId() > this.id && !node.isFailed()) {
                send(node.getId(), MessageType.ELECT, -1);
                hasHigher = true;
            }
        }
        if (!hasHigher) declareVictory();
    }

    private synchronized void checkElectionTimeout() {
        if (!electionInProgress) return;
        if (System.currentTimeMillis() - electionStartTime > ELECTION_TIMEOUT) {
            boolean higherAnswer = answersReceived.stream().anyMatch(a -> a > id);
            if (!higherAnswer) declareVictory();
            else {
                log("Получен ответ от старшего, жду VICTORY");
                electionInProgress = false;
                answersReceived.clear();
            }
        }
    }

    private void declareVictory() {
        electionInProgress = false;
        answersReceived.clear();
        currentLeaderId = id;
        role = Role.LEADER;
        lastPingResponse = System.currentTimeMillis();
        log("Я новый лидер! Рассылаю VICTORY");
        send(-1, MessageType.VICTORY, id);
    }

    private void sleepUninterruptibly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void gracefulShutdown() {
        if (role == Role.LEADER) {
            log("Плановое отключение, уведомляю кластер");
            send(-1, MessageType.TRANSFER_LEADERSHIP, -1);
            failed = true;
            active = false;
        }
    }
}