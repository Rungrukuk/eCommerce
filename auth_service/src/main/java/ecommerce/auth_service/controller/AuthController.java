package ecommerce.auth_service.controller;

import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.service.UserServcie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserServcie userService;

    @MessageMapping("validateToken")
    public Mono<Boolean> validateToken(String token) {
        return Mono.just(true);
    }

    @MessageMapping("registerUser")
    public Mono<ResponseEntity<String>> registerUser(UserCreateDTO userDTO) {
        return userService.createUser(userDTO).flatMap(
                userResponse -> {
                    if (userResponse.getErrors().isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(userResponse.getAccessToken());
                        headers.add("Refresh-Token", userResponse.getRefreshToken());
                        return Mono.just(
                                ResponseEntity
                                        .status(201)
                                        .headers(headers)
                                        .body("User Created Successfully"));
                    } else {
                        return Mono.just(
                                ResponseEntity
                                        .badRequest()
                                        .body("User creation failed: " + String.join(", ", userResponse.getErrors())));
                    }
                });
    }

    @MessageMapping("guestUser")
    public Mono<ResponseEntity<Void>> createGuestUser() {
        return userService.createGuestUser();
    }
}
