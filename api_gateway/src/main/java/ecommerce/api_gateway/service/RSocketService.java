package ecommerce.api_gateway.service;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RSocketService {

    private final RSocketRequester.Builder rSocketRequesterBuilder;
    private RSocketRequester rSocketRequester;

    public RSocketService(Builder rSocketRequesterBuilder) {
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @PostConstruct
    public void init() {
        rSocketRequester = rSocketRequesterBuilder.tcp("localhost", 7000);
    }

    public RSocketRequester getRSocketRequester() {
        return rSocketRequester;
    }
}
