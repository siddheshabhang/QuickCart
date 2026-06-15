package com.quickcart.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Shared filter for all downstream microservices.
 *
 * The API Gateway validates the JWT and forwards two trusted headers:
 *   X-User-Email  → the authenticated user's email
 *   X-User-Role   → the user's role (e.g. CUSTOMER, STORE, DELIVERY)
 *
 * This filter reads those headers and populates the SecurityContextHolder
 * so that @PreAuthorize("hasRole(...)") works per-method in any service.
 *
 * Usage: Add spring-boot-starter-security to the service's pom.xml,
 * create a SecurityConfig with @EnableMethodSecurity, and register
 * this filter before UsernamePasswordAuthenticationFilter.
 */
@Component
public class  GatewayAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String email  = request.getHeader("X-User-Email");
        String role   = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        if (userId != null && !userId.isBlank()
                && role  != null && !role.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Spring Security expects "ROLE_" prefix for hasRole() / hasAnyRole()
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            // principal = userId (parsed as Long by services via getName())
            // credentials = email (retrieved via getCredentials() — no Feign call needed)
            var auth      = new UsernamePasswordAuthenticationToken(userId, email, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
