package com.chatapp.backend.common.audit.service;
import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.common.audit.listener.HibernateAuditListener;
import com.chatapp.backend.common.audit.repository.AuditLogRepository;
import com.chatapp.backend.common.audit.entity.AuditAction;
import com.chatapp.backend.common.audit.entity.AuditLog;
import com.chatapp.backend.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Explicit audit logging for high-value actions (deletes, role assignments,
 * bans, etc.). Services call {@link #log} after the mutation commits its work
 * to the parent transaction.
 *
 * <p>Automatic change capture for every JPA mutation also exists via
 * {@link HibernateAuditListener}, which handles the common CREATE/UPDATE/DELETE
 * lifecycle for entities not on the blocklist. Use this service for actions
 * that don't map cleanly to a single entity (e.g. "user X reset password Y").
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;

    @Transactional
    public void log(AuditAction action,
                    String entityType,
                    String entityId,
                    Map<String, Object> before,
                    Map<String, Object> after) {
        AuditLog row = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actorUserId(currentUserId())
                .beforeJson(before)
                .afterJson(after)
                .build();
        repo.save(row);
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.chatapp.backend.user.entity.User u) return u.getId();
        if (principal instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}
