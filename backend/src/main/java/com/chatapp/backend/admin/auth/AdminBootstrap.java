package com.chatapp.backend.admin.auth;

import com.chatapp.backend.user.Role;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {

    @Value("${app.admin.bootstrap.username:admin}")
    private String adminUsername;

    @Value("${app.admin.bootstrap.email:admin@chatapp.local}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.password:admin123}")
    private String adminPassword;

    @Bean
    public ApplicationRunner ensureDefaultAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.existsByUsername(adminUsername) || users.existsByEmail(adminEmail.toLowerCase())) {
                users.findByUsername(adminUsername).ifPresent(u -> {
                    if (u.getRole() != Role.ADMIN) {
                        u.setRole(Role.ADMIN);
                        users.save(u);
                        log.info("Promoted existing user '{}' to ADMIN", adminUsername);
                    }
                });
                return;
            }
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail.toLowerCase())
                    .passwordHash(encoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            users.save(admin);
            log.info("Bootstrapped default admin '{}' (change app.admin.bootstrap.password in prod!)", adminUsername);
        };
    }
}
