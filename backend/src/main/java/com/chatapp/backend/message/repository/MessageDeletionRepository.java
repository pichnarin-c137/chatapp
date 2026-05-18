package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageDeletion;
import com.chatapp.backend.message.entity.MessageDeletionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageDeletionRepository extends JpaRepository<MessageDeletion, MessageDeletionId> {

    boolean existsById(MessageDeletionId id);
}
