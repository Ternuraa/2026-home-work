package company.vk.edu.distrib.compute;

import company.vk.edu.distrib.compute.ternuraa.KVClusterImpl;
import company.vk.edu.distrib.compute.ternuraa.KVServiceFactoryImpl;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Server {

    void main(String... args) throws IOException {
        var log = LoggerFactory.getLogger("server");
        if (args.length > 0 && "cluster".equals(args[0])) {
            runCluster(log);
            return;
        }

        var port = 8080;
        KVService storage = new KVServiceFactoryImpl().create(port);
        storage.start();
        log.info("Server started on port {}", port);
        Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
    }

    private static void runCluster(org.slf4j.Logger log) {
        KVClusterImpl cluster = new KVClusterImpl(List.of(8080, 8081));
        cluster.start();
        if (log.isInfoEnabled()) {
            log.info("Cluster started: {}", cluster.getEndpoints());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(cluster::close));
    }
}
