package ecommerce.auth_service.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RedisHash("Session")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session implements Serializable {
    @Id
    private String accessToken;
    private String sessionId;
}
