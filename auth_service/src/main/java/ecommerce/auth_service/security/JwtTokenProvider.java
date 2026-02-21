package ecommerce.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    private final PrivateKey accessTokenPrivateKey;
    private final PublicKey accessTokenPublicKey;

    private final PrivateKey serviceTokenPrivateKey;
    private final PublicKey serviceTokenPublicKey;

    private final PrivateKey refreshTokenPrivateKey;
    private final PublicKey refreshTokenPublicKey;

    private final long accessTokenExpiration;

    private final long refreshTokenExpiration;

    private final long serviceTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.access.private.key}") String accessPrivateKeyStr,
            @Value("${jwt.access.public.key}") String accessPublicKeyStr,
            @Value("${jwt.refresh.private.key}") String refreshPrivateKeyStr,
            @Value("${jwt.refresh.public.key}") String refreshPublicKeyStr,
            @Value("${jwt.service.private.key}") String servicePrivateKeyStr,
            @Value("${jwt.service.public.key}") String servicePublicKeyStr,
            @Value("${jwt.access.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh.expiration}") long refreshTokenExpiration,
            @Value("${jwt.service.expiration}") long serviceTokenExpiration) throws Exception {
        this.accessTokenPrivateKey = getPrivateKeyFromString(accessPrivateKeyStr);
        this.accessTokenPublicKey = getPublicKeyFromString(accessPublicKeyStr);
        this.refreshTokenPrivateKey = getPrivateKeyFromString(refreshPrivateKeyStr);
        this.refreshTokenPublicKey = getPublicKeyFromString(refreshPublicKeyStr);
        this.serviceTokenPrivateKey = getPrivateKeyFromString(servicePrivateKeyStr);
        this.serviceTokenPublicKey = getPublicKeyFromString(servicePublicKeyStr);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.serviceTokenExpiration = serviceTokenExpiration;
    }

    private PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public String createAccessToken(String userId, String roleName) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleName);

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(accessTokenPrivateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    public String createServiceToken(String userId, String roleName, List<String> services,
            List<String> destinations) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleName);
        claims.put("services", services);
        claims.put("destinations", destinations);

        Date now = new Date();
        Date validity = new Date(now.getTime() + serviceTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(serviceTokenPrivateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    public String createRefreshToken(String userId, String roleName) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleName);

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(refreshTokenPrivateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenPublicKey);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenPublicKey);
    }

    public boolean validateServiceToken(String token) {
        return validateToken(token, serviceTokenPublicKey);
    }

    private boolean validateToken(String token, PublicKey publicKey) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .setAllowedClockSkewSeconds(60)
                .build();
        jwtParser.parseClaimsJws(token);
        return true;
    }

    private Claims getClaims(String token, PublicKey publicKey) {
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build();
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public Claims getAccessTokenClaims(String token) {
        return getClaims(token, accessTokenPublicKey);
    }

    public Claims getRefreshTokenClaims(String token) {
        return getClaims(token, refreshTokenPublicKey);
    }

    public Claims getServiceTokenClaims(String token) {
        return getClaims(token, serviceTokenPublicKey);
    }

}
