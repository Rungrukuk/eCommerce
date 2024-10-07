package ecommerce.user_service.repository.location;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.location.City;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CityRepository extends ReactiveCrudRepository<City, Integer> {
    Flux<City> findByStateId(Integer stateId);

    Mono<City> findByNameAndStateId(String city, Integer id);

    Mono<City> findByName(String city);
}
