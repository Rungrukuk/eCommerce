package ecommerce.auth_service.controller;

import ecommerce.auth_service.dto.UserRegisterDTO;
import ecommerce.auth_service.service.AuthService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @MessageMapping("validateToken")
    public Mono<Boolean> validateToken(String token) {
        // Logic to validate token
        return Mono.just(true); // Placeholder
    }

    @MessageMapping("registerUser")
    public  Mono<ResponseEntity<String>> registerUser(@Valid UserRegisterDTO user) {
        return authService.registerUser(user);
    }

    // @MessageMapping("createGuest")
    // public Mono<String> createGuest() {
    //     return authService.createGuestUser();
    // }
}

