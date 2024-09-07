package ecommerce.auth_service.service;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import ecommerce.auth_service.security.JwtTokenProvider;

@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;

    public TokenService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String createAccessToken(String userId, String roleName) {
        return jwtTokenProvider.createAccessToken(userId, roleName);
    }

    public String createRefreshToken(String userId, String roleName) {
        return jwtTokenProvider.createRefreshToken(userId, roleName);
    }

    public boolean validateAccessToken(String token) {
        return jwtTokenProvider.validateAccessToken(token);
    }

    public boolean validateRefreshToken(String token) {
        return jwtTokenProvider.validateRefreshToken(token);
    }

    public Claims getAccessTokenClaims(String token) {
        return jwtTokenProvider.getClaims(token, jwtTokenProvider.getAccessTokenPublicKey());
    }

    public Claims getRefreshTokenClaims(String token) {
        return jwtTokenProvider.getClaims(token, jwtTokenProvider.getRefreshTokenPublicKey());
    }
}
