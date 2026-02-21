package ecommerce.auth_service.service.implementation;

import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.MonitoringClient;
import ecommerce.auth_service.service.ReactiveJwtValidationService;
import ecommerce.auth_service.util.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReactiveJwtValidationServiceImpl implements ReactiveJwtValidationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MonitoringClient monitoringClient;

    @Override
    public Mono<Boolean> validateAccessToken(String accessToken, String userAgent,
            String clientCity,
            Map<String, String> metadata) {
        return validateToken(accessToken, jwtTokenProvider::validateAccessToken, userAgent,
                clientCity, metadata);
    }

    @Override
    public Mono<Boolean> validateRefreshToken(String refreshToken, String userAgent,
            String clientCity,
            Map<String, String> metadata) {
        return validateToken(refreshToken, jwtTokenProvider::validateRefreshToken, userAgent,
                clientCity, metadata);
    }

    @Override
    public Mono<Boolean> validateServiceToken(String serviceToken, String userAgent,
            String clientCity,
            Map<String, String> metadata) {
        return validateToken(serviceToken, jwtTokenProvider::validateServiceToken, userAgent,
                clientCity, metadata);
    }

    private Mono<Boolean> validateToken(String token,
            Function<String, Boolean> validator,
            String userAgent,
            String clientCity, Map<String, String> metadata) {
        try {
            boolean valid = validator.apply(token);
            return Mono.just(valid);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return Mono.just(false);
        } catch (io.jsonwebtoken.JwtException e) {
            monitoringClient.sendEvent(
                    EventType.INVALID_JWT_FORMAT,
                    "AUTH_SERVICE",
                    null,
                    userAgent,
                    clientCity,
                    "Invalid JWT token format: " + e.getMessage(),
                    metadata);
            return Mono.just(false);
        }
    }
}
