package company.vk.edu.distrib.compute.ternuraa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class RecordCodec {
    private final Charset cs;

    public RecordCodec() {
        this(StandardCharsets.UTF_8);
    }

    public RecordCodec(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("charset must not be null");
        }
        this.cs = charset;
    }

    public void write(DataOutput out, BucketRecord rec) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        if (rec == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        byte[] kb = rec.key().getBytes(cs);
        byte[] vb = rec.value();
        out.writeInt(kb.length);
        out.writeInt(vb.length);
        out.write(kb);
        out.write(vb);
    }

    public BucketRecord read(DataInput in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        int kl = in.readInt();
        int vl = in.readInt();
        if (kl < 0 || vl < 0) {
            throw new IOException("negative length in record");
        }
        byte[] kb = new byte[kl];
        byte[] vb = new byte[vl];
        in.readFully(kb);
        in.readFully(vb);
        return new BucketRecord(new String(kb, cs), vb);
    }
}
