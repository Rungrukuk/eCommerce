package ecommerce.user_service.repository.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.user.User;
// import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String> {

    // Mono<User> findUserByEmail(String email);

}
