package ecommerce.user_service.dto;

import ecommerce.user_service.util.CustomResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private CustomResponseStatus responseStatus = CustomResponseStatus.UNEXPECTED_ERROR;
    private String message = "";
    private int statusCode = 500;

}
