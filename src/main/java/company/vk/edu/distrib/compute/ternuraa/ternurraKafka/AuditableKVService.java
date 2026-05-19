package company.vk.edu.distrib.compute.ternuraa.ternurraKafka;

import company.vk.edu.distrib.compute.KVService;

import java.io.IOException;
import java.util.NoSuchElementException;

public interface AuditableKVService extends KVService {

    void setBootstrapServers(String bootstrapServers);

    void setAsync(boolean enabled);

    byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException;

    void upsert(String key, byte[] value) throws IllegalArgumentException, IOException;

    void delete(String key) throws IllegalArgumentException, IOException;

    void close() throws IOException // Важно для интерфейса DAO
    ;
}
