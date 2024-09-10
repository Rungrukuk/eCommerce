package ecommerce.auth_service.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserResponse extends BaseResponse {

    public UserResponse(String accessToken, String refreshToken, String sessionId, List<String> errors) {
        super(accessToken, refreshToken, sessionId, errors);
    }
}
