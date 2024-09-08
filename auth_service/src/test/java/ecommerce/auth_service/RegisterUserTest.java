package ecommerce.auth_service;

import ecommerce.auth_service.controller.AuthController;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RegisterUserTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

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
                .setAccessToken("invalid-token")
                .build();

        when(tokenService.validateAccessToken("invalid-token")).thenReturn(false);

        Mono<CreateUserResponse> responseMono = authController.registerUser(request);

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
                .setAccessToken("valid-token")
                .build();

        when(tokenService.validateAccessToken("valid-token")).thenReturn(true);

        UserResponse userResponse = new UserResponse("newAccessToken", "newRefreshToken", Collections.emptyList());
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

        Mono<CreateUserResponse> responseMono = authController.registerUser(request);

        StepVerifier.create(responseMono)
                .expectNextMatches(response -> response.getStatusCode() == 201
                        && response.getAccessToken().equals("newAccessToken")
                        && response.getRefreshToken().equals("newRefreshToken")
                        && response.getBody().equals("User Created Successfully"))
                .verifyComplete();
    }

    @Test
    void registerUser_whenUserCreationFails_shouldReturn400() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setEmail("test@example.com")
                .setPassword("password")
                .setRePassword("password")
                .setAccessToken("valid-token")
                .build();

        when(tokenService.validateAccessToken("valid-token")).thenReturn(true);

        UserResponse userResponse = new UserResponse(null, null, Collections.singletonList("Error occurred"));
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(Mono.just(userResponse));

        Mono<CreateUserResponse> responseMono = authController.registerUser(request);

        StepVerifier.create(responseMono)
                .expectNextMatches(response -> response.getStatusCode() == 400
                        && response.getBody().equals("Bad Request")
                        && response.getErrorsList().contains("Error occurred"))
                .verifyComplete();
    }
}
