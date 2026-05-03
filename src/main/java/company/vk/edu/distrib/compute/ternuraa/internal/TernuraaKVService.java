package company.vk.edu.distrib.compute.ternuraa.internal;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TernuraaKVService implements KVService {
    private static final Logger LOGGER = Logger.getLogger(TernuraaKVService.class.getName());
    private final Dao<byte[]> dao;
    private boolean stopped;
    private final Lock lock = new ReentrantLock();

    public TernuraaKVService(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void start() {
        // Логика старта
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            if (stopped) {
                return;
            }
            stopped = true;
        } finally {
            lock.unlock();
        }

        if (dao != null) {
            try {
                dao.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing DAO during shutdown", e);
            }
        }
    }
}
