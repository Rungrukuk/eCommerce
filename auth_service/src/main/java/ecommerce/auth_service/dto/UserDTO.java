package ecommerce.auth_service.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String userId;
    private String email;
    private String roleName;
}
