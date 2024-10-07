package ecommerce.auth_service.service;

import ecommerce.auth_service.ProtoAuthRequest;
import ecommerce.auth_service.dto.AuthResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
    public Mono<AuthResponse> validate(ProtoAuthRequest metadata);
}
