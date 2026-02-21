package ecommerce.auth_service.service.implementation;

import ecommerce.auth_service.ProtoAuthRequest;
import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.MonitoringClient;
import ecommerce.auth_service.service.ReactiveJwtValidationService;
import ecommerce.auth_service.service.RefreshTokenService;
import ecommerce.auth_service.service.RoleService;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.EventType;
import ecommerce.auth_service.util.Roles;
import ecommerce.auth_service.util.TokenHashUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GuestUserServiceImpl guestUserService;
    private final TransactionalOperator transactionalOperator;
    private final RoleService roleService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final MonitoringClient monitoringClient;
    private final ReactiveJwtValidationService jwtValidationService;

    @Override
    public Mono<AuthResponse> validate(ProtoAuthRequest authRequest) {
        return Mono.defer(() -> {
                    String accessToken = authRequest.getAccessToken();
                    String refreshToken = authRequest.getRefreshToken();
                    String sessionId = authRequest.getSessionId();
                    List<String> services = authRequest.getServicesList();
                    List<String> destinations = authRequest.getDestinationsList();
                    String userAgent = authRequest.getUserAgent();
                    String clientCity = authRequest.getClientCity();

                    if ((accessToken.isBlank() || sessionId.isBlank()) && refreshToken.isBlank()) {
                        return unauthenticatedAccessResponse(services, destinations);
                    }

                    return validateAccessTokenAndSession(authRequest)
                            .flatMap(isValid -> isValid
                                    ? handleValidAccessToken(refreshToken, accessToken, sessionId,
                                    services, destinations)
                                    : handleInvalidAccessToken(refreshToken, services, destinations,
                                    userAgent, clientCity));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(e -> {
                    log.error("Unexpected error in validation", e);
                    return createUnexpectedErrorResponse();
                });
    }

    private Mono<Boolean> validateAccessTokenAndSession(ProtoAuthRequest authRequest) {

        String accessToken = authRequest.getAccessToken();
        String sessionId = authRequest.getSessionId();
        String userAgent = authRequest.getUserAgent();
        String clientCity = authRequest.getClientCity();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("refreshToken", authRequest.getRefreshToken());
        metadata.put("services", String.join(",", authRequest.getServicesList()));
        metadata.put("destinations", String.join(",", authRequest.getDestinationsList()));

        if (accessToken.isBlank() && sessionId.isBlank()) {
            monitoringClient.sendEvent(
                    EventType.INVALID_SESSION_TOKEN_COMBINATION,
                    "AUTH_SERVICE",
                    "",
                    userAgent,
                    clientCity,
                    "Blank session id and accessToken with existing refresh token",
                    metadata);

            return Mono.just(false);
        }

        if (accessToken.isBlank()) {
            metadata.put("sessionId", sessionId);
            monitoringClient.sendEvent(
                    EventType.INVALID_SESSION_TOKEN_COMBINATION,
                    "AUTH_SERVICE",
                    "",
                    userAgent,
                    clientCity,
                    "Blank accessToken with existing refresh token, deleting pair from database",
                    metadata);

            return handleBackgroundErrors(
                    List.of(sessionRepository.deleteBySessionId(sessionId))).thenReturn(false);
        }

        if (sessionId.isBlank()) {
            metadata.put("accessToken", accessToken);
            monitoringClient.sendEvent(
                    EventType.INVALID_SESSION_TOKEN_COMBINATION,
                    "AUTH_SERVICE",
                    "",
                    userAgent,
                    clientCity,
                    "Blank sessionId with existing refresh token, deleting pair from database",
                    metadata);
            return handleBackgroundErrors(
                    List.of(sessionRepository.deleteByAccessToken(accessToken))).thenReturn(false);
        }

        return jwtValidationService
                .validateAccessToken(accessToken, userAgent, clientCity, metadata)
                .flatMap(isValidToken -> {
                    metadata.put("accessToken", accessToken);
                    metadata.put("sessionId", sessionId);
                    if (!isValidToken) {
                        monitoringClient.sendEvent(
                                EventType.INVALID_SESSION_TOKEN_COMBINATION,
                                "AUTH_SERVICE",
                                "",
                                userAgent,
                                clientCity,
                                "Invalid access token, deleting pair from database",
                                metadata);
                        return handleBackgroundErrors(
                                List.of(
                                        sessionRepository.deleteByAccessToken(
                                                accessToken),
                                        sessionRepository.deleteBySessionId(
                                                sessionId)))
                                .thenReturn(false);
                    }
                    return sessionRepository
                            .validateSession(accessToken, sessionId)
                            .flatMap(isValidSession -> {
                                if (!isValidSession) {
                                    monitoringClient.sendEvent(
                                            EventType.INVALID_SESSION_TOKEN_COMBINATION,
                                            "AUTH_SERVICE",
                                            "",
                                            userAgent,
                                            clientCity,
                                            "Invalid access token session pair, deleting pair from database",
                                            metadata);
                                    return handleBackgroundErrors(List.of(
                                            sessionRepository
                                                    .deleteByAccessToken(
                                                            accessToken),
                                            sessionRepository
                                                    .deleteBySessionId(
                                                            sessionId)))
                                            .thenReturn(false);
                                }
                                return Mono.just(true);
                            });
                });
    }

    private Mono<Void> handleBackgroundErrors(List<Mono<Boolean>> operations) {
        return Mono.when(operations)
                .doOnError(e -> log.warn("Background cleanup error: {}", e.getMessage()))
                .onErrorComplete();
    }

    private Mono<AuthResponse> handleValidAccessToken(String refreshToken, String accessToken,
            String sessionId,
            List<String> services, List<String> destinations) {
        return parseAccessTokenClaims(accessToken)
                .flatMap(claims -> {
                    String roleName = claims.get("role", String.class);
                    return roleService.hasAccess(roleName, services, destinations)
                            .flatMap(hasAccess -> {
                                if (hasAccess) {
                                    return createServiceToken(claims.getSubject(),
                                            roleName, services,
                                            destinations)
                                            .map(serviceToken -> createAuthResponse(
                                                    accessToken,
                                                    sessionId,
                                                    serviceToken,
                                                    refreshToken,
                                                    roleName.equals(Roles.USER
                                                            .name())
                                                            ?
                                                            CustomResponseStatus.AUTHORIZED_USER
                                                            :
                                                            CustomResponseStatus.AUTHORIZED_GUEST_USER,
                                                    200));
                                } else {
                                    return Mono.just(unauthorizedAccessResponse(
                                            accessToken, sessionId,
                                            refreshToken,
                                            roleName.equals(Roles.USER
                                                    .name())
                                                    ?
                                                    CustomResponseStatus.UNAUTHORIZED_USER
                                                    :
                                                    CustomResponseStatus.UNAUTHORIZED_GUEST_USER));
                                }
                            });
                });
    }

    private Mono<AuthResponse> handleInvalidAccessToken(String refreshToken, List<String> services,
            List<String> destinations, String userAgent, String clientCity) {
        return refreshTokenService.validateRefreshToken(refreshToken, userAgent, clientCity)
                .flatMap(isValid -> !isValid
                        ? handleInvalidRefreshToken(refreshToken, services, destinations)
                        : handleValidRefreshToken(refreshToken, services, destinations,
                        userAgent, clientCity))
                .doOnError(
                        e -> log.error("Error in handleInvalidAccessToken: {}", e.getMessage(), e));
    }

    private Mono<AuthResponse> handleInvalidRefreshToken(String refreshToken,
            List<String> services, List<String> destinations) {
        return encodeRefreshToken(refreshToken)
                .flatMap(encodedToken -> handleBackgroundErrors(
                        List.of(refreshTokenService.deleteByRefreshToken(encodedToken))))
                .then(unauthenticatedAccessResponse(services, destinations));
    }

    private Mono<AuthResponse> handleValidRefreshToken(String refreshToken, List<String> services,
            List<String> destinations, String userAgent, String clientCity) {
        return parseRefreshTokenClaims(refreshToken)
                .flatMap(claims -> {
                    String userId = claims.getSubject();
                    String roleName = claims.get("role", String.class);

                    return Mono.zip(
                                    roleService.hasAccess(roleName, services, destinations),
                                    createAccessToken(userId, roleName),
                                    createRefreshToken(userId, roleName))
                            .flatMap(tuple -> {
                                boolean hasAccess = tuple.getT1();
                                String newAccessToken = tuple.getT2();
                                String newRefreshToken = tuple.getT3();

                                return refreshTokenService.updateRefreshToken(userId,
                                                passwordEncoder.encode(TokenHashUtil
                                                        .hash(newRefreshToken)),
                                                userAgent, clientCity)
                                        .then(sessionRepository.saveSession(
                                                newAccessToken))
                                        .map(savedSession -> hasAccess
                                                ? createAuthResponse(
                                                newAccessToken,
                                                savedSession.getSessionId(),
                                                jwtTokenProvider.createServiceToken(
                                                        userId,
                                                        roleName,
                                                        services,
                                                        destinations),
                                                newRefreshToken,
                                                CustomResponseStatus.AUTHORIZED_USER,
                                                200)
                                                : unauthorizedAccessResponse(
                                                newAccessToken,
                                                savedSession.getSessionId(),
                                                newRefreshToken,
                                                CustomResponseStatus.UNAUTHORIZED_USER));
                            });
                });
    }

    private Mono<String> encodeRefreshToken(String rawRefreshToken) {
        return Mono.fromCallable(() -> passwordEncoder.encode(TokenHashUtil.hash(rawRefreshToken)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AuthResponse> unauthenticatedAccessResponse(List<String> services,
            List<String> destinations) {
        return guestUserService.createGuestUser().flatMap(guestUserResponse -> {
            String guestAccessToken = guestUserResponse.getAccessToken();
            String sessionId = guestUserResponse.getSessionId();
            return parseAccessTokenClaims(guestAccessToken)
                    .flatMap(claims -> {
                        String roleName = claims.get("role", String.class);
                        return roleService.hasAccess(roleName, services, destinations)
                                .flatMap(hasAccess -> {
                                    if (hasAccess) {
                                        return createServiceToken(
                                                claims.getSubject(),
                                                roleName, services,
                                                destinations)
                                                .map(serviceToken -> createAuthResponse(
                                                        guestAccessToken,
                                                        sessionId,
                                                        serviceToken,
                                                        null,
                                                        CustomResponseStatus.AUTHORIZED_GUEST_USER,
                                                        200));
                                    } else {
                                        return Mono.just(
                                                createAuthResponse(
                                                        guestAccessToken,
                                                        sessionId,
                                                        null,
                                                        null,
                                                        CustomResponseStatus.UNAUTHORIZED_GUEST_USER,
                                                        403));
                                    }
                                });
                    });
        });
    }

    private AuthResponse unauthorizedAccessResponse(String accessToken, String sessionId,
            String refreshToken,
            CustomResponseStatus responseStatus) {
        return createAuthResponse(accessToken, sessionId, null, refreshToken, responseStatus, 403);
    }

    private AuthResponse createAuthResponse(String accessToken, String sessionId,
            String serviceToken,
            String refreshToken, CustomResponseStatus status, int statusCode) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken != null ? accessToken : "");
        response.setSessionId(sessionId != null ? sessionId : "");
        response.setServiceToken(serviceToken != null ? serviceToken : "");
        response.setRefreshToken(refreshToken != null ? refreshToken : "");
        response.setResponseStatus(status != null ? status : CustomResponseStatus.UNEXPECTED_ERROR);
        response.setStatusCode(statusCode != 0 ? statusCode : 500);
        return response;
    }

    private Mono<AuthResponse> createUnexpectedErrorResponse() {
        AuthResponse response = new AuthResponse();
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        response.setStatusCode(500);
        return Mono.just(response);
    }

    private Mono<Claims> parseAccessTokenClaims(String token) {
        return Mono.fromCallable(() -> jwtTokenProvider.getAccessTokenClaims(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Claims> parseRefreshTokenClaims(String token) {
        return Mono.fromCallable(() -> jwtTokenProvider.getRefreshTokenClaims(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> createAccessToken(String userId, String roleName) {
        return Mono.fromCallable(() -> jwtTokenProvider.createAccessToken(userId, roleName))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> createRefreshToken(String userId, String roleName) {
        return Mono.fromCallable(() -> jwtTokenProvider.createRefreshToken(userId, roleName))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> createServiceToken(String userId, String roleName, List<String> services,
            List<String> destinations) {
        return Mono.fromCallable(
                        () -> jwtTokenProvider.createServiceToken(userId, roleName, services, destinations))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
