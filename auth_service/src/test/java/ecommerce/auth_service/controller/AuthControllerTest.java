package ecommerce.auth_service.controller;

import ecommerce.auth_service.ProtoResponse;
import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.service.SessionService;
import ecommerce.auth_service.service.TokenService;
import ecommerce.auth_service.service.UserService;
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
import static org.mockito.Mockito.*;

public class AuthControllerTest {

        @Mock
        private UserService userService;

        @Mock
        private TokenService tokenService;

        @Mock
        private SessionService sessionService;

        @InjectMocks
        private AuthController authController;

        @BeforeEach
        public void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        public void testRegisterUserSuccess() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("accessToken", "validToken");
                metadata.put("sessionId", "validSession");

                ProtoRequest protoRequest = mock(ProtoRequest.class);
                when(protoRequest.getDataOrDefault("email", "")).thenReturn("test@example.com");
                when(protoRequest.getDataOrDefault("password", "")).thenReturn("password123");
                when(protoRequest.getDataOrDefault("rePassword", "")).thenReturn("password123");

                when(tokenService.validateAccessToken("validToken")).thenReturn(true);
                when(sessionService.validateSession("validSession", "validToken")).thenReturn(Mono.just(true));

                UserResponse userResponse = new UserResponse();
                userResponse.setAccessToken("newAccessToken");
                userResponse.setRefreshToken("newRefreshToken");
                userResponse.setSessionId("newSessionId");
                userResponse.setErrors(Collections.emptyList());
                when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, protoRequest);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 201 &&
                                                response.getMessage().equals("User Created Successfully") &&
                                                response.getMetadataMap().get("accessToken").equals("newAccessToken"))
                                .verifyComplete();
        }

        @Test
        public void testRegisterUserUnauthorized() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("accessToken", "invalidToken");
                metadata.put("sessionId", "invalidSession");

                ProtoRequest protoRequest = mock(ProtoRequest.class);

                when(tokenService.validateAccessToken("invalidToken")).thenReturn(false);

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, protoRequest);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 403 &&
                                                response.getMessage().equals("Unauthorized Access!"))
                                .verifyComplete();
        }

        @Test
        public void testRegisterUserBadRequestOnError() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("accessToken", "validToken");
                metadata.put("sessionId", "validSession");

                ProtoRequest protoRequest = mock(ProtoRequest.class);
                when(protoRequest.getDataOrDefault("email", "")).thenReturn("test@example.com");
                when(protoRequest.getDataOrDefault("password", "")).thenReturn("password123");
                when(protoRequest.getDataOrDefault("rePassword", "")).thenReturn("password123");

                when(tokenService.validateAccessToken("validToken")).thenReturn(true);
                when(sessionService.validateSession("validSession", "validToken")).thenReturn(Mono.just(true));

                when(userService.createUser(any(UserCreateDTO.class)))
                                .thenReturn(Mono.error(new RuntimeException("User creation failed")));

                Mono<ProtoResponse> responseMono = authController.registerUser(metadata, protoRequest);

                StepVerifier.create(responseMono)
                                .expectNextMatches(response -> response.getStatusCode() == 400 &&
                                                response.getMessage().equals("Bad Request") &&
                                                response.getErrorsList().contains("User creation failed"))
                                .verifyComplete();
        }
}
