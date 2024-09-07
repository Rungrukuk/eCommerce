package ecommerce.auth_service.controller;

import ecommerce.auth_service.CreateUserRequest;
import ecommerce.auth_service.CreateUserResponse;
// import ecommerce.auth_service.dto.RoleDTO;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.service.TokenService;
// import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.UserService;
// import io.jsonwebtoken.Jwt;
// import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Autowired;
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
    private UserService userService;

    @Autowired
    private TokenService tokenService;

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
    public Mono<CreateUserResponse> registerUser(CreateUserRequest request) {
        if (!tokenService.validateAccessToken(request.getAccessToken())) {
            return Mono.just(CreateUserResponse.newBuilder()
                    .setStatusCode(403)
                    .setBody("Access Denied! Unauthorized Access")
                    .build());
        }

        UserCreateDTO userCreateDTO = new UserCreateDTO(request.getEmail(), request.getPassword(),
                request.getRePassword());

        return userService.createUser(userCreateDTO)
                .map(userResponse -> {
                    CreateUserResponse.Builder responseBuilder = CreateUserResponse.newBuilder();
                    if (userResponse.getErrors().isEmpty()) {
                        responseBuilder
                                .setAccessToken(userResponse.getAccessToken())
                                .setRefreshToken(userResponse.getRefreshToken())
                                .setStatusCode(201)
                                .setBody("User Created Successfully");
                    } else {
                        responseBuilder
                                .setStatusCode(400)
                                .setBody("Bad Request")
                                .addAllErrors(userResponse.getErrors());
                    }
                    return responseBuilder.build();
                });
    }

    // TODO handle token creation and validation for the services: make sure to solve the target and source service problem 
    // @MessageMapping("validateAndCreateToken")
    // public Mono<String> validateAndCreateToken(String token) {
    // if (jwtTokenProvider.validateToken(token)) {
    // Claims claims = jwtTokenProvider.getClaims(token);

    // String userId = claims.getSubject(); // userId
    // RoleDTO role = claims.get("role", RoleDTO.class);
    // String newToken = jwtTokenProvider.createGatewayToken(userId, role,
    // "APIGateway", "SomeService");

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
