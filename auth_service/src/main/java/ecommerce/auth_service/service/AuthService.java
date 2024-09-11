package ecommerce.auth_service.service;

import ecommerce.auth_service.dto.AuthResponse;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface AuthService {
    public Mono<AuthResponse> validate(Map<String, String> metadata);
}
