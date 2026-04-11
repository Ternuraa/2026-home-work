package company.vk.edu.distrib.compute.ternuraa;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class KVServiceImpl implements KVService {
    private static final Logger LOG = LoggerFactory.getLogger(KVServiceImpl.class);
    private final Dao<byte[]> dao;
    private final HttpServer server;

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        if (dao == null) {
            throw new IllegalArgumentException("DAO cannot be null");
        }
        this.dao = dao;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/v0/status", this::handleStatus);
        server.createContext("/v0/entity", this::handleEntity);
    }

    private void handleStatus(HttpExchange ex) throws IOException {
        try (ex) {
            if ("GET".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    private void handleEntity(HttpExchange ex) throws IOException {
        try (ex) {
            String id = extractId(ex);
            if (id == null) {
                ex.sendResponseHeaders(400, -1);
                return;
            }
            try {
                switch (ex.getRequestMethod()) {
                    case "GET":
                        byte[] val = dao.get(id);
                        ex.sendResponseHeaders(200, val.length);
                        try (OutputStream os = ex.getResponseBody()) {
                            os.write(val);
                        }
                        break;
                    case "PUT":
                        byte[] body = ex.getRequestBody().readAllBytes();
                        dao.upsert(id, body);
                        ex.sendResponseHeaders(201, -1);
                        break;
                    case "DELETE":
                        dao.delete(id);
                        ex.sendResponseHeaders(202, -1);
                        break;
                    default:
                        ex.sendResponseHeaders(405, -1);
                }
            } catch (NoSuchElementException e) {
                ex.sendResponseHeaders(404, -1);
            } catch (IllegalArgumentException e) {
                ex.sendResponseHeaders(400, -1);
            } catch (Exception e) {
                LOG.error("error handling request", e);
                ex.sendResponseHeaders(503, -1);
            }
        }
    }

    private String extractId(HttpExchange ex) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if ("id".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
    }
}
