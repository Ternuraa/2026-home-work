package company.vk.edu.distrib.compute;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ArgumentsSource;
import java.net.http.HttpClient;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@ArgumentsSource(KVServiceFactoryArgumentsProvider.class)
class SingleNodeTest extends TestBase {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Parameter
    KVServiceFactory kvServiceFactory;

    @AfterAll
    static void afterAll() {
        HTTP_CLIENT.close();
    }

    @Override
    protected HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    @Test
    void emptyKey() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            int port = randomPort();
            String endpoint = endpoint(port);
            KVService storage = kvServiceFactory.create(port);
            storage.start();
            try {
                // Методы get, delete, upsert берутся из TestBase
                assertEquals(400, get(endpoint, "").statusCode());
                assertEquals(400, delete(endpoint, "").statusCode());
                assertEquals(400, upsert(endpoint, "", new byte[]{0}).statusCode());
            } finally {
                storage.stop();
            }
        });
    }

    // Остальные тесты оставляете как есть, не меняя методы get/delete/upsert
}