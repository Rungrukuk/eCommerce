package ecommerce.user_service.domain.location;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("postal_codes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostalCode {

    @Id
    private Integer id;
    private Integer countryId;
    private String postalCode;
    private Integer cityId;
    private Integer stateId;

}