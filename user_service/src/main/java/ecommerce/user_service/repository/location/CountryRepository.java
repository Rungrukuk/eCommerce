package ecommerce.user_service.repository.location;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import ecommerce.user_service.domain.location.Country;
import reactor.core.publisher.Mono;

public interface CountryRepository extends ReactiveCrudRepository<Country, Integer> {
    Mono<Country> findByIso3(String iso3);
}
