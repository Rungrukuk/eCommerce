package ecommerce.api_gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;

import java.io.File;
import java.io.IOException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.netty.client.TcpClientTransport;

@Service
public class RSocketService {

    private final RSocketRequester.Builder rSocketRequesterBuilder;
    private RSocketRequester rSocketRequester;

    @Value("${spring.rsocket.client.ssl.key-cert}")
    private String keyCert;

    @Value("${spring.rsocket.client.ssl.trust-cert}")
    private String trustCert;

    @Value("${spring.rsocket.client.ssl.key-store}")
    private String key;

    @Value("${rsocket.remote.port}")
    private Integer rSocketPort;

    @Value("${rsocket.remote.host}")
    private String rSocketHost;

    public RSocketService(Builder rSocketRequesterBuilder) {
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @PostConstruct
    public void init() throws IOException {
        rSocketRequester = rSocketRequesterBuilder
                .transport(getTcpClientTransport());
    }

    @SneakyThrows
    private TcpClientTransport getTcpClientTransport() throws IOException {

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(new File(keyCert), new File(key))
                .trustManager(new File(trustCert))
                .build();
        SslProvider sslProvider = SslProvider.builder().sslContext(sslContext).build();
        TcpClient tcpClient = TcpClient.create().host(rSocketHost).port(rSocketPort).secure(sslProvider);
        return TcpClientTransport.create(tcpClient);
    }

    public RSocketRequester getRSocketRequester() {
        return rSocketRequester;
    }
}
