package ecommerce.auth_service.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("roles")
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    private String roleName;
}
