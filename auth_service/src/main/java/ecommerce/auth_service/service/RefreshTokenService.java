package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.RefreshToken;
import reactor.core.publisher.Mono;

public interface RefreshTokenService {

    public Mono<Boolean> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity);

    public Mono<Boolean> deleteByRefreshToken(String refreshToken);

    public Mono<RefreshToken> createOrUpdateRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity);

    public Mono<RefreshToken> createRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity);

    public Mono<RefreshToken> updateRefreshToken(String userId, String refreshToken,
            String userAgent,
            String clientCity);
}
