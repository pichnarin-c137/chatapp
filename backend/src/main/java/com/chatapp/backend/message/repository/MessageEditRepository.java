package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageEdit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageEditRepository extends JpaRepository<MessageEdit, UUID> {

    List<MessageEdit> findByMessageIdOrderByEditedAtDesc(UUID messageId);
}
