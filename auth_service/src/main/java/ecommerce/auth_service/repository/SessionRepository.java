package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Repository
public class SessionRepository {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<Session> saveSession(String accessToken) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, accessToken);
        return redisTemplate.opsForValue().set(sessionId, accessToken, Duration.ofHours(24))
                .then(Mono.just(session))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to save session", e)));
    }

    public Mono<String> getSessionAccessToken(String sessionId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("Session ID must not be null"));
        }
        return redisTemplate.opsForValue().get(sessionId)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to retrieve session", e)));
    }

    public Mono<Boolean> deleteSession(String sessionId) {
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("Session ID must not be null"));
        }
        return redisTemplate.opsForValue().delete(sessionId)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete session", e)));
    }

    public Mono<Boolean> validateSession(String sessionId, String accessToken) {
        return redisTemplate.opsForValue().get(sessionId)
                .map(savedAccessToken -> accessToken.equals(savedAccessToken))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate session", e)));
    }
}
