package ecommerce.auth_service.service.implementation;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.MonitoringClient;
import ecommerce.auth_service.service.ReactiveJwtValidationService;
import ecommerce.auth_service.service.RefreshTokenService;
import ecommerce.auth_service.util.EventType;
import ecommerce.auth_service.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MonitoringClient monitoringClient;
    private final ReactiveJwtValidationService jwtValidationService;

    @Override
    public Mono<Boolean> validateRefreshToken(
            String refreshToken,
            String userAgent,
            String clientCity) {

        return jwtValidationService
                .validateRefreshToken(refreshToken, userAgent, clientCity, null)
                .flatMap(isValidToken -> {
                    if (!isValidToken) {
                        return Mono.just(false);
                    }
                    return parseRefreshTokenClaims(refreshToken)
                            .flatMap(claims -> {
                                String userId = claims.getSubject();
                                String hashedToken = TokenHashUtil.hash(refreshToken);
                                return refreshTokenRepository
                                        .findByUserIdAndUserAgentAndClientCity(
                                                userId, userAgent, clientCity)
                                        .flatMap(storedToken -> matchesRefreshToken(
                                                hashedToken,
                                                storedToken.getRefreshToken())
                                                .flatMap(isMatch -> {

                                                    if (isMatch) {
                                                        return Mono.just(true);
                                                    }
                                                    monitoringClient.sendEvent(
                                                            EventType.FAILED_REFRESH_TOKEN_MATCH,
                                                            "AUTH_SERVICE",
                                                            userId,
                                                            userAgent,
                                                            clientCity,
                                                            "Refresh token hash mismatch - possible token theft",
                                                            null);

                                                    return Mono.just(false);
                                                }))
                                        .switchIfEmpty(Mono.defer(() -> {
                                            monitoringClient.sendEvent(
                                                    EventType.FAILED_REFRESH_TOKEN_MATCH,
                                                    "AUTH_SERVICE",
                                                    userId,
                                                    userAgent,
                                                    clientCity,
                                                    "Refresh token not found - possible token theft",
                                                    null);

                                            return Mono.just(false);
                                        }));
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error in validating refresh token", e);
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Boolean> deleteByRefreshToken(String refreshToken) {
        return refreshTokenRepository.deleteByRefreshToken(refreshToken)
                .then(Mono.just(true))
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to delete session by sessionId", e)));
    }

    @Override
    public Mono<RefreshToken> createOrUpdateRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity) {
        return refreshTokenRepository.findByUserIdAndUserAgentAndClientCity(userId, userAgent,
                        clientCity)
                .flatMap(existingToken -> {
                    existingToken.setRefreshToken(refreshToken);
                    return refreshTokenRepository.save(existingToken);
                })
                .switchIfEmpty(Mono.defer(
                        () -> createRefreshToken(userId, refreshToken, userAgent, clientCity)));
    }

    @Override
    public Mono<RefreshToken> updateRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity) {
        return refreshTokenRepository.findByUserIdAndUserAgentAndClientCity(userId, userAgent,
                        clientCity)
                .flatMap(existing -> {
                    existing.setRefreshToken(refreshToken);
                    return refreshTokenRepository.save(existing);
                });
    }

    @Override
    public Mono<RefreshToken> createRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity) {
        RefreshToken newRefreshToken =
                new RefreshToken(userId, refreshToken, userAgent, clientCity);
        return r2dbcEntityTemplate.insert(RefreshToken.class).using(newRefreshToken);
    }

    private Mono<Boolean> matchesRefreshToken(String rawHashedToken, String storedToken) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawHashedToken, storedToken))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<io.jsonwebtoken.Claims> parseRefreshTokenClaims(String refreshToken) {
        return Mono.fromCallable(() -> jwtTokenProvider.getRefreshTokenClaims(refreshToken))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
