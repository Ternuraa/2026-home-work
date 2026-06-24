package company.vk.edu.distrib.compute;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains utility methods for unit tests.
 */
abstract class TestBase {
    private static final int VALUE_LENGTH = 1024;

    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    static int randomPort() {
        final var port = ThreadLocalRandom.current().nextInt(10000, 60000);
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 100_000; i++) {
                if (isTcpPortAvailable(port)) {
                    return port;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted while looking for available port");
            }
        }
        throw new IllegalStateException("Can't find available port");
    }

    static boolean isTcpPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    static String randomKey() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    static byte[] randomValue() {
        final byte[] result = new byte[VALUE_LENGTH];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }

    static String endpoint(int port) {
        return "http://localhost:" + port;
    }

    static String url(String endpoint, String id) {
        return entityUrl(endpoint, id);
    }

    static String url(String endpoint, String id, Integer ack) {
        if (ack == null) {
            return entityUrl(endpoint, id);
        }
        return entityUrl(endpoint, id) + "&ack=" + ack;
    }

    private static String entityUrl(String endpoint, String id) {
        int queryStart = endpoint.indexOf('?');
        String base = queryStart >= 0 ? endpoint.substring(0, queryStart) : endpoint;
        String query = queryStart >= 0 ? endpoint.substring(queryStart + 1) : null;

        StringBuilder result = new StringBuilder(base)
                .append("/v0/entity?id=")
                .append(id);
        if (query != null && !query.isEmpty()) {
            result.append('&').append(query);
        }
        return result.toString();
    }

    protected abstract HttpClient getHttpClient();

    protected HttpResponse<byte[]> get(String endpoint, String key)
            throws IOException, URISyntaxException, InterruptedException {
        return get(endpoint, key, null);
    }

    protected HttpResponse<byte[]> get(String endpoint, String key, Integer ack)
            throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(url(endpoint, key, ack)))
                .timeout(Duration.ofSeconds(2))
                .build();
        return getHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    protected HttpResponse<Void> delete(String endpoint, String key)
            throws IOException, URISyntaxException, InterruptedException {
        return delete(endpoint, key, null);
    }

    protected HttpResponse<Void> delete(String endpoint, String key, Integer ack)
            throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(new URI(url(endpoint, key, ack)))
                .timeout(TIMEOUT)
                .build();
        return getHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
    }

    protected HttpResponse<Void> upsert(String endpoint, String key, byte[] data)
            throws IOException, URISyntaxException, InterruptedException {
        return upsert(endpoint, key, data, null);
    }

    protected HttpResponse<Void> upsert(String endpoint, String key, byte[] data, Integer ack)
            throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .uri(new URI(url(endpoint, key, ack)))
                .timeout(TIMEOUT)
                .build();
        return getHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
    }
}
