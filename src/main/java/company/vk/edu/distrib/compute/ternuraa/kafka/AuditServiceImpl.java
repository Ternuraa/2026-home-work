package company.vk.edu.distrib.compute.ternuraa.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Audit Service.
 */
public class AuditServiceImpl implements AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final String bootstrapServers;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<AuditEvent> auditLog = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread consumerThread;

    public AuditServiceImpl(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public void start() {
        start("default-group");
    }

    /**
     * Starts the audit consumer with a specific group id.
     */
    public void start(String consumerGroupId) {
        if (running.getAndSet(true)) {
            return;
        }

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumerThread = new Thread(() -> {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList("audit"));
                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        auditLog.add(mapper.readValue(record.value(), AuditEvent.class));
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Exception in audit consumer thread", e);
                }
            }
        });
        consumerThread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    @Override
    public List<AuditEvent> listAuditEntries() {
        return new ArrayList<>(auditLog);
    }
}
