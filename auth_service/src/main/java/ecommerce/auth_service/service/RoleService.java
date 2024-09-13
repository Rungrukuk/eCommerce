package ecommerce.auth_service.service;

import reactor.core.publisher.Mono;

public interface RoleService {
    public Mono<Boolean> hasAccess(String roleName, String audience, String destination);
}
