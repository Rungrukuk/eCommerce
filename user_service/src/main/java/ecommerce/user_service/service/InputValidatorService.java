package ecommerce.user_service.service;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

public interface InputValidatorService {
    public Mono<List<String>> validateInput(Map<String, String> input);
}
