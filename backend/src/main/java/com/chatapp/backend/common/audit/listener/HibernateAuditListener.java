package com.chatapp.backend.common.audit.listener;
import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.common.audit.repository.AuditLogRepository;
import com.chatapp.backend.common.audit.entity.AuditAction;
import com.chatapp.backend.common.audit.entity.AuditLog;
import com.chatapp.backend.message.entity.MessageRead;
import com.chatapp.backend.user.entity.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Captures every JPA insert / update / delete and writes one row to the
 * {@code audit_log} table with column-state snapshots.
 *
 * <p>Writes use raw JDBC ({@link JdbcTemplate}) — going through the
 * {@code AuditLogRepository} would re-enter Hibernate's flush cycle and
 * recurse into this listener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateAuditListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Set<String> SKIPPED_TYPES = Set.of(
            "com.chatapp.backend.common.audit.AuditLog",
            "com.chatapp.backend.message.MessageRead"
    );

    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void register() {
        SessionFactoryImplementor sf = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sf.getServiceRegistry().getService(EventListenerRegistry.class);
        if (registry == null) {
            log.warn("Hibernate EventListenerRegistry unavailable — audit log disabled");
            return;
        }
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
        log.info("Hibernate audit listener registered");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        write(AuditAction.CREATE, event.getEntity(), null, event.getState(), event.getPersister());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        write(AuditAction.UPDATE, event.getEntity(), event.getOldState(), event.getState(), event.getPersister());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        write(AuditAction.DELETE, event.getEntity(), event.getDeletedState(), null, event.getPersister());
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private void write(AuditAction action,
                       Object entity,
                       Object[] oldState,
                       Object[] newState,
                       EntityPersister persister) {
        String type = entity.getClass().getName();
        if (SKIPPED_TYPES.contains(type)) return;

        try {
            String beforeJson = oldState == null ? null : toJson(toMap(oldState, persister));
            String afterJson = newState == null ? null : toJson(toMap(newState, persister));
            UUID actor = currentUserId();

            jdbcTemplate.update(
                    "insert into audit_log (at, actor_user_id, action, entity_type, entity_id, before_json, after_json) " +
                            "values (now(), ?, ?, ?, ?, ?::jsonb, ?::jsonb)",
                    ps -> {
                        if (actor == null) ps.setNull(1, Types.OTHER);
                        else                ps.setObject(1, actor);
                        ps.setString(2, action.name());
                        ps.setString(3, type);
                        ps.setString(4, idString(entity, persister));
                        if (beforeJson == null) ps.setNull(5, Types.OTHER); else ps.setString(5, beforeJson);
                        if (afterJson == null)  ps.setNull(6, Types.OTHER); else ps.setString(6, afterJson);
                    }
            );
        } catch (Exception e) {
            // Auditing must never break business writes.
            log.warn("Failed to write audit log for {} ({}): {}", type, action, e.toString());
        }
    }

    private String idString(Object entity, EntityPersister persister) {
        Object id = persister.getIdentifier(entity, (org.hibernate.engine.spi.SharedSessionContractImplementor) null);
        return id == null ? "" : id.toString();
    }

    private Map<String, Object> toMap(Object[] state, EntityPersister persister) {
        if (state == null) return null;
        String[] names = persister.getPropertyNames();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            Object v = state[i];
            map.put(names[i], v == null ? null : safeValue(v));
        }
        return map;
    }

    /** Hibernate property values can be entities or proxies — flatten to strings. */
    private Object safeValue(Object v) {
        if (v instanceof Number || v instanceof Boolean || v instanceof String || v instanceof Enum<?>) return v.toString();
        if (v instanceof java.time.temporal.Temporal) return v.toString();
        if (v instanceof UUID) return v.toString();
        try {
            return objectMapper.convertValue(v, Object.class);
        } catch (Exception e) {
            return v.toString();
        }
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (p instanceof com.chatapp.backend.user.entity.User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}
