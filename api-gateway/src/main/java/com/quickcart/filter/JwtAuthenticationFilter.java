package com.quickcart.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

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

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(r -> r.header("X-User-Email", email != null ? email : "")
                        .header("X-User-Role", role != null ? role : ""))
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