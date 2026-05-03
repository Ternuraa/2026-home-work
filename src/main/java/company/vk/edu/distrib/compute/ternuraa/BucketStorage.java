package company.vk.edu.distrib.compute.ternuraa;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BucketStorage {
    private final RecordCodec codec;

    public BucketStorage(RecordCodec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }
        this.codec = codec;
    }

    public Optional<byte[]> find(Path path, String key) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("bucketPath must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            while (dis.available() > 0) {
                BucketRecord r = codec.read(dis);
                if (r.key().equals(key)) {
                    return Optional.of(r.value());
                }
            }
        }
        return Optional.empty();
    }

    public void upsert(Path bucket, Path temp, BucketRecord rec) throws IOException {
        if (bucket == null) {
            throw new IllegalArgumentException("bucketPath must not be null");
        }
        if (temp == null) {
            throw new IllegalArgumentException("tempPath must not be null");
        }
        if (rec == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        List<BucketRecord> existing = readAll(bucket);
        List<BucketRecord> merged = new ArrayList<>();
        boolean found = false;
        for (BucketRecord r : existing) {
            if (r.key().equals(rec.key())) {
                merged.add(rec);
                found = true;
            } else {
                merged.add(r);
            }
        }
        if (!found) {
            merged.add(rec);
        }
        rewrite(bucket, temp, merged);
    }

    public boolean delete(Path bucket, Path temp, String key) throws IOException {
        if (bucket == null) {
            throw new IllegalArgumentException("bucketPath must not be null");
        }
        if (temp == null) {
            throw new IllegalArgumentException("tempPath must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (Files.notExists(bucket)) {
            return false;
        }
        List<BucketRecord> existing = readAll(bucket);
        List<BucketRecord> remaining = new ArrayList<>();
        boolean deleted = false;
        for (BucketRecord r : existing) {
            if (r.key().equals(key)) {
                deleted = true;
            } else {
                remaining.add(r);
            }
        }
        if (!deleted) {
            return false;
        }
        if (remaining.isEmpty()) {
            Files.deleteIfExists(bucket);
            Files.deleteIfExists(temp);
        } else {
            rewrite(bucket, temp, remaining);
        }
        return true;
    }

    private List<BucketRecord> readAll(Path f) throws IOException {
        if (Files.notExists(f)) {
            return List.of();
        }
        List<BucketRecord> list = new ArrayList<>();
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(f)))) {
            while (dis.available() > 0) {
                list.add(codec.read(dis));
            }
        }
        return list;
    }

    private void rewrite(Path target, Path tmp, List<BucketRecord> records) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try {
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                for (BucketRecord r : records) {
                    codec.write(dos, r);
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }
}
