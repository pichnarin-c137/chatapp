package com.chatapp.backend.message.service;

import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.message.dto.ReadReceiptDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageRead;
import com.chatapp.backend.message.entity.MessageReadId;
import com.chatapp.backend.message.event.MessageSeenEvent;
import com.chatapp.backend.message.repository.MessageReadRepository;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tracks per-message read receipts. The conversation-level lastReadMessageId
 * (on ConversationMember) is the high-water mark used for unread counts; the
 * message_reads row exists so the UI can answer "who has read this exact
 * message" and the broadcast fans the receipt out to other clients.
 */
@Service
@RequiredArgsConstructor
public class MessageReadReceiptService {

    private final MessageRepository messages;
    private final MessageReadRepository reads;
    private final UserRepository users;
    private final MembershipService memberships;
    private final ApplicationEventPublisher events;

    @Transactional
    public void markSeen(UUID conversationId, UUID userId, UUID lastSeenMessageId) {
        Message m = messages.findById(lastSeenMessageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!m.getConversation().getId().equals(conversationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this conversation");
        }
        if (!memberships.canRead(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        // Update the high-water mark on conversation_members (only if it's a member;
        // CHANNEL readers without membership get a broadcast but no persistent cursor).
        if (memberships.isActiveMember(conversationId, userId)) {
            memberships.markRead(conversationId, userId, lastSeenMessageId);
        }

        // Insert a per-message receipt if not already there.
        MessageReadId id = new MessageReadId(lastSeenMessageId, userId);
        if (!reads.existsById(id)) {
            User user = users.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            reads.save(MessageRead.builder().id(id).message(m).user(user).build());
        }

        events.publishEvent(new MessageSeenEvent(conversationId, userId, lastSeenMessageId, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<ReadReceiptDto> readsOf(UUID messageId) {
        return reads.findByIdMessageId(messageId).stream()
                .map(ReadReceiptDto::from)
                .toList();
    }
}
