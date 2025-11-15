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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
    private RefreshTokenService refreshTokenService;

    @Override
    public Mono<UserResponse> createUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String rePassword = data.get("rePassword");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateUserInput(email, password, rePassword)
                .flatMap(validationMessages -> {
                    if (!validationMessages.isEmpty()) {
                        return createBadRequestResponse(validationMessages.toString());
                    }

                    return userRepository.findUserDtoByEmail(email)
                            .flatMap(existingUser -> createBadRequestResponse("User already exists"))
                            .switchIfEmpty(
                                    Mono.defer(() -> processUserCreation(email, password, userAgent, clientCity)));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(this::handleError);
    }

    @Override
    public Mono<UserResponse> authenticateUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateUserInput(email, password)
                .flatMap(validationMessages -> {
                    if (!validationMessages.isEmpty()) {
                        return createBadRequestResponse(validationMessages.toString());
                    }

                    return userRepository.findUserByEmail(email)
                            .flatMap(existingUser -> verifyPassword(password, existingUser)
                                    .flatMap(passwordMatches -> {
                                        if (passwordMatches) {
                                            return createUserResponse(existingUser.getUserId(),
                                                    existingUser.getEmail(), userAgent, clientCity,
                                                    "Logged in successfully", false);
                                        } else {
                                            return createBadRequestResponse("Email or password is incorrect");
                                        }
                                    }))
                            .switchIfEmpty(createBadRequestResponse("Email or password is incorrect"));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(this::handleError);
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

    private Mono<List<String>> validateUserInput(String email, String password, String rePassword) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password, rePassword));
    }

    private Mono<List<String>> validateUserInput(String email, String password) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password));
    }

    private Mono<UserResponse> processUserCreation(String email, String password, String userAgent, String clientCity) {
        return Mono.fromCallable(() -> passwordEncoder.encode(password))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(encodedPassword -> {
                    User newUser = new User(email, encodedPassword, Roles.USER.name());
                    return entityTemplate.insert(User.class).using(newUser)
                            .flatMap(savedUser -> createUserResponse(savedUser.getUserId(), email, userAgent,
                                    clientCity, "User created successfully", true));
                });
    }

    private Mono<Boolean> verifyPassword(String rawPassword, User existingUser) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, existingUser.getPassword()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<UserResponse> createUserResponse(String userId, String email, String userAgent, String clientCity,
            String message, boolean isNewUser) {
        String accessToken = tokenProvider.createAccessToken(userId, Roles.USER.name());
        String refreshToken = tokenProvider.createRefreshToken(userId, Roles.USER.name());
        return Mono.fromCallable(() -> {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            String preHashed = Base64.getEncoder().encodeToString(hash);
            return passwordEncoder.encode(preHashed);
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(encodedRefreshToken -> {
                    Mono<RefreshToken> savedRefreshTokenMono = isNewUser
                            ? refreshTokenService.createRefreshToken(userId, encodedRefreshToken, userAgent, clientCity)
                            : refreshTokenService.createOrUpdateRefreshToken(userId, encodedRefreshToken, userAgent,
                                    clientCity);

                    Mono<Session> savedSessionMono = sessionRepository.saveSession(accessToken);

                    return Mono.zip(savedRefreshTokenMono, savedSessionMono)
                            .map(tuple -> buildUserResponse(email, accessToken, refreshToken,
                                    tuple.getT2().getSessionId(),
                                    200, message, CustomResponseStatus.OK))
                            .doOnError(e -> sessionRepository.deleteByAccessToken(accessToken).subscribe());
                });
    }

    private UserResponse buildUserResponse(String email, String accessToken, String refreshToken, String sessionId,
            int statusCode, String message, CustomResponseStatus responseStatus) {
        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(email);
        userResponse.setAccessToken(accessToken);
        userResponse.setRefreshToken(refreshToken);
        userResponse.setSessionId(sessionId);
        userResponse.setStatusCode(statusCode);
        userResponse.setMessage(message);
        userResponse.setResponseStatus(responseStatus);
        return userResponse;
    }

    private Mono<UserResponse> createBadRequestResponse(String message) {
        return Mono.just(buildErrorResponse(message, CustomResponseStatus.BAD_REQUEST, 400));
    }

    private UserResponse buildErrorResponse(String message, CustomResponseStatus status, int statusCode) {
        UserResponse response = new UserResponse();
        response.setMessage(message);
        response.setResponseStatus(status);
        response.setStatusCode(statusCode);
        return response;
    }

    private Mono<UserResponse> handleError(Throwable e) {
        // TODO Handle Error Gracefully
        System.out.println(e.getMessage());
        e.printStackTrace();
        return Mono.just(buildErrorResponse("Unexpected error", CustomResponseStatus.UNEXPECTED_ERROR, 500));
    }
}
