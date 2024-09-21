package ecommerce.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import ecommerce.api_gateway.ProtoAuthRequest;
import ecommerce.api_gateway.ProtoAuthResponse;
import ecommerce.api_gateway.ProtoRequest;
import ecommerce.api_gateway.ProtoResponse;
import ecommerce.api_gateway.service.RSocketService;
import ecommerce.api_gateway.util.AuthResponseStatuses;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class ApiGatewayController {

        @Autowired
        private RSocketService rSocketService;

        private Mono<ProtoAuthResponse> validateToken(Map<String, String> headerMap, String audience,
                        String destination) {
                ProtoAuthRequest protoAuthRequest = ProtoAuthRequest.newBuilder()
                                .putMetadata("accessToken", headerMap.getOrDefault("Authorization", ""))
                                .putMetadata("sessionId", headerMap.getOrDefault("Session-Id", ""))
                                .putMetadata("audience", audience)
                                .putMetadata("destination", destination)
                                .build();

                return rSocketService.getRSocketRequester().route("auth.validateToken")
                                .data(protoAuthRequest)
                                .retrieveMono(ProtoAuthResponse.class);
        }

        @PostMapping("register")
        public Mono<ResponseEntity<Map<String, String>>> registerUser(@RequestHeader HttpHeaders headers,
                        @RequestBody Map<String, String> body) {
                // TODO save the audinece and destination in enums
                return validateToken(headers.toSingleValueMap(), "AUTH_SERVICE", "REGISTER").flatMap(
                                authResponse -> {
                                        Map<String, String> responseBody = new HashMap<>();
                                        HttpHeaders responseHeaders = new HttpHeaders();
                                        if (authResponse.getStatus()
                                                        .equals(AuthResponseStatuses.AUTHORIZED_GUEST_USER.name())) {

                                                ProtoRequest protoRequest = ProtoRequest.newBuilder()
                                                                .putMetadata("serviceToken",
                                                                                authResponse.getMetadataOrDefault(
                                                                                                "serviceToken", ""))
                                                                .putData("email", body.getOrDefault("email", ""))
                                                                .putData("password", body.getOrDefault("password", ""))
                                                                .putData("rePassword",
                                                                                body.getOrDefault("rePassword", ""))
                                                                .build();
                                                return rSocketService.getRSocketRequester().route("auth.registerUser")
                                                                .data(protoRequest)
                                                                .retrieveMono(ProtoResponse.class).map(
                                                                                protoResponse -> {
                                                                                        responseBody.put("userStatus",
                                                                                                        authResponse.getStatus());
                                                                                        responseBody.put("status",
                                                                                                        protoResponse.getStatus());
                                                                                        responseBody.put("message",
                                                                                                        protoResponse.getMessage());
                                                                                        responseBody.put("email",
                                                                                                        protoResponse.getDataOrDefault(
                                                                                                                        "email",
                                                                                                                        ""));
                                                                                        responseHeaders.set(
                                                                                                        "Authorization",
                                                                                                        protoResponse.getMetadataOrDefault(
                                                                                                                        "accessToken",
                                                                                                                        ""));
                                                                                        responseHeaders.set(
                                                                                                        "Session-Id",
                                                                                                        protoResponse.getMetadataOrDefault(
                                                                                                                        "sessionId",
                                                                                                                        ""));
                                                                                        responseHeaders.set(
                                                                                                        "Refresh-Token",
                                                                                                        protoResponse.getMetadataOrDefault(
                                                                                                                        "refreshToken",
                                                                                                                        ""));

                                                                                        return new ResponseEntity<>(
                                                                                                        responseBody,
                                                                                                        responseHeaders,
                                                                                                        protoResponse.getStatusCode());
                                                                                });
                                        }
                                        // TODO log the suspicious event
                                        responseBody.put("userStatus", authResponse.getStatus());
                                        responseHeaders.set("Authorization",
                                                        authResponse.getMetadataOrDefault("accessToken", ""));
                                        responseHeaders.set("Session-Id",
                                                        authResponse.getMetadataOrDefault("sessionId", ""));
                                        return Mono.just(new ResponseEntity<>(responseBody, responseHeaders,
                                                        authResponse.getStatusCode()));
                                });
        }

}
