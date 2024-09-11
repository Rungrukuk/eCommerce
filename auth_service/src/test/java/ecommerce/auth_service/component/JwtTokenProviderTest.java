package ecommerce.auth_service.component;

import ecommerce.auth_service.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private String accessToken;
    private String refreshToken;
    private String serviceToken;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("mockAccessToken");
        when(jwtTokenProvider.createRefreshToken(anyString(), anyString())).thenReturn("mockRefreshToken");
        when(jwtTokenProvider.createServiceToken(anyString(), anyString(), anyString())).thenReturn("mockServiceToken");
        when(jwtTokenProvider.validateAccessToken("mockAccessToken")).thenReturn(true);
        when(jwtTokenProvider.validateRefreshToken("mockRefreshToken")).thenReturn(true);
        when(jwtTokenProvider.validateServiceToken("mockServiceToken")).thenReturn(true);

        accessToken = jwtTokenProvider.createAccessToken("12345", "USER");
        refreshToken = jwtTokenProvider.createRefreshToken("12345", "USER");
        serviceToken = jwtTokenProvider.createServiceToken("12345", "USER", "mockService");
    }

    @Test
    public void validateAccessTokenTest() {
        assertTrue(jwtTokenProvider.validateAccessToken(accessToken), "Access Token should be valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("12345");
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtTokenProvider.getAccessTokenClaims(accessToken)).thenReturn(claims);

        Claims tokenClaims = jwtTokenProvider.getAccessTokenClaims(accessToken);
        assertEquals("12345", tokenClaims.getSubject(), "User ID should match");
        assertEquals("USER", tokenClaims.get("role", String.class), "Role should match");
    }

    @Test
    public void validateRefreshTokenTest() {
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken), "Refresh Token should be valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("12345");
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtTokenProvider.getRefreshTokenClaims(refreshToken)).thenReturn(claims);

        Claims tokenClaims = jwtTokenProvider.getRefreshTokenClaims(refreshToken);
        assertEquals("12345", tokenClaims.getSubject(), "User ID should match");
        assertEquals("USER", tokenClaims.get("role", String.class), "Role should match");
    }

    @Test
    public void validateServiceToken() {
        assertTrue(jwtTokenProvider.validateServiceToken(serviceToken), "Service Token should be valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("12345");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getAudience()).thenReturn("mockService");

        when(jwtTokenProvider.getServiceTokenClaims(serviceToken)).thenReturn(claims);

        Claims tokenClaims = jwtTokenProvider.getServiceTokenClaims(serviceToken);
        assertEquals("12345", tokenClaims.getSubject(), "User ID should match");
        assertEquals("USER", tokenClaims.get("role", String.class), "Role should match");
        assertEquals("mockService", tokenClaims.getAudience(), "Audience should match");
    }

    @Test
    public void invalidTokenTest() {
        String invalidToken = "invalid.token";

        when(jwtTokenProvider.validateAccessToken(invalidToken)).thenReturn(false);
        when(jwtTokenProvider.validateRefreshToken(invalidToken)).thenReturn(false);
        when(jwtTokenProvider.validateServiceToken(invalidToken)).thenReturn(false);

        assertFalse(jwtTokenProvider.validateAccessToken(invalidToken), "Invalid Access Token should not be valid");
        assertFalse(jwtTokenProvider.validateRefreshToken(invalidToken), "Invalid Refresh Token should not be valid");
        assertFalse(jwtTokenProvider.validateServiceToken(invalidToken), "Invalid Service Token should not be valid");
    }
}
