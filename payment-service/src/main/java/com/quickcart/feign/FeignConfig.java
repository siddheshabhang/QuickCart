package com.quickcart.feign;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String email  = request.getHeader("X-User-Email");
                String role   = request.getHeader("X-User-Role");
                String userId = request.getHeader("X-User-Id");
                if (email != null)  requestTemplate.header("X-User-Email", email);
                if (role != null)   requestTemplate.header("X-User-Role", role);
                if (userId != null) requestTemplate.header("X-User-Id", userId);
                requestTemplate.header("X-Internal-Service", "PAYMENT-SERVICE");
            }
        };
    }
}
