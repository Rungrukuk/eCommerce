package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.dto.UserCreateDTO;
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

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    // TODO Add email already been registered response
    // TODO Refactor the registerUser
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(@Headers Map<String, String> metadata, ProtoRequest request) {
        return authService.validate(metadata)
                .flatMap(authResponse -> switch (authResponse.getResponseStatus()) {
                    case CustomResponseStatus.AUTHORIZED_GUEST_USER ->
                        handleUserCreation(
                                new UserCreateDTO(
                                        request.getDataOrDefault("email", ""),
                                        request.getDataOrDefault("password", ""),
                                        request.getDataOrDefault("rePassword", "")));

                    case CustomResponseStatus.UNAUTHORIZED_USER ->
                        unauthorizedResponse(
                                authResponse.getAccessToken(),
                                authResponse.getSessionId());

                    case CustomResponseStatus.UNEXPECTED_ERROR ->
                        errorResponse("Unexpected error occurred. Please try again", null, 500);

                    default -> errorResponse("Bad Request", null, 400);

                })
                .onErrorResume(e -> errorResponse("Unexpected error occurred. Please try again", null, 500));
    }

    @MessageMapping("validateToken")
    public Mono<ProtoResponse> validateAndIssueNewToken(@Headers Map<String, String> metadata) {
        return authService.validate(metadata)
                .flatMap(authResponse -> switch (authResponse.getResponseStatus()) {
                    case CustomResponseStatus.AUTHORIZED_GUEST_USER ->
                        buildResponseWithTokens(
                                authResponse.getAccessToken(),
                                authResponse.getSessionId(),
                                authResponse.getServiceToken(),
                                null,
                                200,
                                "");

                    case CustomResponseStatus.AUTHORIZED_USER ->
                        buildResponseWithTokens(
                                authResponse.getAccessToken(),
                                authResponse.getSessionId(),
                                authResponse.getServiceToken(),
                                authResponse.getRefreshToken(),
                                200,
                                "Your session has been refreshed");

                    case CustomResponseStatus.UNAUTHORIZED_USER ->
                        buildResponseWithTokens(
                                authResponse.getAccessToken(),
                                authResponse.getSessionId(),
                                null,
                                null,
                                401,
                                "Unauthorized Access");

                    default -> errorResponse("Unexpected error occurred. Please try again", null, 500);

                });
    }

    private Mono<ProtoResponse> handleUserCreation(UserCreateDTO userCreateDTO) {
        return userService.createUser(userCreateDTO)
                .flatMap(userResponse -> userResponse.getErrors().isEmpty()
                        // TODO New User Created event should be published
                        ? buildResponseWithTokens(
                                userResponse.getAccessToken(),
                                userResponse.getSessionId(),
                                null,
                                userResponse.getRefreshToken(),
                                201,
                                "User Created Successfully")
                        : errorResponse("Bad Request", userResponse.getErrors(), 400));
    }

    // Might be usefull in the future
    private Mono<ProtoResponse> unauthorizedResponse(String accessToken, String sessionId) {
        return buildResponseWithTokens(accessToken, sessionId, null, null, 401, "Unauthorized Access");
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

    private Mono<ProtoResponse> errorResponse(String message, List<String> errors, int statusCode) {
        return Mono.just(ProtoResponse.newBuilder()
                .setStatusCode(statusCode)
                .setMessage(message)
                .addAllErrors(errors != null ? errors : List.of())
                .build());
    }
}
