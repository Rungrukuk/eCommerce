package ecommerce.auth_service.controller;

// import ecommerce.auth_service.dto.RoleDTO;
import ecommerce.auth_service.dto.UserCreateDTO;
// import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.UserServcie;
// import io.jsonwebtoken.Jwt;
// import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;

// import com.google.protobuf.ByteString;

import reactor.core.publisher.Mono;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserServcie userService;

    // @Autowired
    // private final JwtTokenProvider jwtTokenProvider;

    // @MessageMapping("validateToken")
    // public Mono<Boolean> validateToken(ByteString tokenByteString) {
    // String token = new String(tokenByteString.toByteArray());
    // // Validate token with JwtUtil
    // try {
    // return Mono.just(jwtTokenProvider.validateToken(token));
    // } catch (Exception e) {
    // return Mono.just(false);
    // }
    // }

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

    //TODO handle token creation and validation fopr the services
    // @MessageMapping("validateAndCreateToken")
    // public Mono<String> validateAndCreateToken(String token) {
    // if (jwtTokenProvider.validateToken(token)) {
    // Claims claims = jwtTokenProvider.getClaims(token);

    // String userId = claims.getSubject(); // userId
    // RoleDTO role = claims.get("role", RoleDTO.class);
    // String newToken = jwtTokenProvider.createGatewayToken(userId, role, "API
    // Gateway", "SomeService");

    // return Mono.just(newToken);
    // } else {
    // return Mono.error(new RuntimeException("Invalid token"));
    // }
    // }

    // @PostMapping("/add")
    // public Mono<ResponseEntity<String>> addToCart(@RequestBody AddToCartRequest
    // request,
    // @AuthenticationPrincipal Jwt jwt) {
    // String token = jwt.getTokenValue();
    // return rSocketRequester.route("cart.add").metadata(token,
    // "bearer").data(request).retrieveMono(String.class)
    // .map(response -> ResponseEntity.ok(response)).onErrorResume(e ->
    // Mono.just(ResponseEntity
    // .status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " +
    // e.getMessage())));
    // }

    // @MessageMapping("guestUser")
    // public Mono<ResponseEntity<Void>> createGuestUser() {
    // return userService.createGuestUser();
    // }
}
