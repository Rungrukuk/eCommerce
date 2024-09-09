package ecommerce.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import ecommerce.auth_service.domain.Session;
import ecommerce.auth_service.dto.UserDTO;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Session> sessionRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Session> serializer = new Jackson2JsonRedisSerializer<>(Session.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Session> builder = RedisSerializationContext
                .newSerializationContext(serializer);

        RedisSerializationContext<String, Session> context = builder
                .key(new StringRedisSerializer())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, UserDTO> userRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<UserDTO> serializer = new Jackson2JsonRedisSerializer<>(UserDTO.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, UserDTO> builder = RedisSerializationContext
                .newSerializationContext(serializer);

        RedisSerializationContext<String, UserDTO> context = builder
                .key(new StringRedisSerializer())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
