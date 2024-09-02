package ecommerce.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import ecommerce.auth_service.domain.GuestUser;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisOperations<String, GuestUser> redisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<GuestUser> serializer = new Jackson2JsonRedisSerializer<>(GuestUser.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, GuestUser> builder =
                RedisSerializationContext.newSerializationContext(serializer);

        RedisSerializationContext<String, GuestUser> context = builder.build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
