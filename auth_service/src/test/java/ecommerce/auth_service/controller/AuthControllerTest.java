package ecommerce.auth_service.controller;

import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.UserService;
import ecommerce.auth_service.util.CustomResponseStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

        @Mock
        private UserService userService;

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        @BeforeEach
        void setup() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void registerUser_success() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder()
                                .putData("email", "test@example.com")
                                .putData("password", "password123")
                                .putData("rePassword", "password123")
                                .build();

                var authResponse = buildAuthResponse(CustomResponseStatus.AUTHORIZED_USER);
                var userResponse = buildUserResponse();

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));
                when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

                var result = authController.registerUser(metadata, request).block();

                assertNotNull(result);
                assertEquals(201, result.getStatusCode());
                assertEquals("User Created Successfully", result.getMessage());
        }

        @Test
        void registerUser_unauthorized() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder().build(); // Empty request for unauthorized test

                var authResponse = buildAuthResponse(CustomResponseStatus.UNAUTHORIZED_USER);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                var result = authController.registerUser(metadata, request).block();

                assertNotNull(result);
                assertEquals(401, result.getStatusCode());
                assertEquals("Unauthorized Access", result.getMessage());
        }

        @Test
        void registerUser_sessionExpired() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder().build();

                var authResponse = buildAuthResponse(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                var result = authController.registerUser(metadata, request).block();

                assertNotNull(result);
                assertEquals(400, result.getStatusCode());
                assertEquals("Please log out, to create new user", result.getMessage());
        }

        @Test
        void registerUser_unexpectedError() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder().build();

                when(authService.validate(metadata)).thenReturn(Mono.error(new RuntimeException("Error")));

                var result = authController.registerUser(metadata, request).block();

                assertNotNull(result);
                assertEquals(500, result.getStatusCode());
                assertEquals("Unexpected error occurred. Please try again", result.getMessage());
        }

        @Test
        void validateAndIssueNewToken_authorized() {
                Map<String, String> metadata = new HashMap<>();

                var authResponse = buildAuthResponse(CustomResponseStatus.AUTHORIZED_USER);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                var result = authController.validateAndIssueNewToken(metadata).block();

                assertNotNull(result);
                assertEquals(200, result.getStatusCode());
        }

        @Test
        void validateAndIssueNewToken_unauthorized() {
                Map<String, String> metadata = new HashMap<>();

                var authResponse = buildAuthResponse(CustomResponseStatus.UNAUTHORIZED_USER);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                var result = authController.validateAndIssueNewToken(metadata).block();

                assertNotNull(result);
                assertEquals(401, result.getStatusCode());
                assertEquals("Unauthorized Access", result.getMessage());
        }

        @Test
        void validateAndIssueNewToken_sessionExpired() {
                Map<String, String> metadata = new HashMap<>();

                var authResponse = buildAuthResponse(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                var result = authController.validateAndIssueNewToken(metadata).block();

                assertNotNull(result);
                assertEquals(200, result.getStatusCode());
                assertEquals("Your session has been refreshed", result.getMessage());
        }

        private AuthResponse buildAuthResponse(CustomResponseStatus status) {
                return new AuthResponse(
                                "mockAccessToken",
                                "mpockRefreshToken",
                                "mockSessionId",
                                status,
                                null,
                                "mockServiceToken");
        }

        private UserResponse buildUserResponse() {
                return new UserResponse(
                                "mockAccessToken",
                                "mockSessionId",
                                "mockRefreshToken",
                                null,
                                null);
        }

}
