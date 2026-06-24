package company.vk.edu.distrib.compute.ternuraa.cluster;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EndpointUtils {
    private static final String GRPC_PORT_PARAM = "grpcPort";

    private EndpointUtils() {
    }

    public static String formatEndpoint(int httpPort, int grpcPort) {
        return "http://127.0.0.1:" + httpPort + "?" + GRPC_PORT_PARAM + "=" + grpcPort;
    }

    public static int grpcPortFromEndpoint(String endpoint) {
        return Integer.parseInt(queryParams(endpoint).get(GRPC_PORT_PARAM));
    }

    public static int httpPortFromEndpoint(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            return uri.getPort();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint: " + endpoint, e);
        }
    }

    public static String nodeIdFromEndpoint(String endpoint) {
        return String.valueOf(httpPortFromEndpoint(endpoint));
    }

    private static ConcurrentMap<String, String> queryParams(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            String rawQuery = uri.getRawQuery();
            ConcurrentMap<String, String> params = new ConcurrentHashMap<>();
            if (rawQuery == null || rawQuery.isEmpty()) {
                return params;
            }
            for (String pair : rawQuery.split("&")) {
                String[] parts = pair.split("=", 2);
                params.put(parts[0], parts.length > 1 ? parts[1] : "");
            }
            return params;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint: " + endpoint, e);
        }
    }
}
