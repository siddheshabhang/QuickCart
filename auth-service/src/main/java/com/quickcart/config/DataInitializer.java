package com.quickcart.config;

import com.quickcart.entity.AuthProvider;
import com.quickcart.entity.Role;
import com.quickcart.entity.User;
import com.quickcart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDemoUsers() {
        return args -> {
            seedUser("QuickCart Customer", "customer@quickcart.dev", "Customer@123", Role.CUSTOMER);
            seedUser("QuickCart Store", "store@quickcart.dev", "Store@123", Role.STORE);
            seedUser("QuickCart Delivery", "delivery@quickcart.dev", "Delivery@123", Role.DELIVERY);
        };
    }

    private void seedUser(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            log.info("Demo user already exists: {}", email);
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);
        log.info("Seeded demo user: {} ({})", email, role);
    }
}
