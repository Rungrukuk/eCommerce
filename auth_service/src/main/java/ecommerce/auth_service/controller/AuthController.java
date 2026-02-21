package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoAuthRequest;
import ecommerce.auth_service.ProtoAuthResponse;
import ecommerce.auth_service.ProtoRequest;
import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@MessageMapping("auth")
public class AuthController {

    private final UserService userService;

    private final JwtTokenProvider tokenProvider;

    private final AuthService authService;

    // TODO add email verification
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(ProtoRequest request) {
        if (tokenProvider.validateServiceToken(request.getMetadataOrDefault("serviceToken", ""))) {
            return userService.createUser(request.getDataMap(), request.getMetadataMap())
                    .flatMap(userResponse -> {
                        return Mono.just(ProtoResponse.newBuilder()
                                .setStatusCode(userResponse.getStatusCode())
                                .setMessage(userResponse.getMessage())
                                .setStatus(userResponse.getResponseStatus().name())
                                .putMetadata("accessToken", userResponse.getAccessToken())
                                .putMetadata("sessionId", userResponse.getSessionId())
                                .putMetadata("refreshToken", userResponse.getRefreshToken())
                                .putData("email", userResponse.getEmail())
                                .build());
                    });
        }
        return Mono.just(
                ProtoResponse.newBuilder()
                        .setStatusCode(403)
                        .setStatus("Forbidden")
                        .setMessage("No required permissions")
                        .build());
    }

    @MessageMapping("validateToken")
    public Mono<ProtoAuthResponse> validateAndIssueNewToken(ProtoAuthRequest request) {
        return authService.validate(request).map(authResponse -> {
            ProtoAuthResponse protoAuthResponse = ProtoAuthResponse.newBuilder()
                    .setStatus(authResponse.getResponseStatus().name())
                    .setStatusCode(authResponse.getStatusCode())
                    .putMetadata("accessToken", authResponse.getAccessToken())
                    .putMetadata("sessionId", authResponse.getSessionId())
                    .putMetadata("serviceToken", authResponse.getServiceToken())
                    .putMetadata("refreshToken", authResponse.getRefreshToken())
                    .build();
            return protoAuthResponse;
        });
    }

    // TODO add email verification
    @MessageMapping("loginUser")
    public Mono<ProtoResponse> loginUser(ProtoRequest request) {
        if (tokenProvider.validateServiceToken(request.getMetadataOrDefault("serviceToken", ""))) {
            return userService.authenticateUser(request.getDataMap(), request.getMetadataMap())
                    .flatMap(userResponse -> {
                        return Mono.just(ProtoResponse.newBuilder()
                                .setStatusCode(userResponse.getStatusCode())
                                .setMessage(userResponse.getMessage())
                                .setStatus(userResponse.getResponseStatus().name())
                                .putMetadata("accessToken", userResponse.getAccessToken())
                                .putMetadata("sessionId", userResponse.getSessionId())
                                .putMetadata("refreshToken", userResponse.getRefreshToken())
                                .putData("email", userResponse.getEmail())
                                .build());
                    });
        }
        return Mono.just(
                ProtoResponse.newBuilder()
                        .setStatusCode(403)
                        .setStatus("Forbidden")
                        .setMessage("No required permissions")
                        .build());
    }
}
