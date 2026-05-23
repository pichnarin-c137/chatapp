package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.PinnedMessage;
import com.chatapp.backend.message.entity.PinnedMessageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, PinnedMessageId> {

    List<PinnedMessage> findByIdConversationIdOrderByPinnedAtDesc(UUID conversationId);
}
