package ecommerce.auth_service.controller;

import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import ecommerce.auth_service.CreateUserRequest;
import ecommerce.auth_service.CreateUserResponse;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

@SpringBootTest
@SpringJUnitConfig
public class ProtobufRSocketTest {

        @Configuration
        static class TestConfig {
                @Bean
                public RSocketStrategies rSocketStrategies() {
                        return RSocketStrategies.builder()
                                        .encoder(new ProtobufEncoder())
                                        .decoder(new ProtobufDecoder())
                                        .build();
                }

                @Bean
                public RSocketRequester.Builder rSocketRequesterBuilder(RSocketStrategies strategies) {
                        return RSocketRequester.builder().rsocketStrategies(strategies);
                }
        }

        @Autowired
        private RSocketRequester.Builder rSocketRequesterBuilder;

        @Value("${spring.rsocket.client.ssl.trust-store}")
        private String trustStorePath;

        @Value("${spring.rsocket.client.ssl.trust-store-password}")
        private String trustStorePassword;

        @Test
        public void testProtobufEncodingDecoding() throws IOException {
                SslContext sslContext = SslContextBuilder.forClient()
                                .sslProvider(SslProvider.JDK)
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build();

                TcpClient tcpClient = TcpClient.create()
                                .secure(ssl -> ssl.sslContext(sslContext))
                                .host("localhost")
                                .port(7000);

                RSocketRequester rSocketRequester = rSocketRequesterBuilder
                                .transport(TcpClientTransport.create(tcpClient));

                CreateUserRequest request = CreateUserRequest.newBuilder()
                                .setEmail("test@example.com")
                                .setPassword("password123")
                                .setRePassword("password123")
                                .setAccessToken("valid_token")
                                .build();

                Mono<CreateUserResponse> responseMono = rSocketRequester
                                .route("auth.registerUser")
                                .data(request)
                                .retrieveMono(CreateUserResponse.class);

                CreateUserResponse response = responseMono.block();

                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(403);
                assertThat(response.getBody()).isEqualTo("Access Denied! Unauthorized Access");
                assertThat(response.getAccessToken()).isEmpty();
                assertThat(response.getRefreshToken()).isEmpty();
        }
}
