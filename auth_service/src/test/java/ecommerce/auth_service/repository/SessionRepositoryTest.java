package ecommerce.auth_service.repository;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SessionRepositoryTest {

        @Mock
        private ReactiveRedisTemplate<String, String> redisTemplate;

        @Mock
        private ReactiveValueOperations<String, String> valueOperations;

        @InjectMocks
        private SessionRepository sessionRepository;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        void saveSession_Success() {
                String accessToken = "testAccessToken";

                when(valueOperations.set(eq(accessToken), anyString(), eq(Duration.ofHours(24))))
                                .thenReturn(Mono.just(true));

                StepVerifier.create(sessionRepository.saveSession(accessToken))
                                .expectNextMatches(savedSession -> savedSession.getAccessToken().equals(accessToken)
                                                && savedSession.getSessionId() != null)
                                .verifyComplete();

                verify(valueOperations, times(1)).set(eq(accessToken), any(String.class), eq(Duration.ofHours(24)));
        }

        @Test
        void saveSession_Error() {
                String accessToken = "testAccessToken";

                when(valueOperations.set(eq(accessToken), any(String.class), eq(Duration.ofHours(24))))
                                .thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.saveSession(accessToken))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to save session"))
                                .verify();

                verify(valueOperations, times(1)).set(eq(accessToken), any(String.class), eq(Duration.ofHours(24)));
        }

        @Test
        void getSessionAccessToken_Success() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(sessionId)).thenReturn(Mono.just(accessToken));

                StepVerifier.create(sessionRepository.getSession(sessionId))
                                .expectNext(accessToken)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void getSessionAccessToken_Error() {
                String sessionId = "testSessionId";

                when(valueOperations.get(sessionId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.getSession(sessionId))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to retrieve session"))
                                .verify();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void getSession_InvalidSessionId() {
                StepVerifier.create(sessionRepository.getSession(null))
                                .expectComplete()
                                .verify();
        }

        @Test
        void deleteSession_Success() {
                String sessionId = "testSessionId";

                when(valueOperations.delete(sessionId)).thenReturn(Mono.just(true));

                StepVerifier.create(sessionRepository.deleteByAccessToken(sessionId))
                                .expectNext(true)
                                .verifyComplete();

                verify(valueOperations, times(1)).delete(sessionId);
        }

        @Test
        void deleteSession_Error() {
                String sessionId = "testSessionId";

                when(valueOperations.delete(sessionId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.deleteByAccessToken(sessionId))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to delete session"))
                                .verify();

                verify(valueOperations, times(1)).delete(sessionId);
        }

        @Test
        void deleteSession_InvalidSessionId() {
                StepVerifier.create(sessionRepository.deleteByAccessToken(null))
                                .expectNext(true)
                                .verifyComplete();
        }

        @Test
        void validateSession_Success() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(accessToken)).thenReturn(Mono.just(sessionId));

                StepVerifier.create(sessionRepository.validateSession(accessToken, sessionId))
                                .expectNext(true)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(accessToken);
        }

        @Test
        void validateSession_InvalidToken() {
                String sessionId = "testSessionId";
                String wrongSessionId = "wrongSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(accessToken)).thenReturn(Mono.just(wrongSessionId));

                StepVerifier.create(sessionRepository.validateSession(accessToken, sessionId))
                                .expectNext(false)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(accessToken);
        }

        @Test
        void validateSession_Error() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(accessToken)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.validateSession(accessToken, sessionId))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to validate session"))
                                .verify();

                verify(valueOperations, times(1)).get(accessToken);
        }
}
