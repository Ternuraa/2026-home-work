package company.vk.edu.distrib.compute.ternuraa;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashResolver {
    private static final String DEFAULT_ALGO = "SHA-256";
    private final String algorithm;
    private final Charset encoding;

    public HashResolver() {
        this(DEFAULT_ALGO, StandardCharsets.UTF_8);
    }

    public HashResolver(String algorithm, Charset charset) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm must not be null or blank");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset must not be null");
        }
        try {
            MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unsupported algorithm: " + algorithm, e);
        }
        this.algorithm = algorithm;
        this.encoding = charset;
    }

    public BucketId resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(key.getBytes(encoding));
            return new BucketId(convertToHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("algorithm not available: " + algorithm, e);
        }
    }

    private static String convertToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
