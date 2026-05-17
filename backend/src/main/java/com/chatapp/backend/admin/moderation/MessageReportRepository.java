package com.chatapp.backend.admin.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MessageReportRepository extends JpaRepository<MessageReport, UUID> {

    Page<MessageReport> findByStatusOrderByCreatedAtDesc(MessageReport.Status status, Pageable pageable);

    long countByStatus(MessageReport.Status status);

    @Modifying
    @Query("""
            update MessageReport r
               set r.status = com.chatapp.backend.admin.moderation.MessageReport.Status.RESOLVED,
                   r.resolvedAt = :now
             where r.messageId = :messageId
               and r.status = com.chatapp.backend.admin.moderation.MessageReport.Status.OPEN
            """)
    int resolveAllForMessage(@Param("messageId") UUID messageId, @Param("now") Instant now);
}
