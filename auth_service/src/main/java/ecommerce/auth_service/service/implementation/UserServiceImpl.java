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
import ecommerce.auth_service.service.MonitoringClient;
import ecommerce.auth_service.service.RefreshTokenService;
import ecommerce.auth_service.service.UserService;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.EventType;
import ecommerce.auth_service.util.Roles;
import ecommerce.auth_service.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final SessionRepository sessionRepository;
    private final TransactionalOperator transactionalOperator;
    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final InputValidator validatorService;
    private final RefreshTokenService refreshTokenService;
    private final MonitoringClient monitoringClient;

    @Override
    public Mono<UserResponse> createUser(Map<String, String> data, Map<String, String> metadata) {
        String email = data.get("email");
        String password = data.get("password");
        String rePassword = data.get("rePassword");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateInput(email, password, rePassword)
                .flatMap(errors -> !errors.isEmpty()
                        ? badRequest(errors.toString())
                        : userRepository.findUserDtoByEmail(email)
                        .flatMap(__ -> badRequest("User already exists"))
                        .switchIfEmpty(Mono.defer(
                                () -> registerUser(email, password,
                                        userAgent,
                                        clientCity))))
                .as(transactionalOperator::transactional)
                .onErrorResume(this::handleUnexpectedError);
    }

    @Override
    public Mono<UserResponse> authenticateUser(Map<String, String> data,
            Map<String, String> metadata) {

        String email = data.get("email");
        String password = data.get("password");
        String userAgent = metadata.getOrDefault("userAgent", "");
        String clientCity = metadata.getOrDefault("clientCity", "");

        return validateInput(email, password)
                .flatMap(errors -> {
                    if (!errors.isEmpty()) {
                        return badRequest(errors.toString());
                    }
                    return userRepository.findUserByEmail(email)
                            .flatMap(user -> checkPassword(password, user)
                                    .flatMap(matches -> {

                                        if (matches) {
                                            return issueTokens(
                                                    user.getUserId(),
                                                    email,
                                                    userAgent,
                                                    clientCity,
                                                    "Logged in successfully");
                                        }
                                        monitoringClient.sendEvent(
                                                EventType.FAILED_LOGIN_ATTEMPT,
                                                "AUTH_SERVICE",
                                                user.getUserId(),
                                                userAgent,
                                                clientCity,
                                                email,
                                                metadata);

                                        return badRequest(
                                                "Email or password is incorrect");
                                    }))
                            .switchIfEmpty(
                                    Mono.defer(() -> {
                                        monitoringClient.sendEvent(
                                                EventType.FAILED_LOGIN_ATTEMPT,
                                                "AUTH_SERVICE",
                                                "",
                                                userAgent,
                                                clientCity,
                                                email,
                                                metadata);

                                        return badRequest(
                                                "Email or password is incorrect");
                                    }));
                })
                .as(transactionalOperator::transactional)
                .onErrorResume(this::handleUnexpectedError);
    }

    @Override
    public Mono<UserDTO> getUser(String userId) {
        throw new UnsupportedOperationException("getUser not yet implemented");
    }

    @Override
    public Mono<UserDTO> deleteUser(String userId) {
        throw new UnsupportedOperationException("deleteUser not yet implemented");
    }

    @Override
    public Mono<UserDTO> updateUser(UserDTO user) {
        throw new UnsupportedOperationException("updateUser not yet implemented");
    }

    private Mono<UserResponse> registerUser(String email, String password, String userAgent,
            String clientCity) {
        return encodePassword(password)
                .flatMap(encodedPassword -> {
                    User newUser = new User(email, encodedPassword, Roles.USER.name());
                    return entityTemplate.insert(User.class).using(newUser)
                            .flatMap(saved -> issueTokens(
                                    saved.getUserId(), email, userAgent, clientCity,
                                    "User created successfully"));
                });
    }

    private Mono<UserResponse> issueTokens(String userId, String email, String userAgent,
            String clientCity, String message) {
        String accessToken = tokenProvider.createAccessToken(userId, Roles.USER.name());
        String refreshToken = tokenProvider.createRefreshToken(userId, Roles.USER.name());

        return encodeRefreshToken(refreshToken)
                .flatMap(encodedRefreshToken -> {
                    Mono<RefreshToken> saveRefreshToken = refreshTokenService
                            .createOrUpdateRefreshToken(userId,
                                    encodedRefreshToken, userAgent,
                                    clientCity);

                    Mono<Session> saveSession = sessionRepository.saveSession(accessToken);

                    return Mono.zip(saveRefreshToken, saveSession)
                            .map(tuple -> buildSuccessResponse(
                                    email, accessToken, refreshToken,
                                    tuple.getT2().getSessionId(), message))
                            .doOnError(e -> {
                                log.error("Failed to persist tokens for userId={}: {}",
                                        userId, e.getMessage(), e);
                                sessionRepository.deleteByAccessToken(accessToken)
                                        .subscribe();
                            });
                });
    }

    private Mono<String> encodePassword(String rawPassword) {
        return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> encodeRefreshToken(String rawRefreshToken) {
        return Mono.fromCallable(() -> passwordEncoder.encode(TokenHashUtil.hash(rawRefreshToken)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Boolean> checkPassword(String rawPassword, User user) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<String>> validateInput(String email, String password, String rePassword) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password, rePassword));
    }

    private Mono<List<String>> validateInput(String email, String password) {
        return Mono.fromCallable(() -> validatorService.validateData(email, password));
    }

    private UserResponse buildSuccessResponse(String email, String accessToken,
            String refreshToken, String sessionId, String message) {
        UserResponse response = new UserResponse();
        response.setEmail(email);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setSessionId(sessionId);
        response.setStatusCode(200);
        response.setMessage(message);
        response.setResponseStatus(CustomResponseStatus.OK);
        return response;
    }

    private Mono<UserResponse> badRequest(String message) {
        UserResponse response = new UserResponse();
        response.setMessage(message);
        response.setResponseStatus(CustomResponseStatus.BAD_REQUEST);
        response.setStatusCode(400);
        return Mono.just(response);
    }

    private Mono<UserResponse> handleUnexpectedError(Throwable e) {
        log.error("Unexpected error in UserService: {}", e.getMessage(), e);
        UserResponse response = new UserResponse();
        response.setMessage("Unexpected error");
        response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
        response.setStatusCode(500);
        return Mono.just(response);
    }
}
