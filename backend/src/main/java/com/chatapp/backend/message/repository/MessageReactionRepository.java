package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageReaction;
import com.chatapp.backend.message.entity.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, MessageReactionId> {

    @Query("""
            select r from MessageReaction r
             where r.id.messageId in :messageIds
            """)
    List<MessageReaction> findByMessageIdIn(@Param("messageIds") Collection<UUID> messageIds);

    @Query("""
            select r from MessageReaction r
             where r.id.messageId = :messageId
            """)
    List<MessageReaction> findByMessageId(@Param("messageId") UUID messageId);

    @Modifying
    @Query("""
            delete from MessageReaction r
             where r.id.messageId = :messageId
               and r.id.userId    = :userId
               and r.id.emoji     = :emoji
            """)
    int deleteOne(@Param("messageId") UUID messageId,
                  @Param("userId")    UUID userId,
                  @Param("emoji")     String emoji);
}
