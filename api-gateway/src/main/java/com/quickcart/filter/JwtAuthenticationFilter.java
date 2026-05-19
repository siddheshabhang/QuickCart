package com.quickcart.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.quickcart.security.JwtService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    private static final List<String> PUBLIC_ROUTES = List.of("/auth/");
    private static final List<String> TRUSTED_HEADERS = List.of(
            "X-User-Email", "X-User-Role", "X-User-Id", "X-Internal-Service"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return unauthorized(exchange);
        }

        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);
        Long userId = jwtService.extractUserId(token);

        var requestBuilder = exchange.getRequest().mutate();
        requestBuilder.headers(headers -> TRUSTED_HEADERS.forEach(headers::remove));
        requestBuilder
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Role", role != null ? role : "")
                .header("X-User-Id", userId != null ? userId.toString() : "");

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(requestBuilder.build())
                .build();

        return chain.filter(modifiedExchange);
    }

    private boolean isPublicRoute(String path) {
        return PUBLIC_ROUTES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}