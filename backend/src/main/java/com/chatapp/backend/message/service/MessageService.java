package com.chatapp.backend.message.service;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageEdit;
import com.chatapp.backend.message.entity.MessageType;
import com.chatapp.backend.message.event.MessageDeletedEvent;
import com.chatapp.backend.message.event.MessageEditedEvent;
import com.chatapp.backend.message.event.MessageSentEvent;
import com.chatapp.backend.message.repository.MessageEditRepository;
import com.chatapp.backend.message.repository.MessageRepository;

import com.chatapp.backend.common.cache.IdempotencyService;
import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    /** Author-driven delete-for-everyone is allowed within this window after send. */
    private static final Duration DELETE_FOR_EVERYONE_WINDOW = Duration.ofHours(24);
    /** Author-driven edit is allowed within this window after send. */
    private static final Duration EDIT_WINDOW = Duration.ofHours(24);

    private final MessageRepository messages;
    private final MessageEditRepository messageEdits;
    private final ConversationRepository conversations;
    private final MembershipService memberships;
    private final UserRepository users;
    private final IdempotencyService idempotency;
    private final MessageDtoAssembler assembler;
    private final ApplicationEventPublisher events;

    @Transactional
    public MessageDto send(UUID conversationId, UUID senderId, String body, UUID replyToId, String idempotencyKey) {
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body cannot be empty");
        }

        Optional<String> existingId = idempotency.reserve(senderId, idempotencyKey);
        if (existingId.isPresent() && !"PENDING".equals(existingId.get())) {
            UUID id = UUID.fromString(existingId.get());
            return messages.findById(id)
                    .map(m -> assembler.assembleOne(m, idempotencyKey))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency replay failed"));
        }

        Conversation c = conversations.findById(conversationId)
                .filter(cv -> cv.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (c.getType() != ConversationType.CHANNEL && !memberships.isActiveMember(conversationId, senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }

        User sender = users.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));

        Message msg = Message.builder()
                .conversation(c)
                .sender(sender)
                .type(MessageType.TEXT)
                .body(body)
                .replyToId(replyToId)
                .build();
        Message saved = messages.save(msg);
        c.setLastMessageAt(saved.getSentAt());

        MessageDto dto = assembler.assembleOne(saved, idempotencyKey);
        idempotency.store(senderId, idempotencyKey, saved.getId().toString());
        events.publishEvent(new MessageSentEvent(conversationId, dto));
        return dto;
    }

    @Transactional
    public MessageDto edit(UUID messageId, UUID editorId, boolean globalModPerm, String newBody) {
        if (newBody == null || newBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body cannot be empty");
        }
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Message is deleted");
        }

        boolean isAuthor = m.getSender() != null && m.getSender().getId().equals(editorId);
        boolean withinWindow = isAuthor && m.getSentAt().isAfter(Instant.now().minus(EDIT_WINDOW));
        boolean isConvMod = memberships.isConversationModerator(m.getConversation().getId(), editorId);
        if (!withinWindow && !globalModPerm && !isConvMod) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    isAuthor ? "Edit window has expired" : "Only the sender or a moderator can edit");
        }

        messageEdits.save(MessageEdit.builder()
                .messageId(m.getId())
                .editorId(editorId)
                .previousBody(m.getBody() == null ? "" : m.getBody())
                .build());

        m.setBody(newBody);
        Instant editedAt = Instant.now();
        m.setEditedAt(editedAt);

        events.publishEvent(new MessageEditedEvent(
                m.getConversation().getId(), m.getId(), editorId, newBody, editedAt));

        return assembler.assembleOne(m);
    }

    @Transactional
    public MessageDto deleteForEveryone(UUID messageId, UUID userId, boolean globalModPerm) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getDeletedAt() != null) {
            return assembler.assembleOne(m);
        }

        boolean isAuthor = m.getSender() != null && m.getSender().getId().equals(userId);
        boolean withinWindow = isAuthor && m.getSentAt().isAfter(Instant.now().minus(DELETE_FOR_EVERYONE_WINDOW));
        boolean isConvMod = memberships.isConversationModerator(m.getConversation().getId(), userId);
        if (!withinWindow && !globalModPerm && !isConvMod) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    isAuthor ? "Delete window has expired" : "Only the sender or a moderator can delete");
        }

        m.markDeleted(userId);
        events.publishEvent(new MessageDeletedEvent(
                m.getConversation().getId(), m.getId(), userId, m.getDeletedAt()));

        return assembler.assembleOne(m);
    }

    /** Moderator-side delete used by AdminModerationService (no time window, audit-logged separately). */
    @Transactional
    public void softDelete(UUID messageId) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getDeletedAt() != null) return;
        UUID actor = currentUserId();
        m.markDeleted(actor);
        events.publishEvent(new MessageDeletedEvent(
                m.getConversation().getId(), m.getId(), actor, m.getDeletedAt()));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}
