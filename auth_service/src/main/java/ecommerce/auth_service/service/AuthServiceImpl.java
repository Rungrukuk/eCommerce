package ecommerce.auth_service.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.CustomResponseStatus;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

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

    @Override
    public Mono<AuthResponse> validate(Map<String, String> metadata) {
        String accessToken = metadata.get("accessToken");
        String refreshToken = metadata.get("refreshToken");
        String sessionId = metadata.get("sessionId");

        return validateAccessTokenAndSession(accessToken, sessionId)
                .flatMap(isValid -> {
                    if (isValid) {
                        return handleValidAccessToken(accessToken, sessionId);
                    } else {
                        return handleInvalidAccessToken(accessToken, refreshToken);
                    }
                })
                .onErrorResume(e -> {
                    // TODO Handle the error gracefully
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                    return Mono.just(createUnexpectedErrorResponse());
                });

    }

    private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
        if (accessToken == null || sessionId == null) {
            return Mono.error(new IllegalArgumentException("Access token and session ID must not be null"));
        }
        return Mono.just(accessToken)
                .filter(jwtTokenProvider::validateAccessToken)
                .flatMap(validToken -> sessionRepository.validateSession(sessionId, accessToken)
                        .flatMap(isValidSession -> {
                            if (!isValidSession) {
                                return sessionRepository.deleteSession(accessToken)
                                        .then(sessionRepository.deleteBySessionId(sessionId))
                                        .then(Mono.just(false));
                            }
                            return Mono.just(true);
                        })
                        .onErrorResume(e -> {
                            return sessionRepository.deleteSession(accessToken)
                                    .then(sessionRepository.deleteBySessionId(sessionId))
                                    .then(Mono.error(new RuntimeException("Failed to validate or delete session", e)));
                        }))
                .defaultIfEmpty(false);
    }

    private Mono<AuthResponse> handleValidAccessToken(String accessToken, String sessionId) {
        Claims claims = jwtTokenProvider.getAccessTokenClaims(accessToken);
        String serviceToken = jwtTokenProvider.createServiceToken(claims.getSubject(), claims.get("role", String.class),
                "SERVICE");

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setSessionId(sessionId);
        response.setServiceToken(serviceToken);
        response.setResponseStatus(CustomResponseStatus.AUTHORIZED_USER);
        return Mono.just(response);
    }

    private Mono<AuthResponse> handleInvalidAccessToken(String accessToken, String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            refreshTokenRepository.deleteByRefreshToken(refreshToken)
                    .onErrorResume(e -> {
                        System.err.println("Error occurred while deleting refresh token: " + e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe(
                            // TODO handle logging
                            unused -> System.out.println("Invalid refresh token deleted successfully"),
                            error -> System.err.println("Failed to delete refresh token: " + error.getMessage()));

            return createUnauthorizedAccessResponse();
        }

        Claims claims = jwtTokenProvider.getRefreshTokenClaims(refreshToken);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(claims.getSubject(),
                claims.get("role", String.class));
        String newAccessToken = jwtTokenProvider.createAccessToken(claims.getSubject(),
                claims.get("role", String.class));
        String serviceToken = jwtTokenProvider.createServiceToken(claims.getSubject(),
                claims.get("role", String.class), "SERVICE");

        // TODO Make sure to send asynchronous log about session expired
        return sessionRepository.saveSession(accessToken)
                .map(savedSession -> {
                    AuthResponse response = new AuthResponse();
                    response.setAccessToken(newAccessToken);
                    response.setRefreshToken(newRefreshToken);
                    response.setSessionId(savedSession.getSessionId());
                    response.setServiceToken(serviceToken);
                    response.setResponseStatus(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION);
                    return response;
                });
    }

    private Mono<AuthResponse> createUnauthorizedAccessResponse() {
        return guestUserService.createGuestUser().map(
                guestUserResponse -> {
                    AuthResponse response = new AuthResponse();
                    response.setAccessToken(guestUserResponse.getAccessToken());
                    response.setSessionId(guestUserResponse.getSessionId());
                    response.setResponseStatus(CustomResponseStatus.UNAUTHORIZED_USER);
                    return response;
                });

    }

    private AuthResponse createUnexpectedErrorResponse() {
        AuthResponse response = new AuthResponse();
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        return response;
    }
}
