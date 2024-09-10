package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
public class UserService implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ReactiveRedisOperations<String, UserDTO> userRedisTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ValidatorService validatorService;

    // TODO Handle Creating the guest user
    public Mono<ResponseEntity<Void>> createGuestUser() {
        String guestUserId = UUID.randomUUID().toString();
        return roleRepository.findByName("GUEST").flatMap(
                guestRole -> {
                    UserDTO guestUserDTO = new UserDTO();
                    guestUserDTO.setUserId(guestUserId);
                    guestUserDTO.setRole(guestRole);
                    String jwtToken = tokenService.createAccessToken(guestUserId, guestRole.getName());
                    return userRedisTemplate.opsForValue().set(guestUserId, guestUserDTO,
                            Duration.ofHours(24))
                            .flatMap(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    HttpHeaders headers = new HttpHeaders();
                                    headers.setBearerAuth(jwtToken);
                                    return Mono
                                            .just(ResponseEntity.status(HttpStatus.CREATED).headers(headers).build());
                                } else {
                                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .body(null));
                                }
                            });
                }

        );

    }

    public Mono<UserDTO> getGuestUser(String userId) {
        return userRedisTemplate.opsForValue().get(userId);
    }

    // TODO Handle authentication of the user
    // public Mono<User> authenticateUser(String email, String password) {
    // return userRepository.findByEmail(email)
    // .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
    // .filter(user -> passwordEncoder.matches(password, user.getPassword()))
    // .flatMap(user -> {
    // String newRefreshToken =
    // jwtTokenProvider.createRefreshToken(UserMapper.toUserDTO(user));
    // String hashedRefreshToken = passwordEncoder.encode(newRefreshToken);

    // user.setRefreshToken(hashedRefreshToken);
    // return userRepository.save(user)
    // .map(updatedUser -> {
    // updatedUser.setRefreshToken(newRefreshToken);
    // return updatedUser;
    // });
    // });
    // }

    @Override
    public Mono<UserResponse> createUser(UserCreateDTO userCreateDTO) {
        return Mono.fromCallable(() -> validatorService.validateData(userCreateDTO))
                .flatMap(errors -> {
                    if (!errors.isEmpty()) {
                        UserResponse errorResponse = new UserResponse();
                        errorResponse.setErrors(errors);
                        return Mono.just(errorResponse);
                    }

                    return roleRepository.findByName("USER")
                            .switchIfEmpty(Mono.error(new RuntimeException("Role not found")))
                            .flatMap(userRole -> {
                                String userId = UUID.randomUUID().toString();
                                String accessToken = tokenService.createAccessToken(userId, userRole.getName());
                                User user = new User(
                                        userId,
                                        userCreateDTO.getEmail(),
                                        passwordEncoder.encode(userCreateDTO.getPassword()),
                                        userRole);

                                return userRepository.save(user)
                                        .flatMap(savedUser -> {
                                            String refreshToken = tokenService.createRefreshToken(userId,
                                                    userRole.getName());

                                            RefreshToken refreshTokenEntity = new RefreshToken(userId,
                                                    refreshToken);
                                            return refreshTokenRepository.save(refreshTokenEntity)
                                                    .flatMap(savedRefreshToken -> {
                                                        String sessionId = UUID.randomUUID().toString();
                                                        Session session = new Session(sessionId, accessToken);
                                                        return sessionService
                                                                .saveSession(session)
                                                                .map(savedSession -> {
                                                                    UserResponse userResponse = new UserResponse();
                                                                    userResponse.setAccessToken(accessToken);
                                                                    userResponse.setRefreshToken(refreshToken);
                                                                    userResponse
                                                                            .setSessionId(savedSession.getSessionId());
                                                                    userResponse.setErrors(errors);
                                                                    return userResponse;
                                                                });
                                                    });
                                        });
                            });
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(e -> {
                    UserResponse errorResponse = new UserResponse();
                    errorResponse.setErrors(List.of(e.getMessage()));
                    return Mono.just(errorResponse);
                });
    }

    @Override
    public Mono<UserDTO> getUser(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }

    @Override
    public Mono<UserDTO> deleteUser(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public Mono<UserDTO> updateUser(UserDTO user) {
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }
}
