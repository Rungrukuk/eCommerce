package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.InputValidator;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.Roles;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private InputValidator validatorService;

    @Override
    public Mono<UserResponse> createUser(Map<String, String> data) {
        String email = data.get("email");
        String password = data.get("password");
        String rePassword = data.get("rePassword");
        return Mono.fromCallable(() -> validatorService.validateData(email, password, rePassword))
                .flatMap(errors -> {
                    if (!errors.isEmpty()) {
                        UserResponse errorResponse = new UserResponse();
                        errorResponse.setErrors(errors);
                        return Mono.just(errorResponse);
                    }

                    String userId = UUID.randomUUID().toString();
                    String accessToken = tokenProvider.createAccessToken(userId, Roles.USER.name());
                    User user = new User(
                            userId,
                            email,
                            passwordEncoder.encode(password),
                            Roles.USER.name());

                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                String refreshToken = tokenProvider.createRefreshToken(userId,
                                        Roles.USER.name());

                                RefreshToken refreshTokenEntity = new RefreshToken(userId,
                                        refreshToken);
                                return refreshTokenRepository.save(refreshTokenEntity)
                                        .flatMap(savedRefreshToken -> {
                                            return sessionRepository
                                                    .saveSession(accessToken)
                                                    .map(savedSession -> {
                                                        UserResponse userResponse = new UserResponse();
                                                        userResponse.setEmail(email);
                                                        userResponse.setAccessToken(accessToken);
                                                        userResponse.setRefreshToken(refreshToken);
                                                        userResponse.setSessionId(savedSession.getSessionId());
                                                        userResponse.setErrors(errors);
                                                        return userResponse;
                                                    })
                                                    .doOnError(e -> {
                                                        sessionRepository.deleteByAccessToken(accessToken).subscribe();
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
