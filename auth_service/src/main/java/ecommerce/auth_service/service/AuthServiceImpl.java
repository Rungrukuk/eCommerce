package ecommerce.auth_service.service;

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
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.Roles;
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

    @Autowired
    private RoleService roleService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Mono<AuthResponse> validate(Map<String, String> metadata) {
        String accessToken = metadata.get("accessToken");
        String refreshToken = metadata.get("refreshToken");
        String sessionId = metadata.get("sessionId");
        String audience = metadata.get("audience");
        String destination = metadata.get("destination");

        return validateAccessTokenAndSession(accessToken, sessionId)
                .flatMap(isValid -> isValid
                        ? handleValidAccessToken(accessToken, sessionId, refreshToken, audience, destination)
                        : handleInvalidAccessToken(refreshToken, audience, destination))
                .onErrorResume(
                        e -> {
                            // TODO handle error gracefully
                            e.printStackTrace();
                            System.err.println(e.getMessage());
                            return createUnexpectedErrorResponse();
                        });
    }

    // TODO validate token functionality can be removed by handling the errors
    private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
        return Mono.just(accessToken)
                .filter(token -> {
                    return jwtTokenProvider.validateAccessToken(token);
                })
                .flatMap(validToken -> sessionRepository.validateSession(sessionId, accessToken)
                        .flatMap(isValidSession -> {
                            if (!isValidSession) {
                                handleBackgroundErrors(List.of(
                                        sessionRepository.deleteByAccessToken(accessToken),
                                        sessionRepository.deleteBySessionId(sessionId)));
                                return Mono.just(false);
                            }
                            return Mono.just(true);
                        }))
                .switchIfEmpty(
                        handleBackgroundErrors(List.of(
                                sessionRepository.deleteBySessionId(sessionId))).thenReturn(false));
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
                                : CustomResponseStatus.AUTHORIZED_GUEST_USER);
                return Mono.just(response);
            }
            return Mono.just(unauthorizedAccessResponse(accessToken, sessionId, refreshToken,
                    CustomResponseStatus.UNAUTHORIZED_GUEST_USER));
        });
    }

    private Mono<AuthResponse> handleInvalidAccessToken(String refreshToken, String audience, String destination) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            handleBackgroundErrors(List.of(
                    refreshTokenRepository.deleteByRefreshToken(passwordEncoder.encode(refreshToken))));

            return unauthenticatedAccessResponse(CustomResponseStatus.UNAUTHENTICATED_GUEST_USER);
        }

        // TODO Make sure to send asynchronous log about session expired
        Claims claims = jwtTokenProvider.getRefreshTokenClaims(refreshToken);
        String roleName = claims.get("role", String.class);

        return roleService.hasAccess(roleName, audience, destination).flatMap(hasAccess -> {
            String userId = claims.getSubject();
            String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, roleName);
            String newAccessToken = jwtTokenProvider.createAccessToken(userId, roleName);

            return refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken))
                    .flatMap(savedRefreshTokenEntity -> sessionRepository.saveSession(newAccessToken)
                            .map(savedSession -> {
                                if (hasAccess) {
                                    String serviceToken = jwtTokenProvider.createServiceToken(userId, roleName,
                                            audience, destination);
                                    AuthResponse authResponse = createAuthResponse(newAccessToken,
                                            savedSession.getSessionId(),
                                            serviceToken, newRefreshToken, CustomResponseStatus.AUTHORIZED_USER);
                                    return authResponse;
                                }
                                return unauthorizedAccessResponse(newAccessToken, savedSession.getSessionId(),
                                        newRefreshToken, CustomResponseStatus.UNAUTHORIZED_USER);
                            }));
        });
    }

    private Mono<AuthResponse> unauthenticatedAccessResponse(CustomResponseStatus responseStatus) {
        return guestUserService.createGuestUser().map(guestUserResponse -> {
            AuthResponse response = new AuthResponse();
            response.setAccessToken(guestUserResponse.getAccessToken());
            response.setSessionId(guestUserResponse.getSessionId());
            response.setResponseStatus(responseStatus);
            return response;
        });
    }

    private AuthResponse unauthorizedAccessResponse(String accessToken, String sessionId, String refreshToken,
            CustomResponseStatus responseStatus) {
        return createAuthResponse(accessToken, sessionId, null, refreshToken, responseStatus);
    }

    private AuthResponse createAuthResponse(String accessToken, String sessionId, String serviceToken,
            String refreshToken, CustomResponseStatus status) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setSessionId(sessionId);
        response.setServiceToken(serviceToken);
        response.setRefreshToken(refreshToken);
        response.setResponseStatus(status);
        return response;
    }

    private Mono<AuthResponse> createUnexpectedErrorResponse() {
        AuthResponse response = new AuthResponse();
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        return Mono.just(response);
    }
}
