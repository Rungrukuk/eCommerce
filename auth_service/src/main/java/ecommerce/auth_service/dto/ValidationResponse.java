package ecommerce.auth_service.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationResponse extends BaseResponse {
    private String serviceToken;
    private String body;
    private String statusCode;

    public ValidationResponse(String accessToken, String refreshToken, String sessionId, List<String> errors,
            String serviceToken) {
        super(accessToken, refreshToken, sessionId, errors);
        this.serviceToken = serviceToken;
    }
}
