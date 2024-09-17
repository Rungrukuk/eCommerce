package ecommerce.auth_service.dto;

import java.util.List;

import ecommerce.auth_service.util.CustomResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserResponse extends BaseResponse {

    private String email;
    private String refreshToken;
    private List<String> errors;

    public UserResponse(String email, String accessToken, String sessionId, String refreshToken,
            CustomResponseStatus responseStatus,
            List<String> errors) {
        super(accessToken, sessionId, responseStatus);
        this.email = email;
        this.refreshToken = refreshToken;
        this.errors = errors;
    }
}
