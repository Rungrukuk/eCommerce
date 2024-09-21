package ecommerce.auth_service.service;

import ecommerce.auth_service.dto.BaseResponse;
import reactor.core.publisher.Mono;

public interface GuestUserService {
    public Mono<BaseResponse> createGuestUser();
}
