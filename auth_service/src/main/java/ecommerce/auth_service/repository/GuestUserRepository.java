package ecommerce.auth_service.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.dto.UserDTO;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
// TODO Instead of storing role, store user agent data
public class GuestUserRepository {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    public Mono<UserDTO> saveGuestUser(String roleName) {
        String userId = UUID.randomUUID().toString();
        UserDTO guestUser = new UserDTO();
        guestUser.setRoleName(roleName);
        return redisTemplate.opsForValue().set(userId, roleName, Duration.ofHours(24)).then(Mono.just(guestUser))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to save guest user", e)));
    }

    public Mono<String> getGuestUserRole(String userId) {
        return redisTemplate.opsForValue().get(userId)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to retrieve guest user role", e)));
    }

    public Mono<Boolean> deleteGuestUser(String userId) {
        return redisTemplate.opsForValue().delete(userId)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete guest user", e)));
    }

    public Mono<Boolean> validateGuestUser(String userId, String roleName) {
        return redisTemplate.opsForValue().get(userId)
                .map(savedRole -> savedRole.equals(roleName))
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate guest user", e)));
    }
}
