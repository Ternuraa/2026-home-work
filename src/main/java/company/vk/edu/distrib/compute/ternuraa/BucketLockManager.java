package company.vk.edu.distrib.compute.ternuraa;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BucketLockManager {
    private final ConcurrentMap<BucketId, ReentrantReadWriteLock> map = new ConcurrentHashMap<>();

    public Lock readLock(BucketId id) {
        if (id == null) {
            throw new IllegalArgumentException("bucket id cannot be null");
        }
        return map.computeIfAbsent(id, k -> new ReentrantReadWriteLock()).readLock();
    }

    public Lock writeLock(BucketId id) {
        if (id == null) {
            throw new IllegalArgumentException("bucket id cannot be null");
        }
        return map.computeIfAbsent(id, k -> new ReentrantReadWriteLock()).writeLock();
    }
}
