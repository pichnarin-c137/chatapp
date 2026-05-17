package com.chatapp.backend.message.repository;
import com.chatapp.backend.message.entity.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
             where m.conversation.id = :conversationId
               and m.deletedAt is null
             order by m.sentAt desc
            """)
    Page<Message> history(@Param("conversationId") UUID conversationId, Pageable pageable);

    long countByDeletedAtIsNull();
}
