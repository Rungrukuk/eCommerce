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

class GuestUserRepositoryTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @InjectMocks
    private GuestUserRepository guestUserRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveGuestUser_Success() {
        String roleName = "guest";

        when(valueOperations.set(any(String.class), eq(roleName), eq(Duration.ofHours(24))))
                .thenReturn(Mono.just(true));

        StepVerifier.create(guestUserRepository.saveGuestUser(roleName))
                .expectNextMatches(savedUser -> savedUser.getRoleName().equals(roleName))
                .verifyComplete();

        verify(valueOperations, times(1)).set(any(String.class), eq(roleName), eq(Duration.ofHours(24)));
    }

    @Test
    void saveGuestUser_Error() {
        String roleName = "guest";

        when(valueOperations.set(any(String.class), eq(roleName), eq(Duration.ofHours(24))))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(guestUserRepository.saveGuestUser(roleName))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Failed to save guest user"))
                .verify();

        verify(valueOperations, times(1)).set(any(String.class), eq(roleName), eq(Duration.ofHours(24)));
    }

    @Test
    void getGuestUserRole_Success() {
        String userId = "testUserId";
        String roleName = "guest";

        when(valueOperations.get(userId)).thenReturn(Mono.just(roleName));

        StepVerifier.create(guestUserRepository.getGuestUserRole(userId))
                .expectNext(roleName)
                .verifyComplete();

        verify(valueOperations, times(1)).get(userId);
    }

    @Test
    void getGuestUserRole_Error() {
        String userId = "testUserId";

        when(valueOperations.get(userId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(guestUserRepository.getGuestUserRole(userId))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Failed to retrieve guest user role"))
                .verify();

        verify(valueOperations, times(1)).get(userId);
    }

    @Test
    void deleteGuestUser_Success() {
        String userId = "testUserId";

        when(valueOperations.delete(userId)).thenReturn(Mono.just(true));

        StepVerifier.create(guestUserRepository.deleteGuestUser(userId))
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations, times(1)).delete(userId);
    }

    @Test
    void deleteGuestUser_Error() {
        String userId = "testUserId";

        when(valueOperations.delete(userId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(guestUserRepository.deleteGuestUser(userId))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Failed to delete guest user"))
                .verify();

        verify(valueOperations, times(1)).delete(userId);
    }

    @Test
    void validateGuestUser_Success() {
        String userId = "testUserId";
        String roleName = "guest";

        when(valueOperations.get(userId)).thenReturn(Mono.just(roleName));

        StepVerifier.create(guestUserRepository.validateGuestUser(userId, roleName))
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations, times(1)).get(userId);
    }

    @Test
    void validateGuestUser_InvalidRole() {
        String userId = "testUserId";
        String roleName = "guest";
        String wrongRole = "admin";

        when(valueOperations.get(userId)).thenReturn(Mono.just(wrongRole));

        StepVerifier.create(guestUserRepository.validateGuestUser(userId, roleName))
                .expectNext(false)
                .verifyComplete();

        verify(valueOperations, times(1)).get(userId);
    }

    @Test
    void validateGuestUser_Error() {
        String userId = "testUserId";
        String roleName = "guest";

        when(valueOperations.get(userId)).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(guestUserRepository.validateGuestUser(userId, roleName))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Failed to validate guest user"))
                .verify();

        verify(valueOperations, times(1)).get(userId);
    }
}
