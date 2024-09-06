package ecommerce.auth_service;

import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.dto.RoleDTO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = { JwtTokenProvider.class })
public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String accessToken;
    private String refreshToken;

    @Value("${jwt.access.private}")
    private String accessPrivateKey;

    @Value("${jwt.access.public}")
    private String accessPublicKey;

    @Value("${jwt.refresh.private}")
    private String refreshPrivateKey;

    @Value("${jwt.refresh.public}")
    private String refreshPublicKey;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    @BeforeEach
    public void setUp() throws Exception {

        jwtTokenProvider = new JwtTokenProvider(accessPrivateKey, accessPublicKey, refreshPrivateKey, refreshPublicKey,
                accessTokenExpiration, refreshTokenExpiration);

        RoleDTO role = new RoleDTO();
        role.setName("USER");
        String userId = "12345";
        accessToken = jwtTokenProvider.createAccessToken(userId, role);
        refreshToken = jwtTokenProvider.createRefreshToken(userId, role);
    }

    @Test
    public void validateAccessTokenTest() {
        assertTrue(jwtTokenProvider.validateAccessToken(accessToken), "Access Token should be valid");

        Claims claims = jwtTokenProvider.getClaims(accessToken, jwtTokenProvider.getAccessTokenPublicKey());
        assertEquals("12345", claims.getSubject(), "User ID should match");
        assertEquals("USER", claims.get("role", String.class), "Role should match");
    }

    @Test
    public void validateRefreshTokenTest() {
        assertTrue(jwtTokenProvider.validateRefreshToken(refreshToken), "Refresh Token should be valid");

        Claims claims = jwtTokenProvider.getClaims(refreshToken, jwtTokenProvider.getRefreshTokenPublicKey());
        assertEquals("12345", claims.getSubject(), "User ID should match");
        assertEquals("USER", claims.get("role", String.class), "Role should match");
    }

    @Test
    public void invalidTokenTest() {
        String invalidToken = "invalid.token";
        assertFalse(jwtTokenProvider.validateAccessToken(invalidToken), "Invalid Access Token should not be valid");
        assertFalse(jwtTokenProvider.validateRefreshToken(invalidToken), "Invalid Refresh Token should not be valid");
    }
}
