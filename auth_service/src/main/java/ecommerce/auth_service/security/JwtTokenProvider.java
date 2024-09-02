package ecommerce.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ecommerce.auth_service.dto.RoleDTO;
import ecommerce.auth_service.dto.UserDTO;

import javax.crypto.SecretKey;

import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;
    private final SecretKey refreshSecretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpiration;

    public JwtTokenProvider(@Value("${jwt.secret.key}") String jwtSecretKey, @Value("${jwt.refresh.key}") String refreshSecretKey) {
        System.out.println("jwt key: "+jwtSecretKey.length()); 
        System.out.println("refresh key: "+ refreshSecretKey.length()); 
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes()); 
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecretKey.getBytes()); 
    }

    public String createToken(String userId, RoleDTO roleDTO) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("role", roleDTO);
    
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtExpiration);
    
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String createRefreshToken(UserDTO user) {
        Claims claims = Jwts.claims().setSubject(user.getUserId());
        claims.put("role", user.getRole());
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtRefreshExpiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(refreshSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build();
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
