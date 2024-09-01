package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Role;
import ecommerce.auth_service.dto.RoleDTO;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RoleRepository extends ReactiveCrudRepository<Role, Long> {
    Mono<RoleDTO> findByName(String name);
}
