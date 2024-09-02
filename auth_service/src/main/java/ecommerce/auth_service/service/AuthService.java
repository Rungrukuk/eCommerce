package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.GuestUser;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.GuestUserDTO;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserRegisterDTO;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.utils.GuestUserMapper;
import ecommerce.auth_service.utils.RoleMapper;
import ecommerce.auth_service.utils.UserMapper;
import ecommerce.auth_service.utils.Validator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ReactiveRedisOperations<String, GuestUser> redisOperations;
    private final RoleRepository roleRepository;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       JwtTokenProvider jwtTokenProvider, BCryptPasswordEncoder passwordEncoder,
                       ReactiveRedisOperations<String, GuestUser> redisOperations) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.redisOperations = redisOperations;
        this.roleRepository  = roleRepository;
    }

    public Mono<ResponseEntity<Void>> createGuestUser() {
        String guestUserId = UUID.randomUUID().toString();
        return roleRepository.findByName("GUEST").flatMap(
            guestRole->{
                GuestUserDTO guestUserDTO = new GuestUserDTO(guestUserId, guestRole);
                String jwtToken = jwtTokenProvider.createToken(guestUserId, guestRole);
                return redisOperations.opsForValue().set(guestUserId, GuestUserMapper.toGuestUserEntity(guestUserDTO), Duration.ofHours(24))
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(jwtToken);
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).headers(headers).build());
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null)); 
                    }
                });
            }

        );

    }

    public Mono<GuestUser> getGuestUser(String userId) {
        return redisOperations.opsForValue().get(userId);
    }

    public Mono<ResponseEntity<String>> createUser(User user) {
        return userRepository.save(user)
            .flatMap(savedUser -> {
                UserDTO userDTO = UserMapper.toUserDTO(savedUser);
                String jwtToken = jwtTokenProvider.createToken(userDTO.getUserId(), userDTO.getRole());
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
                        return roleRepository.findByName("ROLE").flatMap(
                            roleDTO->{
                                User user = new User();
                                user.setEmail(userDTO.getEmail());
                                user.setPassword(userDTO.getPassword());
                                user.setRole(RoleMapper.toRoleEntity(roleDTO));
                                return createUser(user);
                            }
                        );

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
        return userRepository.findByRefreshToken(passwordEncoder.encode(refreshToken))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid refresh token")))
                .flatMap(user -> {
                    String newJwtToken = jwtTokenProvider.createToken(user.getUserId(), RoleMapper.toRoleDTO(user.getRole()));
                    String newRefreshToken = jwtTokenProvider.createRefreshToken(UserMapper.toUserDTO(user));
                    String hashedRefreshToken = passwordEncoder.encode(newRefreshToken);
                    user.setRefreshToken(hashedRefreshToken);
                    return userRepository.save(user)
                            .then(Mono.just(newJwtToken + " " + newRefreshToken));
                });
    }
}
