package ecommerce.auth_service.service.implementation;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.InputValidator;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.RefreshTokenService;
import ecommerce.auth_service.service.UserService;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.Roles;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private InputValidator validatorService;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Override
    public Mono<UserResponse> createUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String rePassword = data.get("rePassword");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return Mono.fromCallable(() -> validatorService.validateData(email, password, rePassword))
                .flatMap(messages -> {
                    if (!messages.isEmpty()) {
                        return createBadRequestResponse(messages.toString());
                    }

                    return userRepository.findUserDtoByEmail(email)
                            .flatMap(existingUser -> createBadRequestResponse("User already exists"))
                            .switchIfEmpty(
                                    Mono.defer(() -> processUserCreation(email, password, userAgent, clientCity)));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(e -> {
                    // TODO handle error gracefully
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    UserResponse userResponse = new UserResponse();
                    userResponse.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
                    userResponse.setStatusCode(500);
                    return Mono.just(userResponse);
                });
    }

    @Override
    public Mono<UserResponse> authenticateUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return Mono.fromCallable(() -> validatorService.validateData(email, password))
                .flatMap(messages -> {
                    if (!messages.isEmpty()) {
                        return createBadRequestResponse(messages.toString());
                    }

                    return userRepository.findUserByEmail(email)
                            .flatMap(existingUser -> Mono
                                    .fromCallable(() -> passwordEncoder.matches(password, existingUser.getPassword()))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(passwordMatches -> {
                                        if (passwordMatches) {
                                            return createUserResponse(existingUser.getUserId(), existingUser.getEmail(),
                                                    userAgent, clientCity, 200, "Logged in successfully",
                                                    CustomResponseStatus.OK);
                                        } else {
                                            return createBadRequestResponse("Email or password is incorrect");
                                        }
                                    }))
                            .switchIfEmpty(
                                    Mono.defer(() -> createBadRequestResponse("Email or password is incorrect")));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(e -> {
                    // TODO handle error gracefully
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    UserResponse userResponse = new UserResponse();
                    userResponse.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
                    userResponse.setStatusCode(500);
                    return Mono.just(userResponse);
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

    private Mono<UserResponse> createBadRequestResponse(String message) {
        UserResponse userResponse = new UserResponse();
        userResponse.setMessage(message);
        userResponse.setResponseStatus(CustomResponseStatus.BAD_REQUEST);
        userResponse.setStatusCode(400);
        return Mono.just(userResponse);
    }

    private Mono<UserResponse> processUserCreation(String email, String password, String userAgent, String clientCity) {
        return Mono.fromCallable(() -> passwordEncoder.encode(password))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(encodedPassword -> {
                    User newUser = new User(email, encodedPassword, Roles.USER.name());
                    return entityTemplate.insert(User.class).using(newUser)
                            .flatMap(savedUser -> createUserResponse(savedUser.getUserId(), email, userAgent,
                                    clientCity, 201, "User created succesfully", CustomResponseStatus.CREATED));
                });
    }

    private Mono<UserResponse> createUserResponse(String userId, String email, String userAgent, String clientCity,
            int statusCode, String message, CustomResponseStatus responseStatus) {
        String accessToken = tokenProvider.createAccessToken(userId, Roles.USER.name());
        String refreshToken = tokenProvider.createRefreshToken(userId, Roles.USER.name());

        Mono<RefreshToken> savedRefreshTokenMono = saveRefreshToken(userId, refreshToken, userAgent, clientCity);
        Mono<Session> savedSessionMono = sessionRepository.saveSession(accessToken);

        return Mono.zip(savedRefreshTokenMono, savedSessionMono)
                .map(tuple -> {
                    UserResponse userResponse = new UserResponse();
                    userResponse.setEmail(email);
                    userResponse.setAccessToken(accessToken);
                    userResponse.setRefreshToken(refreshToken);
                    userResponse.setSessionId(tuple.getT2().getSessionId());
                    userResponse.setStatusCode(statusCode);
                    userResponse.setMessage(message);
                    userResponse.setResponseStatus(responseStatus);
                    return userResponse;
                })
                .doOnError(e -> {
                    sessionRepository.deleteByAccessToken(accessToken).subscribe();
                });
    }

    private Mono<RefreshToken> saveRefreshToken(String userId, String refreshToken, String userAgent,
            String clientCity) {
        return Mono.fromCallable(() -> passwordEncoder.encode(refreshToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hashedToken -> {
                    return refreshTokenService.createOrUpdateRefreshToken(userId, hashedToken, userAgent, clientCity);
                });
    }

}
