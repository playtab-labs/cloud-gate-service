package com.playtab.cloudgateservice.grpc;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

import java.util.UUID;

@GrpcGlobalServerInterceptor
public class IdentityInterceptor implements ServerInterceptor {

    public static final Context.Key<UUID> IDENTITY_ID = Context.key("identity-id");
    private static final Metadata.Key<String> X_IDENTITY_ID =
            Metadata.Key.of("x-identity-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String identityIdHeader = headers.get(X_IDENTITY_ID);

        if (identityIdHeader != null) {
            try {
                UUID identityId = UUID.fromString(identityIdHeader);
                Context ctx = Context.current().withValue(IDENTITY_ID, identityId);
                return Contexts.interceptCall(ctx, call, headers, next);
            } catch (IllegalArgumentException e) {
                call.close(Status.INVALID_ARGUMENT
                        .withDescription("Invalid x-identity-id format: " + identityIdHeader), new Metadata());
                return new ServerCall.Listener<>() {};
            }
        }

        return next.startCall(call, headers);
    }
}
