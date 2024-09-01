package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUserId(String userId);
    Mono<User> findByEmail(String email);
    Mono<User> findByRefreshToken(String refreshToken);

    Mono<UserDTO> findUserDtoByUserId(String userId);
    Mono<UserDTO> findUserDtoByEmail(String email);
    Mono<UserDTO> findUserDtoByRefreshToken(String refreshToken);
}
