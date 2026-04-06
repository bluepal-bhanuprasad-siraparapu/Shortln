package com.urlshortener.config;

import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "sirbhanu2000@gmail.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123")) // Default hardcoded password
                    .role(Role.ADMIN)
                    .plan(Plan.PRO)
                    .build();

            userRepository.save(admin);
            log.info("Initialized hardcoded ADMIN user: {} with password: admin123", adminEmail);
        } else {
            log.info("ADMIN user already exists, skipping initialization.");
        }
    }
}
