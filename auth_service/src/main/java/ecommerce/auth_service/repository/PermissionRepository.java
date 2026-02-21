package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Permission;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PermissionRepository extends ReactiveCrudRepository<Permission, Long> {
}
