package ecommerce.auth_service.repository;

import ecommerce.auth_service.domain.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;

class SessionRepositoryTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveSession_success() {
        String accessToken = "mockAccessToken";
        String sessionId = "mockSessionId";

        when(redisTemplate.opsForValue().set(sessionId, anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        Mono<Session> result = sessionRepository.saveSession(accessToken);

        StepVerifier.create(result)
                .expectNextMatches(
                        session -> session.getAccessToken().equals(accessToken)
                                && session.getSessionId().equals(sessionId))
                .verifyComplete();

        verify(redisTemplate).opsForValue().set(anyString(), eq(accessToken), any(Duration.class));
    }

    @Test
    void getSessionAccessToken_success() {
        String sessionId = "mockSessionId";
        String accessToken = "mockAccessToken";

        when(redisTemplate.opsForValue().get(sessionId)).thenReturn(Mono.just(accessToken));

        Mono<String> result = sessionRepository.getSessionAccessToken(sessionId);

        StepVerifier.create(result)
                .expectNext(accessToken)
                .verifyComplete();

        verify(redisTemplate).opsForValue().get(sessionId);
    }

    @Test
    void getSessionAccessToken_nullSessionId() {
        Mono<String> result = sessionRepository.getSessionAccessToken(null);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deleteSession_success() {
        String sessionId = "mockSessionId";

        when(redisTemplate.opsForValue().delete(sessionId)).thenReturn(Mono.just(true));

        Mono<Boolean> result = sessionRepository.deleteSession(sessionId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).opsForValue().delete(sessionId);
    }

    @Test
    void deleteSession_nullSessionId() {
        Mono<Boolean> result = sessionRepository.deleteSession(null);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void validateSession_success() {
        String sessionId = "mockSessionId";
        String accessToken = "mockAccessToken";

        when(redisTemplate.opsForValue().get(sessionId)).thenReturn(Mono.just(accessToken));

        Mono<Boolean> result = sessionRepository.validateSession(sessionId, accessToken);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).opsForValue().get(sessionId);
    }

    @Test
    void validateSession_invalidToken() {
        String sessionId = "mockSessionId";
        String accessToken = "mockAccessToken";
        String savedToken = "anotherToken";

        when(redisTemplate.opsForValue().get(sessionId)).thenReturn(Mono.just(savedToken));

        Mono<Boolean> result = sessionRepository.validateSession(sessionId, accessToken);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateSession_nullSessionId() {
        Mono<Boolean> result = sessionRepository.validateSession(null, "mockAccessToken");

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

}
