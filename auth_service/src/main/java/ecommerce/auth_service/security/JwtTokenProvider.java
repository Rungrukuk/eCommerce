package ecommerce.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ecommerce.auth_service.dto.RoleDTO;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final PrivateKey accessTokenPrivateKey;
    private final PublicKey accessTokenPublicKey;

    private final PrivateKey refreshTokenPrivateKey;
    private final PublicKey refreshTokenPublicKey;

    private final long accessTokenExpiration;

    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.access.private.key}") String accessPrivateKeyStr,
            @Value("${jwt.access.public.key}") String accessPublicKeyStr,
            @Value("${jwt.refresh.private.key}") String refreshPrivateKeyStr,
            @Value("${jwt.refresh.public.key}") String refreshPublicKeyStr,
            @Value("${jwt.access.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh.expiration}") long refreshTokenExpiration) throws Exception {
        this.accessTokenPrivateKey = getPrivateKeyFromString(accessPrivateKeyStr);
        this.accessTokenPublicKey = getPublicKeyFromString(accessPublicKeyStr);
        this.refreshTokenPrivateKey = getPrivateKeyFromString(refreshPrivateKeyStr);
        this.refreshTokenPublicKey = getPublicKeyFromString(refreshPublicKeyStr);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
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

    public String createAccessToken(String userId, RoleDTO roleDTO) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleDTO.getName());

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(accessTokenPrivateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    public String createRefreshToken(String userId, RoleDTO roleDTO) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleDTO.getName());

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

    private boolean validateToken(String token, PublicKey publicKey) {
        try {
            JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .setAllowedClockSkewSeconds(60)
                    .build();
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            // TODO handle the token expiration
            e.printStackTrace();
            return false;
        }
    }

    public Claims getClaims(String token, PublicKey publicKey) {
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build();
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public PublicKey getAccessTokenPublicKey() {
        return accessTokenPublicKey;
    }

    public PublicKey getRefreshTokenPublicKey() {
        return refreshTokenPublicKey;
    }
}
