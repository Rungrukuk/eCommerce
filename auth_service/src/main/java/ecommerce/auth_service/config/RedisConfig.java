package ecommerce.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

        @Bean
        public ReactiveRedisTemplate<String, String> redisTemplate(ReactiveRedisConnectionFactory factory) {
                StringRedisSerializer stringSerializer = new StringRedisSerializer();
                RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder = RedisSerializationContext
                                .newSerializationContext(stringSerializer);
                RedisSerializationContext<String, String> context = builder
                                .key(stringSerializer)
                                .value(stringSerializer)
                                .build();

                return new ReactiveRedisTemplate<>(factory, context);
        }
}
