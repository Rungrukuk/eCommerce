package ecommerce.user_service.service.Implementation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ecommerce.user_service.service.TokenService;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Service
public class TokenServiceImpl implements TokenService {
    private final PublicKey serviceTokenPublicKey;

    public TokenServiceImpl(
            @Value("${jwt.service.public.key}") String servicePublicKeyStr) throws Exception {
        this.serviceTokenPublicKey = getPublicKeyFromString(servicePublicKeyStr);
    }

    private PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

public String validateTokenAndGetUserId(String token, Destination destination) {
    if (token == null || token.isEmpty()) {
        return null;
    }
    try {
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(serviceTokenPublicKey)
                .setAllowedClockSkewSeconds(60)
                .build();
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        if (claims.get("services", List.class).contains("USER_SERVICE")
                && claims.get("destinations", List.class).contains(destination.name())) {
            return claims.getSubject();
        }
        return null;
    } catch (Exception e) {
        // TODO handle error gracefully
        System.err.println("Token validation failed: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}

}
