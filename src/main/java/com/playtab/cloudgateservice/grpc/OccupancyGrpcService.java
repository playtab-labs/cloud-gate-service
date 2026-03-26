package com.playtab.cloudgateservice.grpc;

import com.playtab.cloudgateservice.domain.stage.Stage;
import com.playtab.cloudgateservice.domain.stage.StageRepository;
import com.playtab.cloudgateservice.service.OccupancyService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OccupancyGrpcService extends OccupancyServiceGrpc.OccupancyServiceImplBase {

    private final StageRepository stageRepository;
    private final OccupancyService occupancyService;

    public OccupancyGrpcService(StageRepository stageRepository, OccupancyService occupancyService) {
        this.stageRepository = stageRepository;
        this.occupancyService = occupancyService;
    }

    @Override
    public void getOccupancy(StageRequest request, StreamObserver<OccupancyResponse> responseObserver) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElse(null);

        if (stage == null) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Stage not found: " + request.getStageId())
                            .asRuntimeException());
            return;
        }

        long count = occupancyService.getCount(stage.getId());

        OccupancyResponse response = OccupancyResponse.newBuilder()
                .setStageId(stage.getId())
                .setStageName(stage.getName())
                .setCurrentCount(count)
                .setMaxCapacity(stage.getMaxCapacity() != null ? stage.getMaxCapacity() : 0)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAllOccupancy(Empty request, StreamObserver<AllOccupancyResponse> responseObserver) {
        AllOccupancyResponse.Builder builder = AllOccupancyResponse.newBuilder();

        stageRepository.findAll().forEach(stage -> {
            long count = occupancyService.getCount(stage.getId());
            builder.addOccupancies(OccupancyResponse.newBuilder()
                    .setStageId(stage.getId())
                    .setStageName(stage.getName())
                    .setCurrentCount(count)
                    .setMaxCapacity(stage.getMaxCapacity() != null ? stage.getMaxCapacity() : 0)
                    .build());
        });

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void streamOccupancy(StageRequest request, StreamObserver<OccupancyResponse> responseObserver) {
        // TODO: Server-streaming 구현 — Redis Pub/Sub 또는 polling 기반
        // 현재 단일 ECS 태스크 운영이므로 필요 시 구현
        responseObserver.onError(
                io.grpc.Status.UNIMPLEMENTED
                        .withDescription("StreamOccupancy not yet implemented")
                        .asRuntimeException());
    }
}
