package ecommerce.api_gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;

import ecommerce.api_gateway.util.Services;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.SneakyThrows;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;

import java.util.Map;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;

@Service
public class RSocketService {

    @Value("${spring.rsocket.client.ssl.api-key}")
    private String apiKey;

    @Value("${spring.rsocket.client.ssl.api-cert}")
    private String apiCert;

    @Value("${spring.rsocket.client.ssl.trust-cert}")
    private String trustCert;

    private final RSocketRequester.Builder requesterBuilder;
    private final Map<Services, String> serviceHostMap = new HashMap<>();
    private final Map<Services, Integer> servicePortMap = new HashMap<>();

    public RSocketService(RSocketRequester.Builder requesterBuilder) {
        this.requesterBuilder = requesterBuilder;

        serviceHostMap.put(Services.AUTH_SERVICE, "localhost");
        servicePortMap.put(Services.AUTH_SERVICE, 7000);

        serviceHostMap.put(Services.USER_SERVICE, "localhost");
        servicePortMap.put(Services.USER_SERVICE, 7001);

    }

    @SneakyThrows
    public TcpClientTransport getTcpClientTransport(String host, Integer port) {

        SslContext sslContext = SslContextBuilder.forClient().keyManager(new File(apiCert), new File(apiKey))
                .trustManager(new File(trustCert))
                .build();
        SslProvider sslProvider = SslProvider.builder().sslContext(sslContext).build();
        TcpClient tcpClient = TcpClient.create().host(host).port(port).secure(sslProvider);
        return TcpClientTransport.create(tcpClient);
    }

    @SneakyThrows
    private KeyStore getKeystore(String filename, String password) {
        InputStream is = ClassLoader.getSystemResourceAsStream(filename);
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(is, password.toCharArray());
        return keystore;

    }

    public RSocketRequester getRSocketRequester(Services service) {
        String host = serviceHostMap.get(service);
        Integer port = servicePortMap.get(service);
        TcpClientTransport transport = getTcpClientTransport(host, port);
        return requesterBuilder.transport(transport);
    }
}
