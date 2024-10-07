package ecommerce.auth_service.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.auth_service.domain.Permission;
import ecommerce.auth_service.domain.RolePermission;
import reactor.core.publisher.Flux;

public interface RolePermissionRepository extends ReactiveCrudRepository<RolePermission, String> {
    @Query("SELECT p.* FROM permissions p " +
            "JOIN role_permissions rp ON rp.permission_id = p.id " +
            "WHERE rp.role_name = :roleName")
    Flux<Permission> findPermissionsByRoleName(String roleName);

}
