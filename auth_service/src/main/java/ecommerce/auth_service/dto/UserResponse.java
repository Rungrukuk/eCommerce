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

    private String refreshToken;


    public UserResponse(String accessToken, String refreshToken, String sessionId, CustomResponseStatus responseStatus,
            List<String> errors) {
        super(accessToken, sessionId, responseStatus, errors);
        this.refreshToken = refreshToken;
    }
}
