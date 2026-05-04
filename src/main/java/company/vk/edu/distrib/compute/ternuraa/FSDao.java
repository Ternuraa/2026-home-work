package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

public class FSDao implements Dao<byte[]> {

    private static final Path DATA_DIR = Path.of(".data");
    private final HashResolver hasher = new HashResolver();
    private final PathResolver resolver = new PathResolver(DATA_DIR);
    private final BucketLockManager locks = new BucketLockManager();
    private final BucketStorage storage = new BucketStorage(new RecordCodec());
    private final AtomicBoolean closed = new AtomicBoolean();

    // Конструктор по умолчанию генерируется компилятором, явно не пишем

    @Override
    public byte[] get(String key) throws NoSuchElementException, IOException {
        if (closed.get()) {
            throw new IllegalStateException("DAO is already closed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        BucketId id = hasher.resolve(key);
        Lock l = locks.readLock(id);
        l.lock();
        try {
            Optional<byte[]> val = storage.find(resolver.bucketPath(id), key);
            if (val.isEmpty()) {
                throw new NoSuchElementException("Key not found: " + key);
            }
            return val.get();
        } finally {
            l.unlock();

        }
    }

    @Override

    public void upsert(String key, byte[] value) throws IOException {
        if (closed.get()) {
            throw new IllegalStateException("DAO is already closed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        BucketId id = hasher.resolve(key);
        Lock l = locks.writeLock(id);
        l.lock();
        try {
            storage.upsert(resolver.bucketPath(id), resolver.tempBucketPath(id), new BucketRecord(key, value));
        } finally {
            l.unlock();

        }
    }

    @Override

    public void delete(String key) throws IOException {
        if (closed.get()) {
            throw new IllegalStateException("DAO is already closed");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        BucketId id = hasher.resolve(key);
        Lock l = locks.writeLock(id);
        l.lock();
        try {
            storage.delete(resolver.bucketPath(id), resolver.tempBucketPath(id), key);
        } finally {
            l.unlock();

        }
    }

    @Override
    public void close() {
        closed.set(true);
    }

}
