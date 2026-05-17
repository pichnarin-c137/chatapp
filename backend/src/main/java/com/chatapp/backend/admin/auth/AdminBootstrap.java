package com.chatapp.backend.admin.auth;

import com.chatapp.backend.rbac.Role;
import com.chatapp.backend.rbac.RoleRepository;
import com.chatapp.backend.rbac.UserRole;
import com.chatapp.backend.rbac.UserRoleId;
import com.chatapp.backend.rbac.UserRoleRepository;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserProfile;
import com.chatapp.backend.user.UserProfileRepository;
import com.chatapp.backend.user.UserRepository;
import com.chatapp.backend.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {

    @Bean
    public ApplicationRunner ensureDefaultAdmin(AdminBootstrapper bootstrapper) {
        return args -> bootstrapper.run();
    }

    @Component
    @RequiredArgsConstructor
    static class AdminBootstrapper {

        @Value("${app.admin.bootstrap.username:admin}")
        private String adminUsername;

        @Value("${app.admin.bootstrap.email:admin@chatapp.local}")
        private String adminEmail;

        @Value("${app.admin.bootstrap.password:admin123}")
        private String adminPassword;

        private final UserRepository users;
        private final UserProfileRepository profiles;
        private final RoleRepository roles;
        private final UserRoleRepository userRoles;
        private final PasswordEncoder encoder;

        @Transactional
        public void run() {
            Role superAdmin = roles.findByCode(Role.CODE_SUPER_ADMIN)
                    .orElseThrow(() -> new IllegalStateException(
                            "SUPER_ADMIN role missing — V1 migration did not seed roles"));

            User user = users.findByUsername(adminUsername).orElseGet(() -> {
                User created = User.builder()
                        .username(adminUsername)
                        .email(adminEmail.toLowerCase())
                        .passwordHash(encoder.encode(adminPassword))
                        .status(UserStatus.ACTIVE)
                        .build();
                users.save(created);
                profiles.save(UserProfile.builder()
                        .user(created)
                        .displayName("Site Admin")
                        .build());
                log.info("Bootstrapped default admin '{}' (change app.admin.bootstrap.password in prod!)", adminUsername);
                return created;
            });

            if (!userRoles.existsByUserIdAndRoleCode(user.getId(), Role.CODE_SUPER_ADMIN)) {
                userRoles.save(UserRole.builder()
                        .id(new UserRoleId(user.getId(), superAdmin.getId()))
                        .user(user)
                        .role(superAdmin)
                        .build());
                log.info("Granted SUPER_ADMIN role to '{}'", adminUsername);
            }
        }
    }
}
