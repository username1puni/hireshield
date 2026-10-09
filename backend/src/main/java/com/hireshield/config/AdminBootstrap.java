package com.hireshield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hireshield.entity.Role;
import com.hireshield.entity.User;
import com.hireshield.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD so no password ever sits in a migration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("Promoted {} to admin", email);
            }
        }, () -> {
            userRepository.save(new User("Admin", email.toLowerCase(), passwordEncoder.encode(password), Role.ADMIN));
            log.info("Created admin account {}", email);
        });
    }
}
