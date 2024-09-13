package ecommerce.auth_service.domain;

import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("role_permissions")
public class RolePermission {
    private String roleName;
    private Long permissionId;
}
