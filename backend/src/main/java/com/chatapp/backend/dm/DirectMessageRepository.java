package com.chatapp.backend.dm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
    Page<DirectMessage> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
    Optional<DirectMessage> findFirstByConversationIdOrderBySentAtDesc(UUID conversationId);
}
