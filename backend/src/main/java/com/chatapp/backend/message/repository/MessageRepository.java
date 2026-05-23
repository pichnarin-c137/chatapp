package com.chatapp.backend.message.repository;
import com.chatapp.backend.message.entity.Message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Latest page (no cursor). Skips messages the user hid via delete-for-me. */
    @Query("""
            select m from Message m
             where m.conversation.id = :conversationId
               and m.deletedAt is null
               and not exists (
                   select 1 from MessageDeletion d
                    where d.id.messageId = m.id
                      and d.id.userId    = :userId
               )
             order by m.sentAt desc, m.id desc
            """)
    List<Message> findLatestPage(@Param("conversationId") UUID conversationId,
                                 @Param("userId")        UUID userId,
                                 Pageable pageable);

    /**
     * Page strictly older than the cursor. (sentAt, id) breaks ties so the
     * order matches the unique composite index.
     */
    @Query("""
            select m from Message m
             where m.conversation.id = :conversationId
               and m.deletedAt is null
               and not exists (
                   select 1 from MessageDeletion d
                    where d.id.messageId = m.id
                      and d.id.userId    = :userId
               )
               and (m.sentAt <  :cursorSentAt
                    or (m.sentAt = :cursorSentAt and m.id < :cursorId))
             order by m.sentAt desc, m.id desc
            """)
    List<Message> findHistoryBefore(@Param("conversationId") UUID conversationId,
                                    @Param("userId")        UUID userId,
                                    @Param("cursorSentAt")  Instant cursorSentAt,
                                    @Param("cursorId")      UUID cursorId,
                                    Pageable pageable);

    /** Page strictly newer than the cursor. Used by clients reconnecting offline. */
    @Query("""
            select m from Message m
             where m.conversation.id = :conversationId
               and m.deletedAt is null
               and not exists (
                   select 1 from MessageDeletion d
                    where d.id.messageId = m.id
                      and d.id.userId    = :userId
               )
               and (m.sentAt >  :cursorSentAt
                    or (m.sentAt = :cursorSentAt and m.id > :cursorId))
             order by m.sentAt asc, m.id asc
            """)
    List<Message> findHistoryAfter(@Param("conversationId") UUID conversationId,
                                   @Param("userId")        UUID userId,
                                   @Param("cursorSentAt")  Instant cursorSentAt,
                                   @Param("cursorId")      UUID cursorId,
                                   Pageable pageable);

    long countByDeletedAtIsNull();
}
