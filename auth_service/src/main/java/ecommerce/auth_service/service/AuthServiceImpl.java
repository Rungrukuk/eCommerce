package ecommerce.auth_service.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.CustomResponseStatus;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SessionRepository sessionRepo;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GuestUserServiceImpl guestUserService;

    @Override
    // TODO Do not forget to delete expired sessions or refresh tokens
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
        return Mono.justOrEmpty(accessToken)
                .filter(jwtTokenProvider::validateAccessToken)
                .flatMap(token -> sessionRepo.validateSession(sessionId, accessToken))
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
            return createUnauthorizedAccessResponse();
        }

        Claims claims = jwtTokenProvider.getRefreshTokenClaims(refreshToken);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(claims.getSubject(),
                claims.get("role", String.class));
        String newAccessToken = jwtTokenProvider.createAccessToken(claims.getSubject(),
                claims.get("role", String.class));
        String serviceToken = jwtTokenProvider.createServiceToken(claims.getSubject(),
                claims.get("role", String.class), "SERVICE");

        // TODO Make sure to send asynchornoues log about session expired
        return sessionRepo.saveSession(accessToken)
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
        return guestUserService.createGuestUser().flatMap(
                guestUserResponse -> {
                    AuthResponse response = new AuthResponse();
                    response.setAccessToken(guestUserResponse.getAccessToken());
                    response.setSessionId(guestUserResponse.getSessionId());
                    response.setResponseStatus(CustomResponseStatus.UNAUTHORIZED_USER);
                    return Mono.just(response);
                });

    }

    private AuthResponse createUnexpectedErrorResponse() {
        AuthResponse response = new AuthResponse();
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        return response;
    }
}
