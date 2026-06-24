package company.vk.edu.distrib.compute.ternuraa.cluster;

import company.vk.edu.distrib.compute.ternuraa.grpc.DeleteEntityRequest;
import company.vk.edu.distrib.compute.ternuraa.grpc.EntityResponse;
import company.vk.edu.distrib.compute.ternuraa.grpc.GetEntityRequest;
import company.vk.edu.distrib.compute.ternuraa.grpc.InternalKvGrpc;
import company.vk.edu.distrib.compute.ternuraa.grpc.PutEntityRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class GrpcInternalClient implements AutoCloseable {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 1L;
    private static final String LOOPBACK_HOST = InetAddress.getLoopbackAddress().getHostAddress();

    private final ConcurrentMap<Integer, ManagedChannel> channels = new ConcurrentHashMap<>();

    public EntityResult getEntity(int grpcPort, String id) {
        EntityResponse response = stub(grpcPort).getEntity(GetEntityRequest.newBuilder().setId(id).build());
        return toResult(response);
    }

    public EntityResult putEntity(int grpcPort, String id, byte[] value) {
        EntityResponse response = stub(grpcPort).putEntity(
                PutEntityRequest.newBuilder().setId(id).setValue(com.google.protobuf.ByteString.copyFrom(value)).build()
        );
        return toResult(response);
    }

    public EntityResult deleteEntity(int grpcPort, String id) {
        EntityResponse response = stub(grpcPort).deleteEntity(
                DeleteEntityRequest.newBuilder().setId(id).build()
        );
        return toResult(response);
    }

    private InternalKvGrpc.InternalKvBlockingStub stub(int grpcPort) {
        ManagedChannel channel = channels.computeIfAbsent(grpcPort, port ->
                ManagedChannelBuilder.forAddress(LOOPBACK_HOST, port)
                        .usePlaintext()
                        .build()
        );
        return InternalKvGrpc.newBlockingStub(channel);
    }

    private static EntityResult toResult(EntityResponse response) {
        return new EntityResult(response.getStatusCode(), response.getBody().toByteArray());
    }

    public EntityResult executeSafely(GrpcOperation operation) {
        try {
            return operation.execute();
        } catch (StatusRuntimeException e) {
            return EntityResult.of(503);
        }
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        channels.clear();
    }

    @FunctionalInterface
    public interface GrpcOperation {
        EntityResult execute();
    }
}
