package ecommerce.auth_service.dto;

import java.util.List;

import ecommerce.auth_service.util.CustomResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthResponse extends BaseResponse {
    private String serviceToken;
    private String refreshToken;

    public AuthResponse(String accessToken, String refreshToken, String sessionId,
            CustomResponseStatus responseStatus, List<String> errors, String serviceToken, String roleName) {
        super(accessToken, sessionId, responseStatus, errors);
        this.serviceToken = serviceToken;
        this.refreshToken = refreshToken;
    }
}
