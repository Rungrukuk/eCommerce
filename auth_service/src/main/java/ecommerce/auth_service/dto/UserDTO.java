package ecommerce.auth_service.dto;

import ecommerce.auth_service.domain.Role;
import lombok.Data;

@Data
public class UserDTO {
    private String userId;
    private String email;
    private Role role;
}
