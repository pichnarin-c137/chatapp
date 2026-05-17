package com.chatapp.backend.admin.conversations.service;

import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminConversationService {

    private static final int PAGE_SIZE = 20;

    private final ConversationRepository conversations;

    @Transactional(readOnly = true)
    public Page<Conversation> list(String q, int page) {
        return conversations.adminSearch(q, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Conversation get(UUID id) {
        return conversations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    @Transactional
    public Conversation create(String name, String topic, ConversationType type) {
        Conversation c = Conversation.builder()
                .type(type == null ? ConversationType.GROUP : type)
                .name(name == null ? null : name.trim())
                .topic(topic == null ? null : topic.trim())
                .lastMessageAt(Instant.now())
                .build();
        return conversations.save(c);
    }

    @Transactional
    public Conversation update(UUID id, String name, String topic, ConversationType type) {
        Conversation c = get(id);
        if (name != null) c.setName(name.trim());
        if (topic != null) c.setTopic(topic.trim());
        if (type != null) c.setType(type);
        return c;
    }

    @Transactional
    public void softDelete(UUID id, Authentication current) {
        if (Conversation.LOBBY_ID.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the Lobby");
        }
        Conversation c = get(id);
        c.markDeleted(currentUserId(current));
    }

    private UUID currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}
