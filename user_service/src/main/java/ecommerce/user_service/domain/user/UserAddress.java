package ecommerce.user_service.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_addresses")
public class UserAddress {
    @Id
    private String id;
    private String userId;
    private Long addressId;
    private boolean isDefault;
}