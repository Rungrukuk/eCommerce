package ecommerce.auth_service.security;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import ecommerce.auth_service.service.TokenService;
import reactor.core.publisher.Mono;

@Component
public class TokenReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    @Autowired
    private TokenService tokenService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        if (tokenService.validateAccessToken(token)) {
            // Create and return an authentication token based on your application's
            // requirements
            return Mono.just(new UsernamePasswordAuthenticationToken("user", token, Collections.emptyList()));
        } else {
            return Mono.error(new BadCredentialsException("Invalid token"));
        }
    }
}
