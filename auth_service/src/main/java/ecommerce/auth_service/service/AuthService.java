package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.mapper.UserMapper;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       JwtTokenProvider jwtTokenProvider, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    // public Mono<UserDTO> createGuestUser(String refreshToken) {
    //     return roleRepository.findByName("GUEST")
    //     .flatMap(role -> {
    //         User newGuestUser = new User();
    //         newGuestUser.setUserId(java.util.UUID.randomUUID().toString());
    //         newGuestUser.setRole(RoleMapper.toRoleEntity(role));
    //         newGuestUser.setRefreshToken(passwordEncoder.encode(refreshToken));
    //         return userRepository.save(newGuestUser)
    //                 .map(user -> {
    //                     return UserMapper.toUserDTO(user);
    //                 });
    //     });
    // }

    public Mono<ResponseEntity<UserDTO>> createUser(User user) {
        return userRepository.save(user)
            .flatMap(savedUser -> {
                String jwtToken = jwtTokenProvider.createToken(UserMapper.toUserDTO(savedUser));
                String refreshToken = jwtTokenProvider.createRefreshToken(UserMapper.toUserDTO(savedUser));
                
                savedUser.setRefreshToken(passwordEncoder.encode(refreshToken));
                return userRepository.save(savedUser)
                    .map(updatedUser -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", "Bearer " + jwtToken);
                        headers.add("Refresh-Token", refreshToken);

                        return ResponseEntity.status(201)
                            .headers(headers)
                            .body(UserMapper.toUserDTO(updatedUser));
                    });
            });
    }

    public Mono<ResponseEntity<UserDTO>> registerUser(User user) {
        // TODO Check the given user details and implement email verification logic
        return createUser(user);
    }

    public Mono<User> authenticateUser(String email, String password) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .flatMap(user -> {
                    String newRefreshToken = jwtTokenProvider.createRefreshToken(UserMapper.toUserDTO(user));
                    String hashedRefreshToken = passwordEncoder.encode(newRefreshToken);

                    user.setRefreshToken(hashedRefreshToken);
                    return userRepository.save(user)
                            .map(updatedUser -> {
                                updatedUser.setRefreshToken(newRefreshToken);
                                return updatedUser;
                            });
                });
    }

    public Mono<String> refreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid refresh token")))
                .flatMap(user -> {
                    if (passwordEncoder.matches(refreshToken, user.getRefreshToken())) {
                        String newJwtToken = jwtTokenProvider.createToken(UserMapper.toUserDTO(user));
                        String newRefreshToken = jwtTokenProvider.createRefreshToken(UserMapper.toUserDTO(user));
                        String hashedRefreshToken = passwordEncoder.encode(newRefreshToken);

                        user.setRefreshToken(hashedRefreshToken);
                        return userRepository.save(user)
                                .then(Mono.just(newJwtToken + " " + newRefreshToken));
                    } else {
                        return Mono.error(new IllegalArgumentException("Invalid refresh token"));
                    }
                });
    }
}
