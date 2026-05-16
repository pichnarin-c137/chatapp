package com.chatapp.backend.dm;

import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserDto;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectConversationRepository conversations;
    private final DirectMessageRepository messages;
    private final UserRepository users;

    @Transactional
    public DirectConversation getOrCreateWith(User current, UUID otherUserId) {
        if (otherUserId.equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot DM yourself");
        }
        users.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID low = DirectConversation.low(current.getId(), otherUserId);
        UUID high = DirectConversation.high(current.getId(), otherUserId);

        return conversations.findByUserLowAndUserHigh(low, high)
                .orElseGet(() -> conversations.save(DirectConversation.builder()
                        .userLow(low)
                        .userHigh(high)
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<DirectConversationDto> listForUser(UUID userId) {
        List<DirectConversation> convs = conversations.findAllForUser(userId);
        if (convs.isEmpty()) return List.of();

        Set<UUID> peerIds = convs.stream()
                .map(c -> c.peerOf(userId))
                .collect(Collectors.toSet());
        Map<UUID, User> peerById = new HashMap<>();
        users.findAllById(peerIds).forEach(u -> peerById.put(u.getId(), u));

        List<DirectConversationDto> out = new ArrayList<>(convs.size());
        for (DirectConversation c : convs) {
            UUID peerId = c.peerOf(userId);
            User peer = peerById.get(peerId);
            if (peer == null) continue;

            DirectMessageDto lastDto = messages.findFirstByConversationIdOrderBySentAtDesc(c.getId())
                    .map(m -> {
                        String username = m.getSenderId().equals(peerId)
                                ? peer.getUsername()
                                : "you";
                        return DirectMessageDto.from(m, username);
                    })
                    .orElse(null);

            out.add(new DirectConversationDto(
                    c.getId(),
                    UserDto.from(peer),
                    c.getCreatedAt(),
                    c.getLastMessageAt(),
                    lastDto
            ));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DirectConversationDto loadForUser(UUID conversationId, UUID userId) {
        DirectConversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!c.includes(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
        }
        UUID peerId = c.peerOf(userId);
        User peer = users.findById(peerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peer not found"));

        DirectMessageDto lastDto = messages.findFirstByConversationIdOrderBySentAtDesc(c.getId())
                .map(m -> {
                    String username = m.getSenderId().equals(peerId)
                            ? peer.getUsername()
                            : "you";
                    return DirectMessageDto.from(m, username);
                })
                .orElse(null);

        return new DirectConversationDto(
                c.getId(),
                UserDto.from(peer),
                c.getCreatedAt(),
                c.getLastMessageAt(),
                lastDto
        );
    }

    @Transactional(readOnly = true)
    public List<DirectMessageDto> history(UUID conversationId, UUID userId, int page, int size) {
        DirectConversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!c.includes(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
        }

        Page<DirectMessage> result = messages.findByConversationIdOrderBySentAtDesc(
                conversationId, PageRequest.of(page, size));

        Set<UUID> senderIds = result.stream().map(DirectMessage::getSenderId).collect(Collectors.toSet());
        Map<UUID, String> usernames = new HashMap<>();
        users.findAllById(senderIds).forEach(u -> usernames.put(u.getId(), u.getUsername()));

        List<DirectMessageDto> reversed = new ArrayList<>(result.getNumberOfElements());
        result.forEach(m -> reversed.add(
                DirectMessageDto.from(m, usernames.getOrDefault(m.getSenderId(), "unknown"))
        ));
        Collections.reverse(reversed);
        return reversed;
    }

    @Transactional
    public DirectMessageDto saveAndTouch(UUID conversationId, User sender, String content, UUID replyTo) {
        DirectConversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!c.includes(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
        }

        DirectMessage saved = messages.save(DirectMessage.builder()
                .conversationId(conversationId)
                .senderId(sender.getId())
                .content(content)
                .replyTo(replyTo)
                .build());

        c.setLastMessageAt(saved.getSentAt() == null ? Instant.now() : saved.getSentAt());
        conversations.save(c);

        return DirectMessageDto.from(saved, sender.getUsername());
    }

    public UUID peerOf(UUID conversationId, UUID userId) {
        DirectConversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!c.includes(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
        }
        return c.peerOf(userId);
    }
}
