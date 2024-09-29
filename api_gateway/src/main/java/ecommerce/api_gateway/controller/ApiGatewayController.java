package ecommerce.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import ecommerce.api_gateway.ProtoRequest;
import ecommerce.api_gateway.ProtoResponse;
import ecommerce.api_gateway.security.CustomAuthentication;
import ecommerce.api_gateway.service.RSocketService;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@Controller
@RequestMapping("/")
public class ApiGatewayController {

        @Autowired
        private RSocketService rSocketService;

        @PostMapping("register")
        public Mono<ResponseEntity<Map<String, String>>> registerUser(
                        @RequestHeader HttpHeaders headers,
                        @RequestBody Map<String, String> body,
                        ServerWebExchange exchange) {

                return ReactiveSecurityContextHolder.getContext()
                                .flatMap(securityContext -> {
                                        CustomAuthentication authentication = (CustomAuthentication) securityContext
                                                        .getAuthentication();
                                        String serviceToken = authentication.getPrincipal().get("serviceToken");
                                        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
                                        String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");

                                        ProtoRequest protoRequest = ProtoRequest.newBuilder()
                                                        .putMetadata("serviceToken", serviceToken)
                                                        .putMetadata("userAgent", userAgent)
                                                        .putMetadata("clientCity", clientCity)
                                                        .putData("email", body.getOrDefault("email", ""))
                                                        .putData("password", body.getOrDefault("password", ""))
                                                        .putData("rePassword", body.getOrDefault("rePassword", ""))
                                                        .build();

                                        return rSocketService.getRSocketRequester().route("auth.registerUser")
                                                        .data(protoRequest)
                                                        .retrieveMono(ProtoResponse.class)
                                                        .flatMap(protoResponse -> {
                                                                Map<String, String> responseBody = createResponseBody(
                                                                                authentication, protoResponse);
                                                                setCookiesIfPresent(exchange, protoResponse);
                                                                HttpStatus status = HttpStatus
                                                                                .valueOf(protoResponse.getStatusCode());
                                                                return Mono.just(new ResponseEntity<>(responseBody,
                                                                                status));
                                                        })
                                                        .onErrorResume(e -> {
                                                                System.out.println(e.getMessage());
                                                                e.printStackTrace();
                                                                return Mono.just(new ResponseEntity<>(
                                                                                Map.of("error", "Registration failed"),
                                                                                HttpStatus.INTERNAL_SERVER_ERROR));
                                                        });
                                });
        }

        @PostMapping("login")
        public Mono<ResponseEntity<Map<String, String>>> loginUser(
                        @RequestHeader HttpHeaders headers,
                        @RequestBody Map<String, String> body,
                        ServerWebExchange exchange) {

                return ReactiveSecurityContextHolder.getContext()
                                .flatMap(securityContext -> {
                                        CustomAuthentication authentication = (CustomAuthentication) securityContext
                                                        .getAuthentication();
                                        String serviceToken = authentication.getPrincipal().get("serviceToken");
                                        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
                                        String clientCity = exchange.getRequest().getHeaders().getFirst("Client-City");

                                        ProtoRequest protoRequest = ProtoRequest.newBuilder()
                                                        .putMetadata("serviceToken", serviceToken)
                                                        .putMetadata("userAgent", userAgent)
                                                        .putMetadata("clientCity", clientCity)
                                                        .putData("email", body.getOrDefault("email", ""))
                                                        .putData("password", body.getOrDefault("password", ""))
                                                        .build();

                                        return rSocketService.getRSocketRequester().route("auth.loginUser")
                                                        .data(protoRequest)
                                                        .retrieveMono(ProtoResponse.class)
                                                        .flatMap(protoResponse -> {
                                                                Map<String, String> responseBody = createResponseBody(
                                                                                authentication, protoResponse);
                                                                setCookiesIfPresent(exchange, protoResponse);
                                                                HttpStatus status = HttpStatus
                                                                                .valueOf(protoResponse.getStatusCode());
                                                                return Mono.just(new ResponseEntity<>(responseBody,
                                                                                status));
                                                        })
                                                        .onErrorResume(e -> {
                                                                System.out.println(e.getMessage());
                                                                e.printStackTrace();
                                                                return Mono.just(new ResponseEntity<>(
                                                                                Map.of("error", "Registration failed"),
                                                                                HttpStatus.INTERNAL_SERVER_ERROR));
                                                        });
                                });
        }

        @GetMapping
        public Mono<ResponseEntity<Map<String, String>>> getRoot() {
                return ReactiveSecurityContextHolder.getContext().flatMap(
                                securityContext -> {
                                        CustomAuthentication authentication = (CustomAuthentication) securityContext
                                                        .getAuthentication();
                                        return Mono.just(new ResponseEntity<>(
                                                        Map.of("userStatus", authentication.getUserStatus()),
                                                        HttpStatus.OK));
                                });
        }

        private Map<String, String> createResponseBody(CustomAuthentication authentication,
                        ProtoResponse protoResponse) {
                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("userStatus", authentication.getUserStatus());
                responseBody.put("status", protoResponse.getStatus());
                responseBody.put("message", protoResponse.getMessage());
                return responseBody;
        }

        private void setCookiesIfPresent(ServerWebExchange exchange, ProtoResponse protoResponse) {
                Optional.ofNullable(protoResponse.getMetadataOrDefault("accessToken", ""))
                                .filter(token -> !token.isEmpty())
                                .ifPresent(accessToken -> setCookie(exchange, "accessToken", accessToken, 86400));

                Optional.ofNullable(protoResponse.getMetadataOrDefault("sessionId", ""))
                                .filter(sessionId -> !sessionId.isEmpty())
                                .ifPresent(sessionId -> setCookie(exchange, "sessionId", sessionId, 86400));

                Optional.ofNullable(protoResponse.getMetadataOrDefault("refreshToken", ""))
                                .filter(refreshToken -> !refreshToken.isEmpty())
                                .ifPresent(refreshToken -> setCookie(exchange, "refreshToken", refreshToken, 604800));
        }

        private void setCookie(ServerWebExchange exchange, String name, String value, int maxAge) {
                exchange.getResponse().addCookie(ResponseCookie.from(name, value)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .sameSite("Strict")
                                .maxAge(maxAge)
                                .build());
        }

}
