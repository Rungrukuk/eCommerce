package ecommerce.api_gateway.service;

import ecommerce.api_gateway.config.ServiceConfigProperties;
import ecommerce.api_gateway.util.Services;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RSocketService {

    private final RSocketRequester.Builder requesterBuilder;
    private final ServiceConfigProperties serviceConfigs;

    private final Map<Services, RSocketRequester> requesterMap = new ConcurrentHashMap<>();

    @Value("${spring.rsocket.client.ssl.api-key}")
    private String apiKey;

    @Value("${spring.rsocket.client.ssl.api-cert}")
    private String apiCert;

    @Value("${spring.rsocket.client.ssl.trust-cert}")
    private String trustCert;

    @PostConstruct
    @SneakyThrows
    public void init() {
        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(new File(apiCert), new File(apiKey))
                .trustManager(new File(trustCert))
                .build();

        for (Map.Entry<String, ServiceConfigProperties.ServiceEndpoint> entry : serviceConfigs.getEndpoints()
                .entrySet()) {

            Services service = Services.valueOf(entry.getKey());
            ServiceConfigProperties.ServiceEndpoint endpoint = entry.getValue();

            TcpClient tcpClient = TcpClient.create()
                    .host(endpoint.getHost())
                    .port(endpoint.getPort())
                    .secure(ssl -> ssl.sslContext(sslContext));

            RSocketRequester requester = requesterBuilder
                    .rsocketConnector(connector -> connector.reconnect(
                            Retry.fixedDelay(5, Duration.ofSeconds(2))))
                    .transport(TcpClientTransport.create(tcpClient));

            requesterMap.put(service, requester);
        }
    }

    public RSocketRequester getRSocketRequester(Services service) {
        RSocketRequester requester = requesterMap.get(service);
        if (requester == null) {
            throw new IllegalArgumentException(
                    "No RSocket requester configured for service: " + service);
        }
        return requester;
    }
}
