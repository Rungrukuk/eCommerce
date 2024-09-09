package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class SessionServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, Session> redisTemplate;

    @InjectMocks
    private SessionService sessionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSaveSessionSuccess() {
        Session session = new Session("sessionId", "accessToken");
        when(redisTemplate.opsForValue().set(anyString(), any(Session.class), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(sessionService.saveSession(session))
                .expectNext(session)
                .verifyComplete();
    }

    @Test
    public void testSaveSessionFailure() {
        Session session = new Session("sessionId", "accessToken");
        when(redisTemplate.opsForValue().set(anyString(), any(Session.class), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(sessionService.saveSession(session))
                .expectErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Failed to save session"))
                .verify();
    }

    @Test
    public void testGetSessionSuccess() {
        Session session = new Session("sessionId", "accessToken");
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.just(session));

        StepVerifier.create(sessionService.getSession("sessionId"))
                .expectNext(session)
                .verifyComplete();
    }

    @Test
    public void testGetSessionNotFound() {
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sessionService.getSession("sessionId"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    public void testGetSessionFailure() {
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(sessionService.getSession("sessionId"))
                .expectErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Failed to retrieve session"))
                .verify();
    }

    @Test
    public void testDeleteSessionSuccess() {
        when(redisTemplate.opsForValue().delete(anyString()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(sessionService.deleteSession("sessionId"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testDeleteSessionFailure() {
        when(redisTemplate.opsForValue().delete(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(sessionService.deleteSession("sessionId"))
                .expectErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Failed to delete session"))
                .verify();
    }

    @Test
    public void testValidateSessionSuccess() {
        Session session = new Session("sessionId", "accessToken");
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.just(session));

        StepVerifier.create(sessionService.validateSession("sessionId", "accessToken"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testValidateTokenFailure() {
        Session session = new Session("sessionId", "accessToken");
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.just(session));

        StepVerifier.create(sessionService.validateSession("sessionId", "wrongToken"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void testValidateSessionNotFound() {
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sessionService.validateSession("sessionId", "accessToken"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void testValidateSessionFailure() {
        when(redisTemplate.opsForValue().get(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(sessionService.validateSession("sessionId", "accessToken"))
                .expectErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Failed to validate session"))
                .verify();
    }
}
