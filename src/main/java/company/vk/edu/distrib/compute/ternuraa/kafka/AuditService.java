package company.vk.edu.distrib.compute.ternuraa.kafka;

import java.util.List;

public interface AuditService {
    void start();

    void stop();

    List<AuditEvent> listAuditEntries();
}
