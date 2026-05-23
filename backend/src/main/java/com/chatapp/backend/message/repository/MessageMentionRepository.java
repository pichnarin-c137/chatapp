package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageMention;
import com.chatapp.backend.message.entity.MessageMentionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageMentionRepository extends JpaRepository<MessageMention, MessageMentionId> {

    @Query("""
            select mm from MessageMention mm
             where mm.id.messageId in :messageIds
            """)
    List<MessageMention> findByMessageIdIn(@Param("messageIds") Collection<UUID> messageIds);
}
