package ecommerce.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestUserDTO {
    private String userId;
    private RoleDTO role;
}
