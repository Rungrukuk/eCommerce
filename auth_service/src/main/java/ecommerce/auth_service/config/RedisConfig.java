package ecommerce.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import ecommerce.auth_service.dto.UserDTO;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisOperations<String, UserDTO> redisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<UserDTO> serializer = new Jackson2JsonRedisSerializer<>(UserDTO.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserDTO> builder = RedisSerializationContext
                .newSerializationContext(serializer);

        RedisSerializationContext<String, UserDTO> context = builder.build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
