package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import ecommerce.auth_service.util.CustomResponseStatus;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    // TODO handle authenticated user's(logged in user) registration
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(@Headers Map<String, String> metadata, ProtoRequest request) {
        return authService.validate(metadata)
                .flatMap(authResponse -> switch (authResponse.getResponseStatus()) {
                    // Check if there are any need for creating new service token
                    // TODO Instead of creating service token new user created event can be
                    // published
                    case CustomResponseStatus.AUTHORIZED_USER -> handleUserCreation(
                            new UserCreateDTO(
                                    request.getDataOrDefault("email", ""),
                                    request.getDataOrDefault("password", ""),
                                    request.getDataOrDefault("rePassword", "")));
                    case CustomResponseStatus.UNAUTHORIZED_USER -> unauthorizedResponse(
                            authResponse.getAccessToken(), authResponse.getSessionId());
                    case CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION -> sessionExpiredResponse(
                            authResponse.getAccessToken(), authResponse.getSessionId(), authResponse.getRefreshToken());
                    default -> errorResponse("Unexpected error occurred. Please try again", 500);
                })
                .onErrorResume(e -> badRequestResponse("Bad Request", Collections.singletonList(e.getMessage()), 400));
    }

    @MessageMapping("validateToken")
    public Mono<ProtoResponse> validateAndIssueNewToken(@Headers Map<String, String> metadata) {
        return authService.validate(metadata)
                .flatMap(authResponse -> switch (authResponse.getResponseStatus()) {
                    case CustomResponseStatus.AUTHORIZED_USER -> buildResponseWithTokens(
                            authResponse.getAccessToken(),
                            authResponse.getSessionId(),
                            authResponse.getServiceToken(),
                            authResponse.getRefreshToken(),
                            200,
                            "");
                    case CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION ->
                        buildResponseWithTokens(
                                authResponse.getAccessToken(),
                                authResponse.getSessionId(),
                                authResponse.getServiceToken(),
                                authResponse.getRefreshToken(),
                                200,
                                "Your session has been refreshed");
                    case CustomResponseStatus.UNAUTHORIZED_USER -> buildResponseWithErrors(
                            authResponse.getAccessToken(),
                            authResponse.getSessionId(),
                            "Unauthorized Access", 401);
                    default -> errorResponse("Unexpected error occurred. Please try again", 500);
                });
    }

    private Mono<ProtoResponse> handleUserCreation(UserCreateDTO userCreateDTO) {
        return userService.createUser(userCreateDTO)
                .flatMap(userResponse -> userResponse.getErrors().isEmpty()
                        ? successResponse(userResponse)
                        : badRequestResponse("Bad Request", userResponse.getErrors(), 400));
    }

    private Mono<ProtoResponse> successResponse(UserResponse userResponse) {
        return Mono.just(ProtoResponse.newBuilder()
                .setStatusCode(201)
                .setMessage("User Created Successfully")
                .putMetadata("accessToken", userResponse.getAccessToken())
                .putMetadata("refreshToken", userResponse.getRefreshToken())
                .putMetadata("sessionId", userResponse.getSessionId())
                .build());
    }

    private Mono<ProtoResponse> unauthorizedResponse(String accessToken, String sessionId) {
        return buildResponseWithErrors(accessToken, sessionId, "Unauthorized Access", 401);
    }

    private Mono<ProtoResponse> sessionExpiredResponse(String accessToken, String sessionId, String refreshToken) {
        return buildResponseWithTokens(accessToken, sessionId, null, refreshToken, 400,
                "Please log out, to create new user");
    }

    private Mono<ProtoResponse> badRequestResponse(String message, List<String> errors, int statusCode) {
        return errorResponse(message, statusCode, errors);
    }

    private Mono<ProtoResponse> buildResponseWithTokens(String accessToken, String sessionId, String serviceToken,
            String refreshToken, int statusCode, String message) {
        ProtoResponse.Builder responseBuilder = ProtoResponse.newBuilder()
                .setStatusCode(statusCode)
                .setMessage(message)
                .putMetadata("accessToken", accessToken)
                .putMetadata("sessionId", sessionId);

        if (serviceToken != null) {
            responseBuilder.putMetadata("serviceToken", serviceToken);
        }
        if (refreshToken != null) {
            responseBuilder.putMetadata("refreshToken", refreshToken);
        }

        return Mono.just(responseBuilder.build());
    }

    private Mono<ProtoResponse> buildResponseWithErrors(String accessToken, String sessionId, String errorMessage,
            int statusCode) {
        return Mono.just(ProtoResponse.newBuilder()
                .putMetadata("accessToken", accessToken)
                .putMetadata("sessionId", sessionId)
                .setMessage(errorMessage)
                .setStatusCode(statusCode)
                .addErrors(errorMessage)
                .build());
    }

    private Mono<ProtoResponse> errorResponse(String message, int statusCode) {
        return errorResponse(message, statusCode, Collections.singletonList("Internal Server Error"));
    }

    private Mono<ProtoResponse> errorResponse(String message, int statusCode, List<String> errors) {
        return Mono.just(ProtoResponse.newBuilder()
                .setStatusCode(statusCode)
                .setMessage(message)
                .addAllErrors(errors)
                .build());
    }
}
