package com.chatapp.backend.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MessageReportRepository extends JpaRepository<MessageReport, UUID> {

    Page<MessageReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);

    @Modifying
    @Query("""
            update MessageReport r
               set r.status = com.chatapp.backend.moderation.ReportStatus.RESOLVED,
                   r.resolvedAt = :now
             where r.message.id = :messageId
               and r.status = com.chatapp.backend.moderation.ReportStatus.OPEN
            """)
    int resolveAllForMessage(@Param("messageId") UUID messageId, @Param("now") Instant now);
}
