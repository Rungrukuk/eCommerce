package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.service.SessionService;
import ecommerce.auth_service.service.TokenService;
import ecommerce.auth_service.service.UserService;
import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SessionService sessionService;

    // TODO RBAC
    // TODO Create User Controller interface and implementation of it
    // TODO send onErrorResume error to centralized error and log
    @MessageMapping("registerUser")
    public Mono<ProtoResponse> registerUser(@Headers Map<String, String> metadata, ProtoRequest request) {
        return validateAccessTokenAndSession(metadata.get("accessToken"), metadata.get("sessionId"))
                .flatMap(isValid -> {
                    if (!isValid) {
                        return unauthorizedResponse();
                    }
                    return handleUserCreation(new UserCreateDTO(
                            request.getDataOrDefault("email", ""),
                            request.getDataOrDefault("password", ""),
                            request.getDataOrDefault("rePassword", "")));
                })
                .onErrorResume(e -> badRequestResponse("Bad Request", Collections.singletonList(e.getMessage()), 400));
    }

    private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
        return Optional.ofNullable(accessToken)
                .filter(tokenService::validateAccessToken)
                .map(token -> sessionService.validateSession(sessionId, accessToken))
                .orElse(Mono.just(false));
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

    private Mono<ProtoResponse> unauthorizedResponse() {
        return badRequestResponse("Unauthorized Access!", null, 403);
    }

    private Mono<ProtoResponse> badRequestResponse(String message, List<String> errors, int statusCode) {
        return Mono.just(ProtoResponse.newBuilder()
                .setStatusCode(statusCode)
                .setMessage(message)
                .addAllErrors(errors != null ? errors : Collections.emptyList())
                .build());
    }

    // TODO Send new User Access Token and API GATEWAY ACCESS TOKEN
    // TODO add role names to enum or maybe send roledto in the token
    @MessageMapping("validateToken")
    public Mono<ProtoResponse> validateAndIssueNewToken(@Headers Map<String, String> metadata) {
        String accessToken = metadata.get("accessToken");
        return validateAccessTokenAndSession(accessToken, metadata.get("sessionId"))
                .flatMap(isValid -> {
                    if (!isValid) {
                        // TODO research and find in creating a new access token with guest user is a
                        // good approach
                        return unauthorizedResponse();
                    }
                    Claims claims = tokenService.getAccessTokenClaims(accessToken);
                    String userId = claims.getSubject();
                    String roleName = claims.get("role", String.class);
                    String userAccessToken = tokenService.createAccessToken(userId, roleName);
                    String gatewayAccessToken = tokenService.createAccessToken(userId, "API_GATEWAY");
                    return Mono.just(ProtoResponse.newBuilder()
                            .putMetadata("API_GATEWAY", gatewayAccessToken)
                            .putMetadata("accessToken", userAccessToken).build());
                });
    }

}
