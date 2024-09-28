package ecommerce.auth_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import ecommerce.auth_service.domain.RefreshToken;
import reactor.core.publisher.Mono;

@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, String> {

    Mono<RefreshToken> findByUserId(String userId);

    Mono<RefreshToken> findByRefreshToken(String refreshToken);

    Mono<RefreshToken> findByUserIdAndUserAgentAndClientCity(String userId, String userAgent, String clientCity);

    Mono<Void> deleteByRefreshToken(String refreshToken);
}
