package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Role;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RoleRepository extends ReactiveCrudRepository<Role, String> {
}
