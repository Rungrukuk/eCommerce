package ecommerce.auth_service.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String userId;
    private String email;
    private String password;
    private String roleName;
}
