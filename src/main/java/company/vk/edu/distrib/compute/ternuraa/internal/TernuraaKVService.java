package company.vk.edu.distrib.compute.ternuraa.internal;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TernuraaKVService implements KVService {
    private static final Logger LOGGER = Logger.getLogger(TernuraaKVService.class.getName());
    private final Dao<byte[]> dao;
    private boolean stopped = false;

    public TernuraaKVService(int port, Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void start() {
        // Логика старта
    }


    @Override
    public void stop() {
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
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
