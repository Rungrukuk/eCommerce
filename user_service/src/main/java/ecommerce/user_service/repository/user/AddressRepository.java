package ecommerce.user_service.repository.user;

import ecommerce.user_service.domain.user.Address;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AddressRepository extends ReactiveCrudRepository<Address, Long> {

}
