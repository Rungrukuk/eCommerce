package ecommerce.auth_service.repository;

import ecommerce.auth_service.dto.UserDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GuestUserRepository {

    
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<UserDTO> saveGuestUser(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Mono.empty();
        }
        String userId = UUID.randomUUID().toString();
        UserDTO guestUser = new UserDTO();
        guestUser.setRoleName(roleName);
        return redisTemplate.opsForValue().set(userId, roleName, Duration.ofHours(24))
                .then(Mono.just(guestUser))
                .onErrorResume(
                        e -> Mono.error(new RuntimeException("Failed to save guest user", e)));
    }

    public Mono<String> getGuestUserRole(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(userId)
                .onErrorResume(e -> Mono.error(
                        new RuntimeException("Failed to retrieve guest user role", e)));
    }

    public Mono<Boolean> deleteGuestUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Mono.just(true);
        }
        return redisTemplate.opsForValue().delete(userId)
                .onErrorResume(
                        e -> Mono.error(new RuntimeException("Failed to delete guest user", e)));
    }

    public Mono<Boolean> validateGuestUser(String userId, String roleName) {
        if (userId == null || roleName == null || userId.isEmpty() || roleName.isEmpty()) {
            return Mono.just(false);
        }
        return redisTemplate.opsForValue().get(userId)
                .map(savedRole -> savedRole.equals(roleName))
                .defaultIfEmpty(false)
                .onErrorResume(
                        e -> Mono.error(new RuntimeException("Failed to validate guest user", e)));
    }
}
