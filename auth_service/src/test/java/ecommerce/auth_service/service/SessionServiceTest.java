package ecommerce.auth_service.service;

import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.repository.SessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SessionServiceTest {

        @Mock
        private ReactiveRedisTemplate<String, Session> redisTemplate;

        @Mock
        private ReactiveValueOperations<String, Session> valueOps;

        @InjectMocks
        private SessionRepository sessionService;

        private Session session;

        @BeforeEach
        public void setUp() {
                MockitoAnnotations.openMocks(this);
                session = new Session();
                session.setSessionId("test-session-id");
                session.setAccessToken("test-access-token");
                when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        public void saveSession_success() {
                when(valueOps.set(any(String.class), any(Session.class), any(Duration.class)))
                                .thenReturn(Mono.just(true));

                StepVerifier.create(sessionService.saveSession(session))
                                .expectNext(session)
                                .verifyComplete();

                verify(valueOps, times(1)).set(session.getSessionId(), session, Duration.ofHours(24));
        }

        @Test
        public void saveSession_nullSession_shouldThrowException() {
                StepVerifier.create(sessionService.saveSession(null))
                                .expectError(IllegalArgumentException.class)
                                .verify();
        }

        @Test
        public void saveSession_nullSessionId_shouldThrowException() {
                session.setSessionId(null);

                StepVerifier.create(sessionService.saveSession(session))
                                .expectError(IllegalArgumentException.class)
                                .verify();
        }

        @Test
        public void getSession_success() {
                when(valueOps.get("test-session-id")).thenReturn(Mono.just(session));

                StepVerifier.create(sessionService.getSession("test-session-id"))
                                .expectNext(session)
                                .verifyComplete();

                verify(valueOps, times(1)).get("test-session-id");
        }

        @Test
        public void getSession_sessionNotFound_shouldReturnEmpty() {
                when(valueOps.get("test-session-id")).thenReturn(Mono.empty());

                StepVerifier.create(sessionService.getSession("test-session-id"))
                                .expectComplete()
                                .verify();
        }

        @Test
        public void deleteSession_success() {
                when(valueOps.delete("test-session-id")).thenReturn(Mono.just(true));

                StepVerifier.create(sessionService.deleteSession("test-session-id"))
                                .expectNext(true)
                                .verifyComplete();

                verify(valueOps, times(1)).delete("test-session-id");
        }

        @Test
        public void validateSession_success() {
                when(valueOps.get("test-session-id")).thenReturn(Mono.just(session));

                StepVerifier.create(sessionService.validateSession("test-session-id", "test-access-token"))
                                .expectNext(true)
                                .verifyComplete();
        }

        @Test
        public void validateSession_invalidAccessToken_shouldReturnFalse() {
                when(valueOps.get("test-session-id")).thenReturn(Mono.just(session));

                StepVerifier.create(sessionService.validateSession("test-session-id", "wrong-access-token"))
                                .expectNext(false)
                                .verifyComplete();
        }

        @Test
        public void validateSession_sessionNotFound_shouldReturnFalse() {
                when(valueOps.get("test-session-id")).thenReturn(Mono.empty());

                StepVerifier.create(sessionService.validateSession("test-session-id", "test-access-token"))
                                .expectNext(false)
                                .verifyComplete();
        }
}
