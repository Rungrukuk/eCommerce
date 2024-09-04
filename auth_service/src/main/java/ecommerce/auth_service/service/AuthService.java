package ecommerce.auth_service.service;

import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<UserResponse> createUser(UserCreateDTO user);

    Mono<UserDTO> getUser(String userId);

    Mono<UserDTO> deleteUser(String userId);

    Mono<UserDTO> updateUser(UserDTO user);
}
