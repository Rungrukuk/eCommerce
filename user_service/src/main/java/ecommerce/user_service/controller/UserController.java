package ecommerce.user_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import ecommerce.auth_service.RequestProto.ProtoRequest;
import ecommerce.auth_service.ResponseProto.ProtoResponse;
import ecommerce.user_service.service.TokenService.Destination;
import ecommerce.user_service.service.TokenService;
import ecommerce.user_service.service.UserService;
import reactor.core.publisher.Mono;

@Controller
@MessageMapping("user")
public class UserController {

        @Autowired
        private TokenService tokenService;

        @Autowired
        private UserService userService;

        @MessageMapping("createUserDetails")
        public Mono<ProtoResponse> createUser(ProtoRequest request) {

                String userId = tokenService.validateTokenAndGetUserId(
                                request.getMetadataOrDefault("serviceToken", ""),
                                Destination.CREATE_USER_DETAILS);

                if (userId != null) {
                        Map<String, String> data = new HashMap<>(request.getDataMap());
                        data.put("userId", userId);
                        return userService.createUserDetails(data)
                                        .flatMap(userResponse -> {
                                                return Mono.just(ProtoResponse.newBuilder()
                                                                .setStatusCode(userResponse.getStatusCode())
                                                                .setMessage(userResponse.getMessage())
                                                                .setStatus(userResponse.getResponseStatus().name())
                                                                .build());
                                        });
                }

                return Mono.just(
                                ProtoResponse.newBuilder()
                                                .setStatusCode(403)
                                                .setMessage("Forbidden")
                                                .build());
        }
}

