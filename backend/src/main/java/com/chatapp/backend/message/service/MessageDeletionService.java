package com.chatapp.backend.message.service;

import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageDeletion;
import com.chatapp.backend.message.entity.MessageDeletionId;
import com.chatapp.backend.message.repository.MessageDeletionRepository;
import com.chatapp.backend.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * "Delete for me" — the message stays in the database and other users still
 * see it, but this user's history queries filter it out. No broadcast.
 */
@Service
@RequiredArgsConstructor
public class MessageDeletionService {

    private final MessageDeletionRepository deletions;
    private final MessageRepository messages;
    private final MembershipService memberships;

    @Transactional
    public void deleteForMe(UUID messageId, UUID userId) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!memberships.canRead(m.getConversation().getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        MessageDeletionId id = new MessageDeletionId(messageId, userId);
        if (deletions.existsById(id)) return;  // idempotent
        deletions.save(MessageDeletion.builder().id(id).build());
    }

    @Transactional
    public void undoDeleteForMe(UUID messageId, UUID userId) {
        deletions.deleteById(new MessageDeletionId(messageId, userId));
    }
}
