package ecommerce.user_service.domain.location;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("states")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class State {

    @Id
    private Integer id;
    private Integer countryId;
    private String name;
    private String stateCode;
}
