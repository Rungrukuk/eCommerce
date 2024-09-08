package ecommerce.auth_service.controller;

import ecommerce.auth_service.CreateUserRequest;
import ecommerce.auth_service.CreateUserResponse;
import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.service.TokenService;
import ecommerce.auth_service.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@MessageMapping("auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    // TODO Implement RBAC
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

    // TODO handle token creation and validation for the services: make sure to
    // solve the target and source service problem

}
