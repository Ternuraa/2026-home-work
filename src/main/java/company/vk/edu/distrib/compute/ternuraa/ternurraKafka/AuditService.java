package company.vk.edu.distrib.compute.ternuraa.ternurraKafka;

import java.util.List;

public interface AuditService {
    void start();

    void stop();

    List<AuditEvent> listAuditEntries();
}
