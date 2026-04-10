package company.vk.edu.distrib.compute.ternuraa.internal;

import company.vk.edu.distrib.compute.Dao;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;

public class TernuraaDao implements Dao<byte[]> {
    private final PathResolver pathResolver;
    private final HashResolver hashResolver;
    private final BucketStorage storage;
    private final BucketLockManager lockManager;

    public TernuraaDao(Path rootDir) {
        this.pathResolver = new PathResolver(rootDir);
        this.hashResolver = new HashResolver();
        this.storage = new BucketStorage(new RecordCodec());
        this.lockManager = new BucketLockManager();
    }

    @Override
    public byte[] get(String identifier) throws IOException {
        BucketId id = hashResolver.resolve(identifier);
        Lock lock = lockManager.readLock(id);
        lock.lock();
        try {
            return storage.find(pathResolver.bucketPath(id), identifier)
                    .orElseThrow(() -> new NoSuchElementException("Key not found: " + identifier));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String identifier, byte[] data) throws IOException {
        BucketId id = hashResolver.resolve(identifier);
        Lock lock = lockManager.writeLock(id);
        lock.lock();
        try {
            storage.upsert(pathResolver.bucketPath(id),
                    pathResolver.tempBucketPath(id),
                    new BucketRecord(identifier, data));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String identifier) throws IOException {
        BucketId id = hashResolver.resolve(identifier);
        Lock lock = lockManager.writeLock(id);
        lock.lock();
        try {
            storage.delete(pathResolver.bucketPath(id),
                    pathResolver.tempBucketPath(id),
                    identifier);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // Очистка ресурсов, если требуется
    }
}
