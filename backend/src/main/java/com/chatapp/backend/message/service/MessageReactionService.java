package com.chatapp.backend.message.service;

import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.message.dto.ReactionDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageReaction;
import com.chatapp.backend.message.entity.MessageReactionId;
import com.chatapp.backend.message.event.ReactionChangedEvent;
import com.chatapp.backend.message.repository.MessageReactionRepository;
import com.chatapp.backend.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private static final int EMOJI_MAX_LEN = 32;

    private final MessageReactionRepository reactions;
    private final MessageRepository messages;
    private final MembershipService memberships;
    private final ApplicationEventPublisher events;

    @Transactional
    public void add(UUID messageId, UUID userId, String emoji) {
        String normalized = normalize(emoji);
        Message m = requireVisibleMessage(messageId, userId);
        UUID convId = m.getConversation().getId();

        MessageReactionId id = new MessageReactionId(messageId, userId, normalized);
        if (reactions.existsById(id)) return; // idempotent — no second event

        try {
            reactions.save(MessageReaction.builder().id(id).build());
        } catch (DataIntegrityViolationException dup) {
            // racing concurrent add — silently accept.
            return;
        }
        events.publishEvent(new ReactionChangedEvent(
                convId, messageId, userId, normalized, ReactionChangedEvent.Action.ADD));
    }

    @Transactional
    public void remove(UUID messageId, UUID userId, String emoji) {
        String normalized = normalize(emoji);
        Message m = requireVisibleMessage(messageId, userId);
        UUID convId = m.getConversation().getId();

        int removed = reactions.deleteOne(messageId, userId, normalized);
        if (removed == 0) return;
        events.publishEvent(new ReactionChangedEvent(
                convId, messageId, userId, normalized, ReactionChangedEvent.Action.REMOVE));
    }

    /** Grouped reactions for a single message, ordered by first reactor's time. */
    @Transactional(readOnly = true)
    public List<ReactionDto> listForMessage(UUID messageId, UUID viewerId) {
        return groupReactions(reactions.findByMessageId(messageId), viewerId).getOrDefault(messageId, List.of());
    }

    /** Bulk variant used by the assembler. */
    @Transactional(readOnly = true)
    public Map<UUID, List<ReactionDto>> listForMessages(Collection<UUID> messageIds, UUID viewerId) {
        if (messageIds.isEmpty()) return Map.of();
        return groupReactions(reactions.findByMessageIdIn(messageIds), viewerId);
    }

    private Map<UUID, List<ReactionDto>> groupReactions(List<MessageReaction> rows, UUID viewerId) {
        // First level: messageId -> emoji -> list<userId> (insertion-ordered).
        Map<UUID, Map<String, List<UUID>>> grouped = new HashMap<>();
        for (MessageReaction r : rows) {
            UUID mid = r.getId().getMessageId();
            String emoji = r.getId().getEmoji();
            grouped
                    .computeIfAbsent(mid, k -> new LinkedHashMap<>())
                    .computeIfAbsent(emoji, k -> new ArrayList<>())
                    .add(r.getId().getUserId());
        }
        Map<UUID, List<ReactionDto>> out = new HashMap<>();
        for (Map.Entry<UUID, Map<String, List<UUID>>> entry : grouped.entrySet()) {
            UUID mid = entry.getKey();
            List<ReactionDto> buckets = new ArrayList<>();
            for (Map.Entry<String, List<UUID>> bucket : entry.getValue().entrySet()) {
                List<UUID> userIds = bucket.getValue();
                boolean mine = viewerId != null && userIds.contains(viewerId);
                buckets.add(new ReactionDto(mid, bucket.getKey(), userIds.size(),
                        Collections.unmodifiableList(userIds), mine));
            }
            out.put(mid, buckets);
        }
        return out;
    }

    private Message requireVisibleMessage(UUID messageId, UUID userId) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (m.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Message was deleted");
        }
        UUID convId = m.getConversation().getId();
        if (!memberships.isActiveMember(convId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }
        return m;
    }

    private String normalize(String emoji) {
        if (emoji == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji required");
        }
        String trimmed = emoji.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji required");
        }
        if (trimmed.length() > EMOJI_MAX_LEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji too long");
        }
        return trimmed;
    }
}
