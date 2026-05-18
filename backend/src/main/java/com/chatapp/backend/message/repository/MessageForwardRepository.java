package com.chatapp.backend.message.repository;

import com.chatapp.backend.message.entity.MessageForward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageForwardRepository extends JpaRepository<MessageForward, UUID> {

    List<MessageForward> findByForwardedMessageIdIn(Collection<UUID> forwardedMessageIds);
}
