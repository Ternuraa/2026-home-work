package company.vk.edu.distrib.compute.ternuraa.kafka;

import company.vk.edu.distrib.compute.Dao;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Properties;
import java.util.NoSuchElementException;

public class AuditableKVServiceImpl implements AuditableKVService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Dao<byte[]> dao;
    private KafkaProducer<String, String> producer;
    private String bootstrapServers;
    private boolean isAsync = true;

    public AuditableKVServiceImpl(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void setAsync(boolean isAsync) { this.isAsync = isAsync; }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IOException {
        log("GET", key);
        return dao.get(key);
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        log("PUT", key);
        dao.upsert(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        log("DELETE", key);
        dao.delete(key);
    }

    private void log(String method, String id) {
        if (producer == null) return;
        try {
            String json = mapper.writeValueAsString(new AuditEvent(method, id, System.currentTimeMillis()));
            ProducerRecord<String, String> record = new ProducerRecord<>("audit", id, json);
            if (isAsync) producer.send(record);
            else producer.send(record).get();
        } catch (Exception e)
        { e.printStackTrace();
        }
    }

    @Override public void start() {

    }
    @Override public void stop() { if (producer != null) producer.close();
    }
    @Override public void close() throws IOException { dao.close();
    }
}
