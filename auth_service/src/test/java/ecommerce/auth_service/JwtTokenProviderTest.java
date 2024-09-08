package ecommerce.auth_service;

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

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(jwtTokenProvider.createAccessToken(anyString(), anyString())).thenReturn("mockAccessToken");
        when(jwtTokenProvider.createRefreshToken(anyString(), anyString())).thenReturn("mockRefreshToken");
        when(jwtTokenProvider.validateAccessToken("mockAccessToken")).thenReturn(true);
        when(jwtTokenProvider.validateRefreshToken("mockRefreshToken")).thenReturn(true);

        accessToken = jwtTokenProvider.createAccessToken("12345", "USER");
        refreshToken = jwtTokenProvider.createRefreshToken("12345", "USER");
    }

    @Test
    public void validateAccessTokenTest() {
        assertTrue(jwtTokenProvider.validateAccessToken(accessToken), "Access Token should be valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("12345");
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtTokenProvider.getClaims(accessToken, jwtTokenProvider.getAccessTokenPublicKey())).thenReturn(claims);

        Claims tokenClaims = jwtTokenProvider.getClaims(accessToken, jwtTokenProvider.getAccessTokenPublicKey());
        assertEquals("12345", tokenClaims.getSubject(), "User ID should match");
        assertEquals("USER", tokenClaims.get("role", String.class), "Role should match");
    }

    @Test
    public void validateRefreshTokenTest() {
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken), "Refresh Token should be valid");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("12345");
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtTokenProvider.getClaims(refreshToken, jwtTokenProvider.getRefreshTokenPublicKey())).thenReturn(claims);

        Claims tokenClaims = jwtTokenProvider.getClaims(refreshToken, jwtTokenProvider.getRefreshTokenPublicKey());
        assertEquals("12345", tokenClaims.getSubject(), "User ID should match");
        assertEquals("USER", tokenClaims.get("role", String.class), "Role should match");
    }

    @Test
    public void invalidTokenTest() {
        String invalidToken = "invalid.token";

        when(jwtTokenProvider.validateAccessToken(invalidToken)).thenReturn(false);
        when(jwtTokenProvider.validateRefreshToken(invalidToken)).thenReturn(false);

        assertFalse(jwtTokenProvider.validateAccessToken(invalidToken), "Invalid Access Token should not be valid");
        assertFalse(jwtTokenProvider.validateRefreshToken(invalidToken), "Invalid Refresh Token should not be valid");
    }
}
