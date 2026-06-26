package company.vk.edu.distrib.compute.ternuraa;

import company.vk.edu.distrib.compute.KVCluster;
import company.vk.edu.distrib.compute.ternuraa.cluster.ClusterNode;
import company.vk.edu.distrib.compute.ternuraa.cluster.EndpointUtils;
import company.vk.edu.distrib.compute.ternuraa.cluster.GrpcInternalClient;
import company.vk.edu.distrib.compute.ternuraa.cluster.RendezvousSharding;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class KVClusterImpl implements KVCluster {
    private final List<ClusterNode> nodes;
    private final GrpcInternalClient grpcClient;

    public KVClusterImpl(List<Integer> ports) {
        List<Integer> sortedPorts = ports.stream().sorted(Comparator.naturalOrder()).toList();
        List<String> nodeIds = RendezvousSharding.sortedNodeIds(sortedPorts);
        ConcurrentMap<String, Integer> nodeGrpcPorts = new ConcurrentHashMap<>();
        for (int httpPort : sortedPorts) {
            nodeGrpcPorts.put(String.valueOf(httpPort), ClusterNode.grpcPortForHttpPort(httpPort));
        }

        this.grpcClient = new GrpcInternalClient();
        this.nodes = sortedPorts.stream()
                .map(httpPort -> new ClusterNode(
                        httpPort,
                        new InMemoryDao(),
                        nodeIds,
                        nodeGrpcPorts,
                        grpcClient
                ))
                .toList();
    }

    @Override
    public void start() {
        nodes.forEach(ClusterNode::start);
    }

    @Override
    public void start(String endpoint) {
        findNode(endpoint).start();
    }

    @Override
    public void stop() {
        for (ClusterNode node : nodes) {
            stopQuietly(node);
        }
    }

    public void close() {
        stop();
        grpcClient.close();
    }

    @Override
    public void stop(String endpoint) {
        stopQuietly(findNode(endpoint));
    }

    @Override
    public List<String> getEndpoints() {
        return nodes.stream().map(ClusterNode::getEndpoint).toList();
    }

    private ClusterNode findNode(String endpoint) {
        int httpPort = EndpointUtils.httpPortFromEndpoint(endpoint);
        return nodes.stream()
                .filter(node -> node.getHttpPort() == httpPort)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint: " + endpoint));
    }

    private static void stopQuietly(ClusterNode node) {
        try {
            node.stop();
        } catch (IllegalStateException ignored) {
            // node was not started
        }
    }
}
