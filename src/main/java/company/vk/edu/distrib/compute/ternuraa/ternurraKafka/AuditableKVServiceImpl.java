package company.vk.edu.distrib.compute.ternuraa.ternurraKafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import company.vk.edu.distrib.compute.Dao;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AuditableKVService} with Kafka audit logging.
 */
public class AuditableKVServiceImpl implements AuditableKVService {
    private static final Logger log = LoggerFactory.getLogger(AuditableKVServiceImpl.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Dao<byte[]> dao;
    private KafkaProducer<String, String> producer;
    private boolean isAsync = true;

    public AuditableKVServiceImpl(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void setBootstrapServers(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IOException {
        sendAuditLog("GET", key);
        return dao.get(key);
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        sendAuditLog("PUT", key);
        dao.upsert(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        sendAuditLog("DELETE", key);
        dao.delete(key);
    }

    private void sendAuditLog(String method, String id) {
        if (producer == null) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(new AuditEvent(method, id, System.currentTimeMillis()));
            ProducerRecord<String, String> record = new ProducerRecord<>("audit", id, json);
            if (isAsync) {
                producer.send(record);
            } else {
                producer.send(record).get();
            }
        } catch (Exception e) {
            log.error("Failed to send audit log for method {} and id {}", method, id, e);
        }
    }

    @Override
    public void start() {
        // No-op init logic
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.close();
        }
    }

    @Override
    public void close() throws IOException {
        dao.close();
    }
}
