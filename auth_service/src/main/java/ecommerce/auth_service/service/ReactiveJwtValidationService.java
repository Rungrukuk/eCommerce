package ecommerce.auth_service.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ReactiveJwtValidationService {
    public Mono<Boolean> validateAccessToken(String accessToken, String userAgent,
            String clientCity,
            Map<String, String> metadata);

    public Mono<Boolean> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity,
            Map<String, String> metadata);

    public Mono<Boolean> validateServiceToken(String serviceToken, String userAgent,
            String clientCity,
            Map<String, String> metadata);

}
