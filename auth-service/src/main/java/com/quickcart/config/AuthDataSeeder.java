package com.quickcart.config;

import com.quickcart.entity.User;
import com.quickcart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthDataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!userRepository.existsByEmail("admin@quickcart.com")) {
            userRepository.save(User.builder()
                    .name("Demo Admin")
                    .email("admin@quickcart.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(com.quickcart.entity.Role.STORE)
                    .provider(com.quickcart.entity.AuthProvider.LOCAL)
                    .build());
            log.info("AuthDataSeeder: seeded Demo Admin user.");
        }

        if (!userRepository.existsByEmail("delivery@quickcart.com")) {
            userRepository.save(User.builder()
                    .name("Demo Delivery Agent")
                    .email("delivery@quickcart.com")
                    .password(passwordEncoder.encode("delivery123"))
                    .role(com.quickcart.entity.Role.DELIVERY)
                    .provider(com.quickcart.entity.AuthProvider.LOCAL)
                    .build());
            log.info("AuthDataSeeder: seeded Demo Delivery user.");
        }
    }
}
