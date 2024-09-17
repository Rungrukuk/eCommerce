package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoRequest;
import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.ProtoAuthRequest;
import ecommerce.auth_service.ProtoAuthResponse;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    private AuthService authService;

    // TODO Add email already been registered response
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(@Headers Map<String, String> metadata, ProtoRequest request) {
        if (tokenProvider.validateServiceToken(metadata.get("serviceToken"))) {
            return userService.createUser(request.getDataMap())
                    .flatMap(userResponse -> userResponse.getErrors().isEmpty()
                            // TODO New User Created event should be published
                            ? Mono.just(ProtoResponse.newBuilder()
                                    .setStatusCode(201)
                                    .setMessage("User Created Successfully")
                                    .putMetadata("accessToken", userResponse.getAccessToken())
                                    .putMetadata("sessionId", userResponse.getSessionId())
                                    .putMetadata("refreshToken", userResponse.getRefreshToken())
                                    .putData("email", userResponse.getEmail())
                                    .build())
                            : errorResponse("Bad Request", userResponse.getErrors(), 400));
        }
        return Mono.just(
                ProtoResponse.newBuilder()
                        .setStatusCode(403)
                        .setMessage("Forbidden")
                        .build());
    }

    @MessageMapping("validateToken")
    public Mono<ProtoAuthResponse> validateAndIssueNewToken(ProtoAuthRequest request) {
        return authService.validate(request.getMetadataMap()).map(authResponse -> {
            ProtoAuthResponse protoAuthResponse = ProtoAuthResponse.newBuilder()
                    .setMessage(authResponse.getResponseStatus().name())
                    .putMetadata("accessToken", authResponse.getAccessToken())
                    .putMetadata("sessionId", authResponse.getSessionId())
                    .putMetadata("serviceToken", authResponse.getServiceToken())
                    .putMetadata("refreshToken", authResponse.getRefreshToken())
                    .build();
            return protoAuthResponse;
        });
    }

    private Mono<ProtoResponse> errorResponse(String message, List<String> errors, int statusCode) {
        return Mono.just(ProtoResponse.newBuilder()
                .setStatusCode(statusCode)
                .setMessage(message)
                .addAllErrors(errors != null ? errors : List.of())
                .build());
    }
}
