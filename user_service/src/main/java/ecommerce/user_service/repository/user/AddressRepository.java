package ecommerce.user_service.repository.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.user.Address;

public interface AddressRepository extends ReactiveCrudRepository<Address, String> {

}
