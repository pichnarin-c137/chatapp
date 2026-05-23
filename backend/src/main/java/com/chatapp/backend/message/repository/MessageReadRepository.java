package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageRead;
import com.chatapp.backend.message.entity.MessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {

    List<MessageRead> findByIdMessageId(UUID messageId);
}
