package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.security.InputValidator;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.Roles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private SessionRepository sessionRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private TransactionalOperator transactionalOperator;

        @Mock
        private JwtTokenProvider tokenProvider;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @Mock
        private InputValidator validatorService;

        @InjectMocks
        private UserServiceImpl userService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                when(transactionalOperator.transactional(ArgumentMatchers.<Mono<Object>>any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void createUser_success() {
                UserCreateDTO userCreateDTO = new UserCreateDTO("test@example.com", "password123", "password123");

                when(validatorService.validateData(userCreateDTO)).thenReturn(List.of());

                String userId = UUID.randomUUID().toString();
                String accessToken = "mockAccessToken";
                String refreshToken = "mockRefreshToken";
                when(tokenProvider.createAccessToken(anyString(), anyString())).thenReturn(accessToken);
                when(tokenProvider.createRefreshToken(anyString(), anyString())).thenReturn(refreshToken);

                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

                User user = new User(userId, userCreateDTO.getEmail(), "encodedPassword", Roles.USER.name());
                when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

                when(refreshTokenRepository.save(any(RefreshToken.class)))
                                .thenReturn(Mono.just(new RefreshToken(userId, refreshToken)));

                when(sessionRepository.saveSession(accessToken))
                                .thenReturn(Mono.just(new Session("mockSessionId", accessToken)));

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(response -> response.getAccessToken().equals(accessToken) &&
                                                response.getRefreshToken().equals(refreshToken) &&
                                                response.getSessionId().equals("mockSessionId") &&
                                                response.getErrors().isEmpty())
                                .verifyComplete();

                verify(userRepository).save(any(User.class));
                verify(refreshTokenRepository).save(any(RefreshToken.class));
                verify(sessionRepository).saveSession(accessToken);
        }

        @Test
        void createUser_transactionErrorBeforeSave() {
                UserCreateDTO userCreateDTO = new UserCreateDTO("test@example.com", "password123", "password123");

                when(validatorService.validateData(userCreateDTO)).thenReturn(List.of());

                String accessToken = "mockAccessToken";
                String refreshToken = "mockRefreshToken";
                when(tokenProvider.createAccessToken(anyString(), anyString())).thenReturn(accessToken);
                when(tokenProvider.createRefreshToken(anyString(), anyString())).thenReturn(refreshToken);

                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

                when(userRepository.save(any(User.class)))
                                .thenReturn(Mono.error(new RuntimeException("Database error")));

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(response -> response.getErrors().contains("Database error"))
                                .verifyComplete();

                verify(userRepository).save(any(User.class));

                verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
                verify(sessionRepository, never()).saveSession(anyString());
        }

}
