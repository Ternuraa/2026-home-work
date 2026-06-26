package company.vk.edu.distrib.compute.ternuraa.cluster;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class ClusterNode implements KVService {
    private static final Logger log = LoggerFactory.getLogger(ClusterNode.class);
    private static final int GRPC_PORT_OFFSET = 10_000;
    private static final long GRPC_SHUTDOWN_TIMEOUT_SECONDS = 1L;
    private static final String GET_METHOD = "GET";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";

    private final int httpPort;
    private final int grpcPort;
    private final String nodeId;
    private final Dao<byte[]> dao;
    private final List<String> nodeIds;
    private final Map<String, Integer> nodeGrpcPorts;
    private final GrpcInternalClient grpcClient;

    private HttpServer httpServer;
    private Server grpcServer;
    private boolean started;

    public ClusterNode(
            int httpPort,
            Dao<byte[]> dao,
            List<String> nodeIds,
            Map<String, Integer> nodeGrpcPorts,
            GrpcInternalClient grpcClient
    ) {
        this.httpPort = httpPort;
        this.grpcPort = httpPort + GRPC_PORT_OFFSET;
        this.nodeId = String.valueOf(httpPort);
        this.dao = dao;
        this.nodeIds = List.copyOf(nodeIds);
        this.nodeGrpcPorts = Map.copyOf(nodeGrpcPorts);
        this.grpcClient = grpcClient;
    }

    public static int grpcPortForHttpPort(int httpPort) {
        return httpPort + GRPC_PORT_OFFSET;
    }

    public String getEndpoint() {
        return EndpointUtils.formatEndpoint(httpPort, grpcPort);
    }

    public int getHttpPort() {
        return httpPort;
    }

    @Override
    public void start() {
        if (started) {
            throw new IllegalStateException("Node already started: " + getEndpoint());
        }

        try {
            httpServer = HttpServer.create();
            createHttpContexts(httpServer);
            httpServer.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), httpPort), 0);
            httpServer.start();

            grpcServer = ServerBuilder.forPort(grpcPort)
                    .addService(new GrpcInternalService(dao))
                    .build()
                    .start();

            started = true;
            if (log.isDebugEnabled()) {
                log.debug("Cluster node started at {} (gRPC:{})", getEndpoint(), grpcPort);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start cluster node on port " + httpPort, e);
        }
    }

    @Override
    public void stop() {
        if (!started) {
            throw new IllegalStateException("Node is not started: " + getEndpoint());
        }

        httpServer.stop(0);
        shutdownGrpcServer();
        started = false;
    }

    private void shutdownGrpcServer() {
        if (grpcServer == null) {
            return;
        }
        grpcServer.shutdown();
        try {
            if (!grpcServer.awaitTermination(GRPC_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                grpcServer.shutdownNow();
            }
        } catch (InterruptedException e) {
            grpcServer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void createHttpContexts(HttpServer server) {
        server.createContext("/v0/status", wrapHandler(this::handleStatus));
        server.createContext("/v0/entity", wrapHandler(this::handleEntity));
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!Objects.equals(exchange.getRequestMethod(), GET_METHOD)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.sendResponseHeaders(200, -1);
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange);
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String owner = RendezvousSharding.resolveOwner(id, nodeIds);
        EntityResult result;
        if (nodeId.equals(owner)) {
            result = handleLocal(exchange.getRequestMethod(), id, exchange);
        } else {
            int targetGrpcPort = nodeGrpcPorts.get(owner);
            result = handleRemote(exchange.getRequestMethod(), id, exchange, targetGrpcPort);
        }
        sendResult(exchange, result);
    }

    private EntityResult handleLocal(String method, String id, HttpExchange exchange) throws IOException {
        return switch (method) {
            case GET_METHOD -> LocalEntityUtils.get(dao, id);
            case PUT_METHOD -> LocalEntityUtils.put(dao, id, exchange.getRequestBody().readAllBytes());
            case DELETE_METHOD -> LocalEntityUtils.delete(dao, id);
            default -> EntityResult.of(405);
        };
    }

    private EntityResult handleRemote(String method, String id, HttpExchange exchange, int targetGrpcPort)
            throws IOException {
        return switch (method) {
            case GET_METHOD -> grpcClient.executeSafely(() -> grpcClient.getEntity(targetGrpcPort, id));
            case PUT_METHOD -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                yield grpcClient.executeSafely(() -> grpcClient.putEntity(targetGrpcPort, id, body));
            }
            case DELETE_METHOD -> grpcClient.executeSafely(() -> grpcClient.deleteEntity(targetGrpcPort, id));
            default -> EntityResult.of(405);
        };
    }

    private static void sendResult(HttpExchange exchange, EntityResult result) throws IOException {
        byte[] body = result.body();
        if (body.length == 0) {
            exchange.sendResponseHeaders(result.statusCode(), -1);
        } else {
            exchange.sendResponseHeaders(result.statusCode(), body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static Map<String, String> parseQueryParams(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        ConcurrentMap<String, String> params = new ConcurrentHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private HttpHandler wrapHandler(HttpHandler handler) {
        return exchange -> {
            try (exchange) {
                try {
                    handler.handle(exchange);
                } catch (IllegalArgumentException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (Exception e) {
                    sendError(exchange, 503, e.getMessage());
                }
            }
        };
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String response = message == null ? "" : message;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
