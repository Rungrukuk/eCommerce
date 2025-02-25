package ecommerce.user_service.repository.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.user.UserAddress;

public interface UserAddressRepository extends ReactiveCrudRepository<UserAddress, String> {

}
