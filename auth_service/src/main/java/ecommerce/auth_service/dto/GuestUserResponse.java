package ecommerce.auth_service.dto;

import ecommerce.auth_service.util.CustomResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GuestUserResponse extends BaseResponse {
    private List<String> errors;

    public GuestUserResponse(String accessToken, String sessionId,
            CustomResponseStatus responseStatus, List<String> errors) {
        super(accessToken, sessionId, responseStatus);
        this.errors = errors;
    }
}
