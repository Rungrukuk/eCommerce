package ecommerce.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateDTO {
    private String email;
    private String password;
    private String rePassword;
}
