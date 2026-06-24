package company.vk.edu.distrib.compute.ternuraa.cluster;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.ternuraa.grpc.DeleteEntityRequest;
import company.vk.edu.distrib.compute.ternuraa.grpc.EntityResponse;
import company.vk.edu.distrib.compute.ternuraa.grpc.GetEntityRequest;
import company.vk.edu.distrib.compute.ternuraa.grpc.InternalKvGrpc;
import company.vk.edu.distrib.compute.ternuraa.grpc.PutEntityRequest;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public final class GrpcInternalService extends InternalKvGrpc.InternalKvImplBase {
    private final Dao<byte[]> dao;

    public GrpcInternalService(Dao<byte[]> dao) {
        super();
        this.dao = dao;
    }

    @Override
    public void getEntity(GetEntityRequest request, StreamObserver<EntityResponse> responseObserver) {
        handle(() -> LocalEntityUtils.get(dao, request.getId()), responseObserver);
    }

    @Override
    public void putEntity(PutEntityRequest request, StreamObserver<EntityResponse> responseObserver) {
        handle(
                () -> LocalEntityUtils.put(dao, request.getId(), request.getValue().toByteArray()),
                responseObserver
        );
    }

    @Override
    public void deleteEntity(DeleteEntityRequest request, StreamObserver<EntityResponse> responseObserver) {
        handle(() -> LocalEntityUtils.delete(dao, request.getId()), responseObserver);
    }

    private static void handle(
            EntityOperation operation,
            StreamObserver<EntityResponse> responseObserver
    ) {
        try {
            EntityResult result = operation.execute();
            responseObserver.onNext(toResponse(result));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onNext(toResponse(EntityResult.of(400)));
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onNext(toResponse(EntityResult.of(503)));
            responseObserver.onCompleted();
        }
    }

    private static EntityResponse toResponse(EntityResult result) {
        return EntityResponse.newBuilder()
                .setStatusCode(result.statusCode())
                .setBody(com.google.protobuf.ByteString.copyFrom(result.body()))
                .build();
    }

    @FunctionalInterface
    private interface EntityOperation {
        EntityResult execute() throws IOException;
    }
}
