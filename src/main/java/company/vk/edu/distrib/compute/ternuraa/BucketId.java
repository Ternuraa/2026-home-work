package company.vk.edu.distrib.compute.ternuraa;

import java.util.Locale;

public final class BucketId {
    private final String full;
    private final String d1;
    private final String d2;

    public BucketId(String hexHash) {
        if (hexHash == null || hexHash.length() < 4) {
            throw new IllegalArgumentException("hex hash must have at least 4 chars");
        }
        for (char c : hexHash.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                throw new IllegalArgumentException("invalid hex: " + c);
            }
        }
        this.full = hexHash.toLowerCase(Locale.ROOT);
        this.d1 = this.full.substring(0, 2);
        this.d2 = this.full.substring(2, 4);
    }

    public String hex() {
        return full;
    }

    public String dir1() {
        return d1;
    }

    public String dir2() {
        return d2;
    }

    public String fileName() {
        return full;
    }

    @Override
    public String toString() {
        return full;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BucketId)) {
            return false;
        }
        BucketId other = (BucketId) o;
        return full.equals(other.full);
    }

    @Override
    public int hashCode() {
        return full.hashCode();
    }
}
