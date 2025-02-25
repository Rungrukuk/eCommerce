package ecommerce.user_service.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("addresses")
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    private Long id;
    private String country;
    private String state;
    private String city;
    private String postalCode;
    private String addressLine_1;
    private String addressLine_2;
}
