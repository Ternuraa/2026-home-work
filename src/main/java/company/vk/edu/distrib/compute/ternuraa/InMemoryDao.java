package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.Dao;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDao implements Dao<byte[]> {
    private final ConcurrentMap<String, byte[]> map = new ConcurrentHashMap<>();

    @Override
    public byte[] get(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("invalid key");
        }
        byte[] val = map.get(key);
        if (val == null) {
            throw new NoSuchElementException("no entry for key: " + key);
        }
        return val.clone();
    }

    @Override
    public void upsert(String key, byte[] value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("invalid key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        map.put(key, value.clone());
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("invalid key");
        }
        map.remove(key);
    }

    @Override
    public void close() {
        // nothing
    }
}
