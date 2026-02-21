package ecommerce.user_service.service.implementation;

import ecommerce.user_service.ProtoMonitoringEvent;
import ecommerce.user_service.service.MonitoringClient;
import ecommerce.user_service.util.EventType;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringClientImpl implements MonitoringClient {

    @Value("${services.monitoring.host:localhost}")
    private String monitoringHost;

    @Value("${services.monitoring.port:7002}")
    private int monitoringPort;

    @Value("${spring.rsocket.client.ssl.user-key}")
    private String userKey;

    @Value("${spring.rsocket.client.ssl.user-cert}")
    private String userCert;

    @Value("${spring.rsocket.client.ssl.trust-cert}")
    private String trustCert;

    private final RSocketRequester.Builder requesterBuilder;
    private volatile RSocketRequester requester;

    private Mono<RSocketRequester> initRequesterReactive() {

        if (requester != null) {
            return Mono.just(requester);
        }

        return Mono.fromCallable(() -> {

                    SslContext sslContext = SslContextBuilder.forClient()
                            .keyManager(new File(userCert), new File(userKey))
                            .trustManager(new File(trustCert))
                            .build();

                    TcpClient tcpClient = TcpClient.create()
                            .host(monitoringHost)
                            .port(monitoringPort)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                            .secure(ssl -> ssl.sslContext(sslContext));

                    return requesterBuilder
                            .rsocketConnector(connector -> connector
                                    .reconnect(Retry.fixedDelay(5, Duration.ofSeconds(2))))
                            .transport(TcpClientTransport.create(tcpClient));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(req -> requester = req)
                .onErrorResume(e -> {
                    log.error("MonitoringClient initialization failed", e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> sendEvent(
            EventType eventType,
            String serviceName,
            String userId,
            String userAgent,
            String clientCity,
            String details,
            Map<String, String> metadata) {

        Mono<Void> pipeline = initRequesterReactive()
                .flatMap(req -> {
                    if (req == null) {
                        return Mono.empty();
                    }
                    ProtoMonitoringEvent.Builder builder = ProtoMonitoringEvent.newBuilder()
                            .setEventType(eventType.name())
                            .setServiceName(serviceName)
                            .setUserId(userId != null ? userId : "")
                            .setUserAgent(userAgent != null ? userAgent : "")
                            .setClientCity(clientCity != null ? clientCity : "")
                            .setDetails(details != null ? details : "")
                            .setTimestamp(Instant.now().toString());

                    if (metadata != null && !metadata.isEmpty()) {
                        builder.putAllMetadata(metadata);
                    }

                    return req.route("monitoring.logEvent")
                            .data(builder.build())
                            .send();
                })
                .onErrorResume(e -> {
                    log.warn("Monitoring event dropped: {}", e.getMessage());
                    return Mono.empty();
                });
        // Subscribing with worker thread without interfering the main flow of the
        // request
        pipeline.subscribeOn(Schedulers.boundedElastic()).subscribe();
        return Mono.empty();
    }

}
