package ecommerce.auth_service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestUser {
    private String userId;
    private Role role;
}
