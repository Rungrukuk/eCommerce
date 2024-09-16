package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Repository
public class SessionRepository {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    // TODO Consider making sessionID as key
    private String hashAccessToken(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing access token", e);
        }
    }

    public Mono<Session> saveSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(accessToken, sessionId);

        String key = hashAccessToken(accessToken);
        String hashedSessionId = passwordEncoder.encode(sessionId);

        return redisTemplate.opsForHash()
                .put(key, "sessionId", hashedSessionId)
                .then(redisTemplate.expire(key, Duration.ofHours(24)))
                .then(Mono.just(session))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to save session", e)));
    }

    public Mono<String> getSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }

        String key = hashAccessToken(accessToken);

        return redisTemplate.opsForHash()
                .get(key, "sessionId")
                .map(Object::toString)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to retrieve session", e)));
    }

    public Mono<Boolean> deleteByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.just(true);
        }

        String key = hashAccessToken(accessToken);

        return redisTemplate.opsForHash()
                .remove(key, "sessionId")
                .map(deletedCount -> deletedCount > 0)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete session", e)));
    }

    public Mono<Boolean> validateSession(String accessToken, String sessionId) {
        if (accessToken == null || sessionId == null || accessToken.isEmpty() || sessionId.isEmpty()) {
            return Mono.just(false);
        }

        String key = hashAccessToken(accessToken);

        return redisTemplate.opsForHash()
                .get(key, "sessionId")
                .map(storedSessionId -> passwordEncoder.matches(sessionId, storedSessionId.toString()))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate session", e)));
    }

    public Mono<Boolean> deleteBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Mono.just(true);
        }

        return redisTemplate.keys("session:*")
                .flatMap(key -> redisTemplate.opsForHash().get(key, "sessionId")
                        .filter(storedSessionId -> passwordEncoder.matches(sessionId, storedSessionId.toString()))
                        .flatMap(storedSessionId -> redisTemplate.opsForHash()
                                .remove(key, "sessionId")
                                .map(deletedCount -> deletedCount > 0)))
                .hasElements()
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete session by sessionId", e)));
    }

}
