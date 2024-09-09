package ecommerce.auth_service.controller;

import ecommerce.auth_service.CreateUserRequest;
import ecommerce.auth_service.CreateUserResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.service.TokenService;
import ecommerce.auth_service.service.UserService;
import ecommerce.auth_service.service.SessionService;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserTest {

        @Mock
        private UserService userService;

        @Mock
        private TokenService tokenService;

        @Mock
        private SessionService sessionService;

        @Mock
        private RSocketRequester requester;

        @InjectMocks
        private AuthController authController;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void registerUser_whenAccessTokenInvalid_shouldReturn403() {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setEmail("test@example.com")
                                .setPassword("password")
                                .setRePassword("password")
                                .build();

                // Simulate invalid access token
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("accessToken", "invalid-token");
                metadata.put("sessionId", "valid-session");

                when(tokenService.validateAccessToken("invalid-token")).thenReturn(false);

                Mono<CreateUserResponse> responseMono = authController.registerUser(requester, metadata, request);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 403
                                                && response.getBody().equals("Access Denied! Unauthorized Access"))
                                .verifyComplete();
        }

        @Test
        void registerUser_whenUserCreatedSuccessfully_shouldReturn201() {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setEmail("test@example.com")
                                .setPassword("password")
                                .setRePassword("password")
                                .build();

                // Simulate valid access token and session
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("accessToken", "valid-token");
                metadata.put("sessionId", "valid-session");

                when(tokenService.validateAccessToken("valid-token")).thenReturn(true);
                when(sessionService.validateSession("valid-session", "valid-token")).thenReturn(Mono.just(true));

                // Mock the user creation
                UserResponse userResponse = new UserResponse("newAccessToken", "newRefreshToken", "newSessionId",
                                Collections.emptyList());
                when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

                // Create mocks for RSocketRequester
                RSocketRequester.RequestSpec requestSpec = mock(RSocketRequester.RequestSpec.class);
                when(requester.route(anyString())).thenReturn(requestSpec);
                when(requestSpec.metadata(anyString(), any())).thenReturn(requestSpec);
                when(requestSpec.data(any())).thenReturn(requestSpec);
                when(requestSpec.send()).thenReturn(Mono.empty());

                Mono<CreateUserResponse> responseMono = authController.registerUser(requester, metadata, request);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 201
                                                && response.getBody().equals("User Created Successfully"))
                                .verifyComplete();

                // Verify interactions
                verify(requester).route("responseRoute");
                verify(requestSpec).metadata("newAccessToken", MimeTypeUtils.TEXT_PLAIN);
                verify(requestSpec).metadata("newRefreshToken", MimeTypeUtils.TEXT_PLAIN);
                verify(requestSpec).metadata("newSessionId", MimeTypeUtils.TEXT_PLAIN);
                verify(requestSpec).data(any(CreateUserResponse.class));
                verify(requestSpec).send();
        }

        @Test
        void registerUser_whenUserCreationFails_shouldReturn400() {
                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setEmail("test@example.com")
                                .setPassword("password")
                                .setRePassword("password")
                                .build();

                // Simulate valid access token and session
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("accessToken", "valid-token");
                metadata.put("sessionId", "valid-session");

                when(tokenService.validateAccessToken("valid-token")).thenReturn(true);
                when(sessionService.validateSession("valid-session", "valid-token")).thenReturn(Mono.just(true));

                // Mock a failed user creation with an error
                UserResponse userResponse = new UserResponse(null, null, null,
                                Collections.singletonList("Error occurred"));
                when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

                Mono<CreateUserResponse> responseMono = authController.registerUser(requester, metadata, request);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 400
                                                && response.getBody().equals("Bad Request")
                                                && response.getErrorsList().contains("Error occurred"))
                                .verifyComplete();
        }
}
