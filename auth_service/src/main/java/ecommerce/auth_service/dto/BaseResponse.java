package ecommerce.auth_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse {
    protected String accessToken;
    protected String refreshToken;
    protected String sessionId;
    protected List<String> errors;
}
