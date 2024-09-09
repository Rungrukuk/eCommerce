package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class SessionService {

    @Autowired
    private ReactiveRedisTemplate<String, Session> redisTemplate;

    // Updated to return Mono<Session> instead of Mono<Void>
    public Mono<Session> saveSession(Session session) {
        if (session == null || session.getSessionId() == null) {
            return Mono.error(new IllegalArgumentException("Session or Session ID must not be null"));
        }
        return redisTemplate.opsForValue().set(session.getSessionId(), session, Duration.ofHours(24))
                .then(Mono.just(session)) // Return the saved session after successful save
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to save session", e)));
    }

    public Mono<Session> getSession(String sessionId) {
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
                .map(session -> session.getAccessToken().equals(accessToken))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate session", e)));
    }
}
