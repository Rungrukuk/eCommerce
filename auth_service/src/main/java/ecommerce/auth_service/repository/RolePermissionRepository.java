package ecommerce.auth_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.auth_service.domain.RolePermission;
import reactor.core.publisher.Flux;

public interface RolePermissionRepository extends ReactiveCrudRepository<RolePermission, String> {
    Flux<RolePermission> findByRoleName(String roleName);
}
