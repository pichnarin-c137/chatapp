package com.chatapp.backend.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByActorUserIdOrderByAtDesc(UUID actorUserId, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByAtDesc(String entityType, String entityId, Pageable pageable);
}
