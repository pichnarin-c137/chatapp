package com.chatapp.backend.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            select c from Conversation c
             where c.deletedAt is null
               and (:q is null or :q = '' or lower(c.name) like lower(concat('%', :q, '%')))
             order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    Page<Conversation> adminSearch(@Param("q") String q, Pageable pageable);

    long countByDeletedAtIsNull();

    @org.springframework.data.jpa.repository.Query("""
            select c from Conversation c
             where c.deletedAt is null and c.type = com.chatapp.backend.conversation.ConversationType.CHANNEL
             order by c.createdAt asc
            """)
    java.util.List<Conversation> findAllChannels();
}
