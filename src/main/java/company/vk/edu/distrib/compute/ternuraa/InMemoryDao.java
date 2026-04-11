package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.Dao;
<<<<<<< HEAD
import java.util.NoSuchElementException;
=======

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDao implements Dao<byte[]> {
<<<<<<< HEAD
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
=======
    private final ConcurrentMap<String, byte[]> storage;

    public InMemoryDao() {
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        validateKey(key);

        byte[] value = storage.get(key);
        if (value == null) {
            throw new NoSuchElementException("No value for key: " + key);
        }
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        validateKey(key);
        validateValue(value);

        storage.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        validateKey(key);

        storage.remove(key);
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    private void validateKey(String key) {
        if (Objects.isNull(key) || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or blank");
        }
    }

    private static void validateValue(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
    }
}
