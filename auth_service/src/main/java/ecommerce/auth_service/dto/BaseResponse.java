package ecommerce.auth_service.dto;

import ecommerce.auth_service.util.CustomResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse {
    protected String accessToken;
    protected String sessionId;
    protected CustomResponseStatus responseStatus;
}
