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

                when(valueOperations.set(anyString(), eq(accessToken), eq(Duration.ofHours(24))))
                                .thenReturn(Mono.just(true));

                StepVerifier.create(sessionRepository.saveSession(accessToken))
                                .expectNextMatches(savedSession -> savedSession.getAccessToken().equals(accessToken)
                                                && savedSession.getSessionId() != null)
                                .verifyComplete();

                verify(valueOperations, times(1)).set(any(String.class), eq(accessToken), eq(Duration.ofHours(24)));
        }

        @Test
        void saveSession_Error() {
                String accessToken = "testAccessToken";

                when(valueOperations.set(any(String.class), eq(accessToken), eq(Duration.ofHours(24))))
                                .thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.saveSession(accessToken))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to save session"))
                                .verify();

                verify(valueOperations, times(1)).set(any(String.class), eq(accessToken), eq(Duration.ofHours(24)));
        }

        @Test
        void getSessionAccessToken_Success() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(sessionId)).thenReturn(Mono.just(accessToken));

                StepVerifier.create(sessionRepository.getSessionAccessToken(sessionId))
                                .expectNext(accessToken)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void getSessionAccessToken_Error() {
                String sessionId = "testSessionId";

                when(valueOperations.get(sessionId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.getSessionAccessToken(sessionId))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to retrieve session"))
                                .verify();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void getSessionAccessToken_InvalidSessionId() {
                StepVerifier.create(sessionRepository.getSessionAccessToken(null))
                                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                                                && throwable.getMessage().equals("Session ID must not be null"))
                                .verify();
        }

        @Test
        void deleteSession_Success() {
                String sessionId = "testSessionId";

                when(valueOperations.delete(sessionId)).thenReturn(Mono.just(true));

                StepVerifier.create(sessionRepository.deleteSession(sessionId))
                                .expectNext(true)
                                .verifyComplete();

                verify(valueOperations, times(1)).delete(sessionId);
        }

        @Test
        void deleteSession_Error() {
                String sessionId = "testSessionId";

                when(valueOperations.delete(sessionId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.deleteSession(sessionId))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to delete session"))
                                .verify();

                verify(valueOperations, times(1)).delete(sessionId);
        }

        @Test
        void deleteSession_InvalidSessionId() {
                StepVerifier.create(sessionRepository.deleteSession(null))
                                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                                                && throwable.getMessage().equals("Session ID must not be null"))
                                .verify();
        }

        @Test
        void validateSession_Success() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(sessionId)).thenReturn(Mono.just(accessToken));

                StepVerifier.create(sessionRepository.validateSession(sessionId, accessToken))
                                .expectNext(true)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void validateSession_InvalidToken() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";
                String wrongToken = "wrongAccessToken";

                when(valueOperations.get(sessionId)).thenReturn(Mono.just(wrongToken));

                StepVerifier.create(sessionRepository.validateSession(sessionId, accessToken))
                                .expectNext(false)
                                .verifyComplete();

                verify(valueOperations, times(1)).get(sessionId);
        }

        @Test
        void validateSession_Error() {
                String sessionId = "testSessionId";
                String accessToken = "testAccessToken";

                when(valueOperations.get(sessionId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

                StepVerifier.create(sessionRepository.validateSession(sessionId, accessToken))
                                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                                                && throwable.getMessage().equals("Failed to validate session"))
                                .verify();

                verify(valueOperations, times(1)).get(sessionId);
        }
}
