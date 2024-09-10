package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.RefreshToken;
import ecommerce.auth_service.domain.Role;
import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.repository.RefreshTokenRepository;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;
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

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private TokenService tokenService;

        @Mock
        private SessionService sessionService;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @Mock
        private ValidatorService validatorService;

        @Mock
        private TransactionalOperator transactionalOperator;

        @InjectMocks
        private UserService userService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                when(transactionalOperator.transactional(ArgumentMatchers.<Mono<Object>>any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void createUser_Success() {
                UserCreateDTO userCreateDTO = new UserCreateDTO();
                userCreateDTO.setEmail("test@example.com");
                userCreateDTO.setPassword("password");

                when(validatorService.validateData(userCreateDTO)).thenReturn(Collections.emptyList());

                Role role = new Role();
                role.setName("USER");
                when(roleRepository.findByName("USER")).thenReturn(Mono.just(role));

                String accessToken = UUID.randomUUID().toString();
                when(tokenService.createAccessToken(any(String.class), any(String.class))).thenReturn(accessToken);

                String refreshToken = UUID.randomUUID().toString();
                when(tokenService.createRefreshToken(any(String.class), any(String.class))).thenReturn(refreshToken);

                User user = new User(UUID.randomUUID().toString(), userCreateDTO.getEmail(), "encodedPassword", role);
                when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

                when(refreshTokenRepository.save(any(RefreshToken.class)))
                                .thenReturn(Mono.just(new RefreshToken(user.getUserId(), refreshToken)));

                String sessionId = UUID.randomUUID().toString();
                Session session = new Session(sessionId, accessToken);
                when(sessionService.saveSession(any(Session.class))).thenReturn(Mono.just(session));

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(userResponse -> {
                                        System.out.println("SessionId: " + userResponse.getSessionId());
                                        System.out.println("SessionId: " + sessionId);

                                        assertNotNull(userResponse);
                                        assertTrue(userResponse.getAccessToken().equals(accessToken),
                                                        "Access token mismatch");
                                        assertTrue(userResponse.getRefreshToken().equals(refreshToken),
                                                        "Refresh token mismatch");
                                        assertTrue(userResponse.getSessionId().equals(sessionId),
                                                        "Session ID mismatch");
                                        assertNull(userResponse.getErrors(), "Errors should be null");
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void createUser_ValidationErrors() {
                UserCreateDTO userCreateDTO = new UserCreateDTO();
                userCreateDTO.setEmail("invalid@example.com");
                userCreateDTO.setPassword("password");

                when(validatorService.validateData(userCreateDTO))
                                .thenReturn(Collections.singletonList("Invalid email"));

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(userResponse -> {
                                        assertTrue(userResponse.getErrors().contains("Invalid email"));
                                        assertTrue(userResponse.getAccessToken() == null);
                                        assertTrue(userResponse.getRefreshToken() == null);
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void createUser_RoleNotFound() {
                UserCreateDTO userCreateDTO = new UserCreateDTO();
                userCreateDTO.setEmail("test@example.com");
                userCreateDTO.setPassword("password");

                when(validatorService.validateData(userCreateDTO)).thenReturn(Collections.emptyList());

                when(roleRepository.findByName("USER")).thenReturn(Mono.empty());

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(userResponse -> {
                                        assertTrue(userResponse.getErrors().contains("Role not found"));
                                        assertTrue(userResponse.getAccessToken() == null);
                                        assertTrue(userResponse.getRefreshToken() == null);
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void createUser_transactionRollbackOnFailure() {
                UserCreateDTO userCreateDTO = new UserCreateDTO();
                userCreateDTO.setEmail("test@example.com");
                userCreateDTO.setPassword("password");

                when(validatorService.validateData(userCreateDTO)).thenReturn(Collections.emptyList());

                Role role = new Role();
                role.setName("USER");
                when(roleRepository.findByName("USER")).thenReturn(Mono.just(role));

                User user = new User("userId", "test@example.com", "encodedPassword", role);
                when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

                RefreshToken refreshToken = new RefreshToken("userId", "refreshToken");
                when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(refreshToken));

                when(sessionService.saveSession(any(Session.class)))
                                .thenReturn(Mono.error(new RuntimeException("Session save failed")));

                Mono<UserResponse> result = userService.createUser(userCreateDTO);

                StepVerifier.create(result)
                                .expectNextMatches(response -> {
                                        assertNotNull(response.getErrors());
                                        assertTrue(response.getErrors().contains("Session save failed"));
                                        return true;
                                })
                                .verifyComplete();

                verify(userRepository, times(1)).save(any(User.class));
                verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
                verify(sessionService, times(1)).saveSession(any(Session.class));
                verify(transactionalOperator, times(1)).transactional(ArgumentMatchers.<Mono<Object>>any());
        }

}
