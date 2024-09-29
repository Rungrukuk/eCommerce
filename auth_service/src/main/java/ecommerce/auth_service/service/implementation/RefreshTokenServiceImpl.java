package ecommerce.auth_service.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.RefreshTokenService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Mono<Boolean> validateRefreshToken(String refreshToken, String userAgent, String clientCity) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return Mono.just(false);
        }
        String userId = jwtTokenProvider.getRefreshTokenClaims(refreshToken).getSubject();
        return refreshTokenRepository.findByUserIdAndUserAgentAndClientCity(userId, userAgent, clientCity)
                .flatMap(storedToken -> Mono
                        .fromCallable(() -> passwordEncoder.matches(refreshToken, storedToken.getRefreshToken()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(isMatch -> {
                            if (isMatch) {
                                return Mono.just(true);
                            } else {
                                // TODO: log suspicious event, send an email to user
                                return Mono.just(false);
                            }
                        }))
                .switchIfEmpty(Mono.just(false))
                .onErrorResume(
                        e -> {
                            e.printStackTrace();
                            System.err.println(e.getMessage());
                            return Mono.just(false);
                        });
    }

    @Override
    public Mono<Boolean> deleteByRefreshToken(String refreshToken) {
        return refreshTokenRepository.deleteByRefreshToken(refreshToken)
                .then(Mono.just(true))
                .onErrorResume(
                        e -> Mono.error(new RuntimeException("Failed to delete session by sessionId", e)));
    }

    @Override
    public Mono<RefreshToken> createOrUpdateRefreshToken(String userId, String refreshToken, String userAgent,
            String clientCity) {
        return refreshTokenRepository.findByUserIdAndUserAgentAndClientCity(userId, userAgent, clientCity)
                .flatMap(existingToken -> {
                    existingToken.setRefreshToken(refreshToken);
                    return refreshTokenRepository.save(existingToken);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return createRefreshToken(userId, refreshToken, userAgent, clientCity);
                }));
    }

    @Override
    public Mono<RefreshToken> updateRefreshToken(String userId, String refreshToken, String userAgent,
            String clientCity) {
        RefreshToken refreshTokenObj = new RefreshToken(userId, refreshToken, userAgent, clientCity);
        return refreshTokenRepository.save(refreshTokenObj);
    }

    @Override
    public Mono<RefreshToken> createRefreshToken(String userId, String refreshToken, String userAgent,
            String clientCity) {
        RefreshToken newRefreshToken = new RefreshToken(userId, refreshToken, userAgent, clientCity);
        return r2dbcEntityTemplate.insert(RefreshToken.class).using(newRefreshToken);
    }
}
