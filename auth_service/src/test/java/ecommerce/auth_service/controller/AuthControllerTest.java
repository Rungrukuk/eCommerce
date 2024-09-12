package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.UserService;
import ecommerce.auth_service.util.CustomResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AuthControllerTest {

        @InjectMocks
        private AuthController authController;

        @Mock
        private AuthService authService;

        @Mock
        private UserService userService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void testRegisterUser_AuthorizedUser_SuccessfulRegistration() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder()
                                .putData("email", "test@example.com")
                                .putData("password", "password123")
                                .putData("rePassword", "password123")
                                .build();

                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.AUTHORIZED_USER);
                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                UserResponse userResponse = new UserResponse();
                userResponse.setAccessToken("testAccessToken");
                userResponse.setSessionId("testSessionId");
                userResponse.setRefreshToken("testRefreshToken");
                userResponse.setErrors(Collections.emptyList());
                when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, request);
                ProtoResponse response = responseMono.block();

                assertEquals(201, response.getStatusCode());
                assertEquals("User Created Successfully", response.getMessage());
                assertEquals("testAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("testSessionId", response.getMetadataMap().get("sessionId"));
                assertEquals("testRefreshToken", response.getMetadataMap().get("refreshToken"));
        }

        @Test
        void testRegisterUser_UnauthorizedUser() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder()
                                .putData("email", "test@example.com")
                                .putData("password", "password123")
                                .putData("rePassword", "password123")
                                .build();

                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.UNAUTHORIZED_USER);
                authResponse.setAccessToken("invalidAccessToken");
                authResponse.setSessionId("invalidSessionId");
                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, request);
                ProtoResponse response = responseMono.block();

                assertEquals(401, response.getStatusCode());
                assertEquals("Unauthorized Access", response.getMessage());
                assertEquals("invalidAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("invalidSessionId", response.getMetadataMap().get("sessionId"));
        }

        @Test
        void testRegisterUser_SessionExpired() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder()
                                .putData("email", "test@example.com")
                                .putData("password", "password123")
                                .putData("rePassword", "password123")
                                .build();

                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION);
                authResponse.setAccessToken("newAccessToken");
                authResponse.setSessionId("newSessionId");
                authResponse.setRefreshToken("newRefreshToken");
                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, request);
                ProtoResponse response = responseMono.block();

                assertEquals(400, response.getStatusCode());
                assertEquals("Please log out, to create new user", response.getMessage());
                assertEquals("newAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("newSessionId", response.getMetadataMap().get("sessionId"));
        }

        @Test
        void testRegisterUser_UnexpectedError() {
                Map<String, String> metadata = new HashMap<>();
                ProtoRequest request = ProtoRequest.newBuilder()
                                .putData("email", "test@example.com")
                                .putData("password", "password123")
                                .putData("rePassword", "password123")
                                .build();

                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, request);
                ProtoResponse response = responseMono.block();

                assertEquals(500, response.getStatusCode());
                assertEquals("Unexpected error occurred. Please try again", response.getMessage());
        }

        @Test
        void testValidateAndIssueNewToken_AuthorizedUser() {
                Map<String, String> metadata = new HashMap<>();
                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.AUTHORIZED_USER);
                authResponse.setAccessToken("testAccessToken");
                authResponse.setSessionId("testSessionId");
                authResponse.setServiceToken("testServiceToken");

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.validateAndIssueNewToken(metadata);
                ProtoResponse response = responseMono.block();

                assertEquals(200, response.getStatusCode());
                assertEquals("", response.getMessage());
                assertEquals("testAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("testSessionId", response.getMetadataMap().get("sessionId"));
                assertEquals("testServiceToken", response.getMetadataMap().get("serviceToken"));
                assertNull(response.getMetadataMap().get("refreshToken"));
        }

        @Test
        void testValidateAndIssueNewToken_SessionExpired_CreatesNewSession() {
                Map<String, String> metadata = new HashMap<>();
                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION);
                authResponse.setAccessToken("testAccessToken");
                authResponse.setSessionId("testSessionId");
                authResponse.setServiceToken("testServiceToken");
                authResponse.setRefreshToken("testRefreshToken");

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.validateAndIssueNewToken(metadata);
                ProtoResponse response = responseMono.block();

                assertEquals(200, response.getStatusCode());
                assertEquals("Your session has been refreshed", response.getMessage());
                assertEquals("testAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("testSessionId", response.getMetadataMap().get("sessionId"));
                assertEquals("testServiceToken", response.getMetadataMap().get("serviceToken"));
                assertEquals("testRefreshToken", response.getMetadataMap().get("refreshToken"));
        }

        @Test
        void testValidateAndIssueNewToken_UnauthorizedUser() {
                Map<String, String> metadata = new HashMap<>();
                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.UNAUTHORIZED_USER);
                authResponse.setAccessToken("testAccessToken");
                authResponse.setSessionId("testSessionId");

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.validateAndIssueNewToken(metadata);
                ProtoResponse response = responseMono.block();

                assertEquals(401, response.getStatusCode());
                assertEquals("Unauthorized Access", response.getMessage());
                assertEquals("testAccessToken", response.getMetadataMap().get("accessToken"));
                assertEquals("testSessionId", response.getMetadataMap().get("sessionId"));
                assertNull(response.getMetadataMap().get("serviceToken"));
                assertNull(response.getMetadataMap().get("refreshToken"));
        }

        @Test
        void testValidateAndIssueNewToken_UnexpectedError() {
                Map<String, String> metadata = new HashMap<>();
                AuthResponse authResponse = new AuthResponse();
                authResponse.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);

                when(authService.validate(metadata)).thenReturn(Mono.just(authResponse));

                Mono<ProtoResponse> responseMono = authController.validateAndIssueNewToken(metadata);
                ProtoResponse response = responseMono.block();

                assertEquals(500, response.getStatusCode());
                assertEquals("Unexpected error occurred. Please try again", response.getMessage());
        }

}
