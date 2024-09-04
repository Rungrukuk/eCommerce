package ecommerce.auth_service.dto;

import lombok.Data;

@Data
public class UserCreateDTO {
    private String email;
    private String password;
    private String rePassword;
}
