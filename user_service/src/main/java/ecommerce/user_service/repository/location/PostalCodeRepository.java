package ecommerce.user_service.repository.location;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.location.PostalCode;
import reactor.core.publisher.Mono;

public interface PostalCodeRepository extends ReactiveCrudRepository<PostalCode, Integer> {

    Mono<PostalCode> findByPostalCodeAndCityIdAndStateId(String postalCode, Integer cityId, Integer stateId);

    Mono<PostalCode> findByPostalCodeAndStateId(String postalCode, Integer stateId);

    Mono<PostalCode> findByPostalCode(String postalCode);

    Mono<PostalCode> findByPostalCodeAndCityId(String postalCode, Integer id);
}
