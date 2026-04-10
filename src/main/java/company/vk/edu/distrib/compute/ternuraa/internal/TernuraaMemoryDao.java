package company.vk.edu.distrib.compute.ternuraa.internal;

import company.vk.edu.distrib.compute.Dao;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TernuraaMemoryDao implements Dao<byte[]> {
    private final Map<String, byte[]> repository = new ConcurrentHashMap<>();

    @Override
    public byte[] get(String identifier) throws IOException {
        validateIdentifier(identifier);
        byte[] data = repository.get(identifier);
        if (data == null) {
            throw new NoSuchElementException("Entity not found for id: " + identifier);
        }
        return data.clone();
    }

    @Override
    public void upsert(String identifier, byte[] data) throws IOException {
        validateIdentifier(identifier);
        Objects.requireNonNull(data, "Payload data cannot be null");
        repository.put(identifier, data.clone());
    }

    @Override
    public void delete(String identifier) throws IOException {
        validateIdentifier(identifier);
        repository.remove(identifier);
    }

    @Override
    public void close() throws IOException {
        repository.clear();
    }

    private void validateIdentifier(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Identifier must be a non-empty string");
        }
    }
}
