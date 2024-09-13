package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Repository
// TODO encrypt the data before saving it in Redis
public class SessionRepository {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<Session> saveSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(accessToken, sessionId);
        return redisTemplate.opsForValue().set(accessToken, sessionId, Duration.ofHours(24))
                .then(Mono.just(session))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to save session", e)));
    }

    public Mono<String> getSession(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(accessToken)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to retrieve session", e)));
    }

    public Mono<Boolean> deleteByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.just(true);
        }
        return redisTemplate.opsForValue().delete(accessToken)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete session", e)));
    }

    public Mono<Boolean> validateSession(String accessToken, String sessionId) {
        if (accessToken == null || sessionId == null || accessToken.isEmpty() || sessionId.isEmpty()) {
            return Mono.just(false);
        }
        return redisTemplate.opsForValue().get(accessToken)
                .map(savedSession -> sessionId.equals(savedSession))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate session", e)));
    }

    public Mono<Boolean> deleteBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Mono.just(true);
        }
        return redisTemplate.keys("*")
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .filter(value -> sessionId.equals(value))
                        .flatMap(value -> redisTemplate.opsForValue().delete(key).then(Mono.just(true)))
                        .switchIfEmpty(Mono.empty()))
                .hasElements()
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete session by sessionId", e)));
    }

}
