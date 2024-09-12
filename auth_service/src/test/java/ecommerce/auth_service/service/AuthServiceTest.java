package ecommerce.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.dto.GuestUserResponse;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.CustomResponseStatus;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public class AuthServiceTest {

    @Mock
    private SessionRepository sessionRepo;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GuestUserServiceImpl guestUserService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidate_AuthorizedUser() {
        String accessToken = "validAccessToken";
        String sessionId = "validSessionId";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("accessToken", accessToken);
        metadata.put("sessionId", sessionId);

        when(jwtTokenProvider.validateAccessToken(accessToken)).thenReturn(true);
        when(sessionRepo.validateSession(sessionId, accessToken)).thenReturn(Mono.just(true));
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(jwtTokenProvider.getAccessTokenClaims(accessToken)).thenReturn(claims);
        when(jwtTokenProvider.createServiceToken(anyString(), anyString(), anyString())).thenReturn("serviceToken");

        Mono<AuthResponse> responseMono = authService.validate(metadata);
        AuthResponse response = responseMono.block();

        assertEquals(CustomResponseStatus.AUTHORIZED_USER, response.getResponseStatus());
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(sessionId, response.getSessionId());
        assertEquals("serviceToken", response.getServiceToken());
    }

    @Test
    void testValidate_SessionExpiredAndNewSessionCreated() {
        String accessToken = "expiredAccessToken";
        String refreshToken = "validRefreshToken";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("accessToken", accessToken);
        metadata.put("refreshToken", refreshToken);

        when(jwtTokenProvider.validateAccessToken(accessToken)).thenReturn(false);
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user123");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(jwtTokenProvider.getRefreshTokenClaims(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.createRefreshToken(anyString(), anyString())).thenReturn("newRefreshToken");
        when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("newAccessToken");
        when(jwtTokenProvider.createServiceToken(anyString(), anyString(), anyString())).thenReturn("newServiceToken");
        when(sessionRepo.saveSession(accessToken)).thenReturn(Mono.just(mock(Session.class)));

        Mono<AuthResponse> responseMono = authService.validate(metadata);
        AuthResponse response = responseMono.block();

        assertEquals(CustomResponseStatus.SESSION_EXPIRED_CREATED_NEW_SESSION, response.getResponseStatus());
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        assertEquals("newServiceToken", response.getServiceToken());
    }

    @Test
    void testValidate_UnauthorizedUser() {
        String accessToken = "invalidAccessToken";
        String refreshToken = "invalidRefreshToken";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("accessToken", accessToken);
        metadata.put("refreshToken", refreshToken);

        when(jwtTokenProvider.validateAccessToken(accessToken)).thenReturn(false);
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(false);
        when(guestUserService.createGuestUser())
                .thenReturn(Mono.just(new GuestUserResponse(accessToken, refreshToken, null, null)));

        Mono<AuthResponse> responseMono = authService.validate(metadata);
        AuthResponse response = responseMono.block();

        assertEquals(CustomResponseStatus.UNAUTHORIZED_USER, response.getResponseStatus());
    }

    @Test
    void testValidate_UnexpectedError() {
        String accessToken = "validAccessToken";
        String sessionId = "validSessionId";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("accessToken", accessToken);
        metadata.put("sessionId", sessionId);

        when(jwtTokenProvider.validateAccessToken(accessToken)).thenThrow(new RuntimeException("Unexpected error"));

        Mono<AuthResponse> responseMono = authService.validate(metadata);
        AuthResponse response = responseMono.block();

        assertEquals(CustomResponseStatus.UNEXPECTED_ERROR, response.getResponseStatus());
    }
}
