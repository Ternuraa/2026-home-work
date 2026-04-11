package company.vk.edu.distrib.compute.ternuraa;

import java.util.Arrays;

public final class BucketRecord {
    private final String recordKey;
    private final byte[] recordValue;

    public BucketRecord(String key, byte[] value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        this.recordKey = key;
        this.recordValue = value.clone();
    }

    public String key() {
        return recordKey;
    }

    public byte[] value() {
        return recordValue.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BucketRecord)) {
            return false;
        }
        BucketRecord that = (BucketRecord) o;
        return recordKey.equals(that.recordKey) && Arrays.equals(recordValue, that.recordValue);
    }

    @Override
    public int hashCode() {
        return 31 * recordKey.hashCode() + Arrays.hashCode(recordValue);
    }

    @Override
    public String toString() {
        return "BucketRecord{key=" + recordKey + ", valueLength=" + recordValue.length + "}";
    }
}
