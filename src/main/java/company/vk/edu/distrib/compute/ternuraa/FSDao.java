package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.Dao;
<<<<<<< HEAD
=======
import company.vk.edu.distrib.compute.ternuraa.internal.BucketId;
import company.vk.edu.distrib.compute.ternuraa.internal.BucketLockManager;
import company.vk.edu.distrib.compute.ternuraa.internal.BucketRecord;
import company.vk.edu.distrib.compute.ternuraa.internal.BucketStorage;
import company.vk.edu.distrib.compute.ternuraa.internal.HashResolver;
import company.vk.edu.distrib.compute.ternuraa.internal.PathResolver;
import company.vk.edu.distrib.compute.ternuraa.internal.RecordCodec;

>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

public class FSDao implements Dao<byte[]> {
<<<<<<< HEAD
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
=======

    private static final Path DEFAULT_ROOT_DIR = Path.of(".data");

    private final HashResolver hashResolver;
    private final PathResolver pathResolver;
    private final BucketLockManager lockManager;
    private final BucketStorage bucketStorage;
    private final AtomicBoolean closed;

    public FSDao() {
        this(DEFAULT_ROOT_DIR);
    }

    public FSDao(Path rootDir) {
        validateRootDir(rootDir);

        this.hashResolver = new HashResolver();
        this.pathResolver = new PathResolver(rootDir);
        this.lockManager = new BucketLockManager();
        this.bucketStorage = new BucketStorage(new RecordCodec());
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        ensureOpen();
        validateKey(key);

        BucketId bucketId = hashResolver.resolve(key);
        Lock lock = lockManager.readLock(bucketId);
        lock.lock();
        try {
            Optional<byte[]> value = bucketStorage.find(pathResolver.bucketPath(bucketId), key);
            return value.orElseThrow(() -> new NoSuchElementException("Key not found: " + key));
        } finally {
            lock.unlock();
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
        }
    }

    @Override
<<<<<<< HEAD
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
=======
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        ensureOpen();
        validateKey(key);
        validateValue(value);

        BucketId bucketId = hashResolver.resolve(key);
        Lock lock = lockManager.writeLock(bucketId);
        lock.lock();
        try {
            bucketStorage.upsert(
                    pathResolver.bucketPath(bucketId),
                    pathResolver.tempBucketPath(bucketId),
                    new BucketRecord(key, value)
            );
        } finally {
            lock.unlock();
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
        }
    }

    @Override
<<<<<<< HEAD
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
=======
    public void delete(String key) throws IllegalArgumentException, IOException {
        ensureOpen();
        validateKey(key);

        BucketId bucketId = hashResolver.resolve(key);
        Lock lock = lockManager.writeLock(bucketId);
        lock.lock();
        try {
            bucketStorage.delete(
                    pathResolver.bucketPath(bucketId),
                    pathResolver.tempBucketPath(bucketId),
                    key
            );
        } finally {
            lock.unlock();
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
        }
    }

    @Override
    public void close() {
        closed.set(true);
    }
<<<<<<< HEAD
=======

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("DAO is already closed");
        }
    }

    private static void validateRootDir(Path rootDir) {
        if (rootDir == null) {
            throw new IllegalArgumentException("rootDir must not be null");
        }
    }

    private static void validateKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
    }

    private static void validateValue(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
>>>>>>> 80e327893ed0dbb61501bd89cd1e5c4ac0a974f6
}
