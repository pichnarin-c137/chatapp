package com.chatapp.backend.message.service;

import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageForward;
import com.chatapp.backend.message.repository.MessageForwardRepository;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bulk-builds MessageDtos with replyTo and forwardOf previews preloaded.
 * History queries can serve large pages without N+1 lookups: a single
 * pass collects all referenced ids, one shot per related table fetches
 * them, and DTOs assemble from the in-memory maps.
 */
@Service
@RequiredArgsConstructor
public class MessageDtoAssembler {

    private final MessageRepository messages;
    private final MessageForwardRepository forwards;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<MessageDto> assemble(List<Message> rows) {
        if (rows.isEmpty()) return List.of();

        // Senders for the rows themselves.
        Set<UUID> senderIds = rows.stream()
                .map(m -> m.getSender() == null ? null : m.getSender().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Reply parents.
        Set<UUID> replyParentIds = rows.stream()
                .map(Message::getReplyToId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Forwards keyed by the forwarded message id.
        Set<UUID> rowIds = rows.stream().map(Message::getId).collect(Collectors.toSet());
        Map<UUID, MessageForward> forwardByMsg = forwards.findByForwardedMessageIdIn(rowIds).stream()
                .collect(Collectors.toMap(MessageForward::getForwardedMessageId, f -> f));

        // Collect every user id we still need a username for.
        Set<UUID> userIds = new HashSet<>(senderIds);
        // Reply parents have their own senders.
        Map<UUID, Message> repliedTo = replyParentIds.isEmpty()
                ? Map.of()
                : messages.findAllById(replyParentIds).stream()
                    .collect(Collectors.toMap(Message::getId, m -> m));
        for (Message r : repliedTo.values()) {
            if (r.getSender() != null) userIds.add(r.getSender().getId());
        }
        for (MessageForward f : forwardByMsg.values()) {
            userIds.add(f.getOriginalSenderId());
        }

        Map<UUID, String> usernames = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return rows.stream().map(m -> assembleOne(m, usernames, repliedTo, forwardByMsg)).toList();
    }

    @Transactional(readOnly = true)
    public MessageDto assembleOne(Message m) {
        return assemble(List.of(m)).get(0);
    }

    @Transactional(readOnly = true)
    public MessageDto assembleOne(Message m, String idempotencyKey) {
        MessageDto base = assembleOne(m);
        return new MessageDto(
                base.id(), base.conversationId(), base.senderId(), base.senderUsername(),
                base.type(), base.body(), base.replyToId(),
                base.editedAt(), base.sentAt(), base.deletedAt(),
                base.replyTo(), base.forwardOf(), idempotencyKey);
    }

    private MessageDto assembleOne(Message m,
                                   Map<UUID, String> usernames,
                                   Map<UUID, Message> repliedTo,
                                   Map<UUID, MessageForward> forwardByMsg) {
        String senderUsername = m.getSender() == null
                ? "system"
                : usernames.getOrDefault(m.getSender().getId(), "unknown");

        MessageDto.ReplyPreview replyPreview = null;
        if (m.getReplyToId() != null) {
            Message parent = repliedTo.get(m.getReplyToId());
            if (parent != null) {
                UUID parentSenderId = parent.getSender() == null ? null : parent.getSender().getId();
                replyPreview = new MessageDto.ReplyPreview(
                        parent.getId(),
                        parentSenderId,
                        parentSenderId == null ? "system" : usernames.getOrDefault(parentSenderId, "unknown"),
                        parent.getDeletedAt() == null ? parent.getBody() : null,
                        parent.getDeletedAt() != null);
            }
        }

        MessageDto.ForwardInfo forwardInfo = null;
        MessageForward f = forwardByMsg.get(m.getId());
        if (f != null) {
            forwardInfo = new MessageDto.ForwardInfo(
                    f.getOriginalMessageId(),
                    f.getOriginalConversationId(),
                    f.getOriginalSenderId(),
                    usernames.getOrDefault(f.getOriginalSenderId(), "unknown"));
        }

        return MessageDto.from(m, senderUsername, replyPreview, forwardInfo, null);
    }

    /**
     * Convenience for callers that already know they're building a single
     * forwarded message (skips the bulk path).
     */
    public MessageDto.ForwardInfo previewForward(MessageForward f) {
        Optional<User> u = users.findById(f.getOriginalSenderId());
        return new MessageDto.ForwardInfo(
                f.getOriginalMessageId(),
                f.getOriginalConversationId(),
                f.getOriginalSenderId(),
                u.map(User::getUsername).orElse("unknown"));
    }

    public MessageDto.ReplyPreview previewReply(UUID replyToId) {
        if (replyToId == null) return null;
        Optional<Message> opt = messages.findById(replyToId);
        if (opt.isEmpty()) return null;
        Message parent = opt.get();
        UUID parentSenderId = parent.getSender() == null ? null : parent.getSender().getId();
        String username = parentSenderId == null ? "system"
                : users.findById(parentSenderId).map(User::getUsername).orElse("unknown");
        return new MessageDto.ReplyPreview(
                parent.getId(),
                parentSenderId,
                username,
                parent.getDeletedAt() == null ? parent.getBody() : null,
                parent.getDeletedAt() != null);
    }

    /** Resolve usernames for a small set in one query — utility for callers. */
    public Map<UUID, String> usernamesFor(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        users.findAllById(userIds).forEach(u -> out.put(u.getId(), u.getUsername()));
        return out;
    }
}
