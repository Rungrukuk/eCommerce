package ecommerce.user_service.service;

import ecommerce.user_service.util.EventType;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MonitoringClient {
    Mono<Void> sendEvent(
            EventType eventType,
            String serviceName,
            String userId,
            String userAgent,
            String clientCity,
            String details,
            Map<String, String> metadata);
}
