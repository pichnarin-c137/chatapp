package com.chatapp.backend.message.service;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.message.entity.MessageType;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.conversation.entity.ConversationType;

import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final MembershipService memberships;
    private final UserRepository users;

    @Transactional
    public MessageDto send(UUID conversationId, UUID senderId, String body, UUID replyToId) {
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body cannot be empty");
        }
        Conversation c = conversations.findById(conversationId)
                .filter(cv -> cv.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        // CHANNELs are open: any authenticated user can post. DM/GROUP require membership.
        if (c.getType() != com.chatapp.backend.conversation.entity.ConversationType.CHANNEL
                && !memberships.isActiveMember(conversationId, senderId)) {
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
        return MessageDto.from(saved, sender.getUsername());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> history(UUID conversationId, int page, int size) {
        Page<Message> result = messages.history(conversationId, PageRequest.of(page, size));
        Set<UUID> senderIds = result.stream()
                .map(m -> m.getSender() == null ? null : m.getSender().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> usernames = users.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        return result.stream()
                .map(m -> MessageDto.from(m,
                        m.getSender() == null
                                ? "system"
                                : usernames.getOrDefault(m.getSender().getId(), "unknown")))
                .toList();
    }

    @Transactional
    public void softDelete(UUID messageId) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        m.markDeleted(currentUserId());
    }

    @Transactional
    public MessageDto edit(UUID messageId, UUID editorId, String newBody) {
        if (newBody == null || newBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body cannot be empty");
        }
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getSender() == null || !m.getSender().getId().equals(editorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can edit");
        }
        m.setBody(newBody);
        m.setEditedAt(Instant.now());
        return MessageDto.from(m, m.getSender().getUsername());
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
