package company.vk.edu.distrib.compute.ternuraa;

import java.nio.file.Path;

public final class PathResolver {
    private final Path root;

    public PathResolver(Path rootDir) {
        if (rootDir == null) {
            throw new IllegalArgumentException("rootDir must not be null");
        }
        this.root = rootDir;
    }

    public Path rootDir() {
        return root;
    }

    public Path bucketDirectory(BucketId id) {
        if (id == null) {
            throw new IllegalArgumentException("bucketId must not be null");
        }
        return root.resolve(id.dir1()).resolve(id.dir2());
    }

    public Path bucketPath(BucketId id) {
        if (id == null) {
            throw new IllegalArgumentException("bucketId must not be null");
        }
        return bucketDirectory(id).resolve(id.fileName());
    }

    public Path tempBucketPath(BucketId id) {
        if (id == null) {
            throw new IllegalArgumentException("bucketId must not be null");
        }
        return bucketDirectory(id).resolve(id.fileName() + ".tmp");
    }
}
