package ecommerce.auth_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.auth_service.domain.Permission;

public interface PermissionRepository extends ReactiveCrudRepository<Permission, Long> {
}
