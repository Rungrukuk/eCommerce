package ecommerce.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ecommerce.auth_service.security.JwtTokenProvider;

@Configuration
public class JwtTokenProviderConfig {

    @Value("${jwt.access.private.key}")
    private String accessPrivateKeyStr;

    @Value("${jwt.access.public.key}")
    private String accessPublicKeyStr;

    @Value("${jwt.refresh.private.key}")
    private String refreshPrivateKeyStr;

    @Value("${jwt.refresh.public.key}")
    private String refreshPublicKeyStr;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    @Bean
    public JwtTokenProvider jwtTokenProvider() throws Exception {
        return new JwtTokenProvider(
                accessPrivateKeyStr,
                accessPublicKeyStr,
                refreshPrivateKeyStr,
                refreshPublicKeyStr,
                accessTokenExpiration,
                refreshTokenExpiration);
    }
}
