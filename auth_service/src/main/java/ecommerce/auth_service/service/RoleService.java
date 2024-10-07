package ecommerce.auth_service.service;

import java.util.List;

import reactor.core.publisher.Mono;

public interface RoleService {
    public Mono<Boolean> hasAccess(String roleName, List<String> services, List<String> destinations);
}
