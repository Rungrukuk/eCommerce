package ecommerce.auth_service.service.implementation;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.RoleService;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.Roles;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GuestUserServiceImpl guestUserService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Mono<AuthResponse> validate(Map<String, String> metadata) {
        if (metadata == null) {
            return unauthenticatedAccessResponse(CustomResponseStatus.UNAUTHENTICATED_GUEST_USER);
        }

        return Mono.defer(() -> {
            String accessToken = metadata.getOrDefault("accessToken", "");
            String refreshToken = metadata.getOrDefault("refreshToken", "");
            String sessionId = metadata.getOrDefault("sessionId", "");
            String audience = metadata.getOrDefault("audience", "");
            String destination = metadata.getOrDefault("destination", "");

            if ((accessToken.isBlank() || sessionId.isBlank()) && refreshToken.isBlank()) {
                return unauthenticatedAccessResponse(CustomResponseStatus.UNAUTHENTICATED_GUEST_USER);
            }

            return validateAccessTokenAndSession(accessToken, sessionId)
                    .flatMap(isValid -> isValid
                            ? handleValidAccessToken(accessToken, sessionId, refreshToken, audience, destination)
                            : handleInvalidAccessToken(refreshToken, audience, destination));
        }).onErrorResume(e -> {
            e.printStackTrace();
            System.err.println("Error in validate method: " + e.getMessage());
            return createUnexpectedErrorResponse();
        });
    }

    private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
        if (accessToken.isBlank() && sessionId.isBlank()) {
            return Mono.just(false);
        }

        if (accessToken.isBlank()) {
            return handleBackgroundErrors(List.of(sessionRepository.deleteBySessionId(sessionId)))
                    .thenReturn(false);
        }

        if (sessionId.isBlank()) {
            return handleBackgroundErrors(List.of(sessionRepository.deleteByAccessToken(accessToken)))
                    .thenReturn(false);
        }
        return Mono.just(accessToken)
                .filter(jwtTokenProvider::validateAccessToken)
                .flatMap(validToken -> sessionRepository.validateSession(accessToken, sessionId)
                        .flatMap(isValidSession -> {
                            if (!isValidSession) {
                                return handleBackgroundErrors(List.of(
                                        sessionRepository.deleteByAccessToken(accessToken),
                                        sessionRepository.deleteBySessionId(sessionId)))
                                        .thenReturn(false);
                            }
                            return Mono.just(true);
                        }))
                .switchIfEmpty(
                        handleBackgroundErrors(List.of(sessionRepository.deleteBySessionId(sessionId)))
                                .thenReturn(false));
    }

    // TODO handle error gracefully
    private Mono<Void> handleBackgroundErrors(List<Mono<Boolean>> operations) {
        return Mono.when(operations)
                .doOnError(e -> System.err.println("Error occurred during background operation: " + e.getMessage()))
                .then();
    }

    private Mono<AuthResponse> handleValidAccessToken(String accessToken, String sessionId, String refreshToken,
            String audience, String destination) {
        Claims claims = jwtTokenProvider.getAccessTokenClaims(accessToken);
        String roleName = claims.get("role", String.class);

        return roleService.hasAccess(roleName, audience, destination).flatMap(hasAccess -> {
            if (hasAccess) {
                String serviceToken = jwtTokenProvider.createServiceToken(claims.getSubject(), roleName, audience,
                        destination);
                AuthResponse response = createAuthResponse(accessToken, sessionId, serviceToken, refreshToken,
                        roleName.equals(Roles.USER.name()) ? CustomResponseStatus.AUTHORIZED_USER
                                : CustomResponseStatus.AUTHORIZED_GUEST_USER,
                        200);
                return Mono.just(response);
            }
            return Mono.just(unauthorizedAccessResponse(accessToken, sessionId, refreshToken,
                    roleName.equals(Roles.USER.name()) ? CustomResponseStatus.UNAUTHORIZED_USER
                            : CustomResponseStatus.UNAUTHORIZED_GUEST_USER));
        });
    }

    private Mono<AuthResponse> handleInvalidAccessToken(String refreshToken, String audience, String destination) {
        return Mono.justOrEmpty(refreshToken)
                .filter(token -> !token.isBlank())
                .flatMap(token -> {
                    boolean isValid = jwtTokenProvider.validateRefreshToken(token);
                    if (!isValid) {
                        return Mono.fromCallable(() -> passwordEncoder.encode(refreshToken))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(encodedToken -> handleBackgroundErrors(List.of(
                                        refreshTokenRepository.deleteByRefreshToken(encodedToken))))
                                .then(unauthenticatedAccessResponse(CustomResponseStatus.UNAUTHENTICATED_GUEST_USER));
                    }
                    Claims claims = jwtTokenProvider.getRefreshTokenClaims(token);
                    String roleName = claims.get("role", String.class);

                    return roleService.hasAccess(roleName, audience, destination)
                            .flatMap(hasAccess -> {
                                String userId = claims.getSubject();
                                String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, roleName);
                                String newAccessToken = jwtTokenProvider.createAccessToken(userId, roleName);

                                return refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken))
                                        .flatMap(
                                                savedRefreshTokenEntity -> sessionRepository.saveSession(newAccessToken)
                                                        .map(savedSession -> {
                                                            if (hasAccess) {
                                                                String serviceToken = jwtTokenProvider
                                                                        .createServiceToken(userId,
                                                                                roleName, audience, destination);
                                                                return createAuthResponse(newAccessToken,
                                                                        savedSession.getSessionId(),
                                                                        serviceToken, newRefreshToken,
                                                                        CustomResponseStatus.AUTHORIZED_USER, 200);
                                                            }
                                                            return unauthorizedAccessResponse(newAccessToken,
                                                                    savedSession.getSessionId(), newRefreshToken,
                                                                    CustomResponseStatus.UNAUTHORIZED_USER);
                                                        }));
                            });
                })
                .switchIfEmpty(unauthenticatedAccessResponse(CustomResponseStatus.UNAUTHENTICATED_GUEST_USER));
    }

    private Mono<AuthResponse> unauthenticatedAccessResponse(CustomResponseStatus responseStatus) {
        return guestUserService.createGuestUser().map(guestUserResponse -> {
            AuthResponse response = new AuthResponse();
            response.setAccessToken(guestUserResponse.getAccessToken());
            response.setSessionId(guestUserResponse.getSessionId());
            response.setResponseStatus(responseStatus);
            response.setStatusCode(401);
            return response;
        });
    }

    private AuthResponse unauthorizedAccessResponse(String accessToken, String sessionId, String refreshToken,
            CustomResponseStatus responseStatus) {
        return createAuthResponse(accessToken, sessionId, "", refreshToken, responseStatus, 403);
    }

    private AuthResponse createAuthResponse(String accessToken, String sessionId, String serviceToken,
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
}
