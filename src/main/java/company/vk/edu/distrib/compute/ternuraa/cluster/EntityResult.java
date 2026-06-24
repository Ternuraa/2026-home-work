package company.vk.edu.distrib.compute.ternuraa.cluster;

public record EntityResult(int statusCode, byte[] body) {
    public EntityResult {
        body = body == null ? new byte[0] : body;
    }

    public static EntityResult of(int statusCode) {
        return new EntityResult(statusCode, new byte[0]);
    }
}
