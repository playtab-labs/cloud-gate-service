package com.playtab.cloudgateservice.grpc;

import com.playtab.cloudgateservice.domain.wristband.WristbandOwnership;
import com.playtab.cloudgateservice.service.WristbandService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@GrpcService
public class WristbandGrpcService extends WristbandServiceGrpc.WristbandServiceImplBase {

    private final WristbandService wristbandService;

    public WristbandGrpcService(WristbandService wristbandService) {
        this.wristbandService = wristbandService;
    }

    @Override
    public void linkWristband(LinkWristbandRequest request, StreamObserver<LinkWristbandResponse> responseObserver) {
        UUID identityId = getIdentityId(responseObserver);
        if (identityId == null) return;

        try {
            WristbandOwnership ownership = wristbandService.linkWristband(identityId, request.getRfid());

            LinkWristbandResponse response = LinkWristbandResponse.newBuilder()
                    .setRfid(ownership.getWristband().getRfid())
                    .setActiveDate(ownership.getWristband().getActiveDate().toString())
                    .setLinkedAt(ownership.getLinkedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (WristbandService.WristbandNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (WristbandService.WristbandAlreadyClaimedException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (WristbandService.WristbandLimitExceededException | WristbandService.WristbandDuplicateDateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getMyWristbands(GetMyWristbandsRequest request, StreamObserver<GetMyWristbandsResponse> responseObserver) {
        UUID identityId = getIdentityId(responseObserver);
        if (identityId == null) return;

        List<WristbandOwnership> ownerships = wristbandService.getMyWristbands(identityId);

        GetMyWristbandsResponse.Builder builder = GetMyWristbandsResponse.newBuilder();
        for (WristbandOwnership ownership : ownerships) {
            builder.addWristbands(WristbandInfo.newBuilder()
                    .setRfid(ownership.getWristband().getRfid())
                    .setActiveDate(ownership.getWristband().getActiveDate().toString())
                    .setLinkedAt(ownership.getLinkedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private <T> UUID getIdentityId(StreamObserver<T> responseObserver) {
        UUID identityId = IdentityInterceptor.IDENTITY_ID.get();
        if (identityId == null) {
            responseObserver.onError(
                    Status.UNAUTHENTICATED
                            .withDescription("x-identity-id metadata is required")
                            .asRuntimeException());
            return null;
        }
        return identityId;
    }
}
