package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.Role;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.dto.UserResponse;
import ecommerce.auth_service.repository.RoleRepository;
import ecommerce.auth_service.repository.UserRepository;
import ecommerce.auth_service.utils.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private ValidatorService validatorService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_SuccessfulUserCreation() {
        String userId = UUID.randomUUID().toString();
        String email = "testuser@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        String refreshToken = "refreshToken";
        String accessToken = "accessToken";

        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail(email);
        userCreateDTO.setPassword(password);

        when(validatorService.validateData(any(UserCreateDTO.class))).thenReturn(new ArrayList<>());

        Role userRole = new Role();
        userRole.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(Mono.just(RoleMapper.toRoleDTO(userRole)));

        when(tokenService.createRefreshToken(anyString(), anyString())).thenReturn(refreshToken);
        when(tokenService.createAccessToken(anyString(), anyString())).thenReturn(accessToken);

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(passwordEncoder.encode(refreshToken)).thenReturn("encodedRefreshToken");

        User savedUser = new User(userId, email, encodedPassword, userRole, "encodedRefreshToken");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        Mono<UserResponse> userResponseMono = userService.createUser(userCreateDTO);

        StepVerifier.create(userResponseMono)
                .expectNextMatches(userResponse -> userResponse.getAccessToken().equals(accessToken) &&
                        userResponse.getRefreshToken().equals(refreshToken))
                .verifyComplete();

        verify(roleRepository, times(1)).findByName("USER");
        verify(tokenService, times(1)).createAccessToken(anyString(), anyString());
        verify(tokenService, times(1)).createRefreshToken(anyString(), anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_ValidationError() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("invalid");

        List<String> validationErrors = List.of("Invalid email");
        when(validatorService.validateData(any(UserCreateDTO.class))).thenReturn(validationErrors);

        Mono<UserResponse> userResponseMono = userService.createUser(userCreateDTO);

        StepVerifier.create(userResponseMono)
                .expectNextMatches(userResponse -> userResponse.getErrors().contains("Invalid email"))
                .verifyComplete();

        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_RoleNotFound() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("testuser@example.com");
        userCreateDTO.setPassword("password123");

        when(validatorService.validateData(any(UserCreateDTO.class))).thenReturn(new ArrayList<>());

        when(roleRepository.findByName("USER")).thenReturn(Mono.empty());

        Mono<UserResponse> userResponseMono = userService.createUser(userCreateDTO);

        StepVerifier.create(userResponseMono)
                .expectNextMatches(userResponse -> userResponse.getErrors().contains("Role not found"))
                .verifyComplete();

        verify(roleRepository, times(1)).findByName("USER");
        verify(userRepository, never()).save(any(User.class));
    }
}
