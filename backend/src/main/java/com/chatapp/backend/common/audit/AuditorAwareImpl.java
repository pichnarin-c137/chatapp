package com.chatapp.backend.common.audit;

import com.chatapp.backend.user.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the current user's id for @CreatedBy / @LastModifiedBy.
 * Returns empty for anonymous / system operations (bootstrap, scheduled jobs).
 */
@Component
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            return Optional.ofNullable(u.getId());
        }
        if (principal instanceof CustomUserDetails cud) {
            return Optional.ofNullable(cud.getId());
        }
        return Optional.empty();
    }
}
