package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserRegisterDTO;
import ecommerce.auth_service.mapper.UserMapper;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.utils.Validator;

import java.util.ArrayList;
import java.util.List;

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

    public Mono<ResponseEntity<String>> createUser(User user) {
        return userRepository.save(user)
            .flatMap(savedUser -> {
                UserDTO userDTO = UserMapper.toUserDTO(savedUser);
                String jwtToken = jwtTokenProvider.createToken(userDTO);
                String refreshToken = jwtTokenProvider.createRefreshToken(userDTO);

                savedUser.setRefreshToken(passwordEncoder.encode(refreshToken));
                return userRepository.save(savedUser)
                    .map(updatedUser -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(jwtToken);
                        headers.add("Refresh-Token", refreshToken);

                        return ResponseEntity.status(201)
                            .headers(headers)
                            .body("User created successfully!");
                    });
            });
    }


    public Mono<ResponseEntity<String>> registerUser(UserRegisterDTO userDTO) {
        List<String> errorMessages = new ArrayList<>();
    
        return userRepository.findByEmail(userDTO.getEmail())
            .flatMap(existingUser -> 
                Mono.just(ResponseEntity.badRequest().body("Email is already registered."))
            )
            .switchIfEmpty(
                Mono.defer(() -> {
                    if (!Validator.isValidEmail(userDTO.getEmail())) {
                        errorMessages.add("Email is not valid!");
                    }
    
                    if (!Validator.isStrongPassword(userDTO.getPassword())) {
                        errorMessages.add("""
                            Password is not strong enough. 
                            Ensure it has at least 1 uppercase letter, 1 lowercase letter, 
                            1 special character, and 1 numerical character.
                        """);
                    }
    
                    if (!userDTO.getPassword().equals(userDTO.getRePassword())) {
                        errorMessages.add("Passwords do not match.");
                    }
    
                    if (errorMessages.isEmpty()) {
                        User user = new User();
                        user.setEmail(userDTO.getEmail());
                        user.setPassword(userDTO.getPassword());
                        return createUser(user);
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(String.join(", ", errorMessages)));
                    }
                })
            );
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
