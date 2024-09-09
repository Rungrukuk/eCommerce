package ecommerce.auth_service.controller;

import ecommerce.auth_service.CreateUserRequest;
import ecommerce.auth_service.CreateUserResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.service.SessionService;
import ecommerce.auth_service.service.TokenService;
import ecommerce.auth_service.service.UserService;
import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;

import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.List;

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
    // TODO Make sure to impelement SSL-TLS in api-gateway also learn about mtls
    // TODO Create User Controller interface and implementation of it
    @MessageMapping("registerUser")
    public Mono<CreateUserResponse> registerUser(RSocketRequester requester, @Headers Map<String, Object> metadata,
            CreateUserRequest request) {
        String accessToken = (String) metadata.get("accessToken");
        String sessionId = (String) metadata.get("sessionId");

        return validateAccessTokenAndSession(accessToken, sessionId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return unauthorizedResponse();
                    }

                    UserCreateDTO userCreateDTO = new UserCreateDTO(request.getEmail(), request.getPassword(),
                            request.getRePassword());

                    return handleUserCreation(requester, userCreateDTO);
                })
                .onErrorResume(e -> badRequestResponse("Bad Request: " + e.getMessage()));
    }

    // Validate access token and session
    private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
        if (accessToken == null || sessionId == null || !tokenService.validateAccessToken(accessToken)) {
            return Mono.just(false);
        }
        return sessionService.validateSession(sessionId, accessToken);
    }

    // Handle user creation and return response
    private Mono<CreateUserResponse> handleUserCreation(RSocketRequester requester, UserCreateDTO userCreateDTO) {
        return userService.createUser(userCreateDTO)
                .flatMap(userResponse -> {
                    CreateUserResponse.Builder responseBuilder = CreateUserResponse.newBuilder();
                    if (userResponse.getErrors().isEmpty()) {
                        responseBuilder.setStatusCode(201)
                                .setBody("User Created Successfully");

                        return sendMetadataWithResponse(
                                requester,
                                responseBuilder.build(),
                                userResponse.getAccessToken(),
                                userResponse.getRefreshToken(),
                                userResponse.getSessionId());
                    } else {
                        return badRequestResponse("Bad Request", userResponse.getErrors());
                    }
                });
    }

    // Send metadata with response
    private Mono<CreateUserResponse> sendMetadataWithResponse(RSocketRequester requester, CreateUserResponse response,
            String accessToken, String refreshToken, String sessionId) {
        return requester.route("responseRoute")
                .metadata(accessToken, MimeTypeUtils.TEXT_PLAIN)
                .metadata(refreshToken, MimeTypeUtils.TEXT_PLAIN)
                .metadata(sessionId, MimeTypeUtils.TEXT_PLAIN)
                .data(response)
                .send()
                .thenReturn(response);
    }

    // Unauthorized response helper
    private Mono<CreateUserResponse> unauthorizedResponse() {
        return Mono.just(CreateUserResponse.newBuilder()
                .setStatusCode(403)
                .setBody("Access Denied! Unauthorized Access")
                .build());
    }

    // Bad request response helper (without errors)
    private Mono<CreateUserResponse> badRequestResponse(String body) {
        return Mono.just(CreateUserResponse.newBuilder()
                .setStatusCode(400)
                .setBody(body)
                .build());
    }

    // Bad request response helper (with errors)
    private Mono<CreateUserResponse> badRequestResponse(String body, List<String> errors) {
        return Mono.just(CreateUserResponse.newBuilder()
                .setStatusCode(400)
                .setBody(body)
                .addAllErrors(errors)
                .build());
    }

    // // TODO Send new User Access Token and API GATEWAY ACCESS TOKEN
    public Mono<String> validateAndIssueNewToken(String token) {
        if (tokenService.validateAccessToken(token)) {
            Claims claims = tokenService.getAccessTokenClaims(token);
            String userId = claims.getSubject();
            String roleName = claims.get("role", String.class);
            tokenService.createAccessToken(userId, roleName);
        }
        return Mono.just("understandable ");
    }

}
