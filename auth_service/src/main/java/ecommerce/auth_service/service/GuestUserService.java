package ecommerce.auth_service.service;

import ecommerce.auth_service.dto.GuestUserResponse;
import reactor.core.publisher.Mono;

public interface GuestUserService {
    public Mono<GuestUserResponse> createGuestUser();
}
