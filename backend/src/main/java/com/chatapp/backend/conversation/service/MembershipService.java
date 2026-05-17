package com.chatapp.backend.conversation.service;
import com.chatapp.backend.conversation.repository.ConversationMemberRepository;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.entity.MemberRole;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.entity.ConversationMemberId;
import com.chatapp.backend.conversation.entity.ConversationMember;
import com.chatapp.backend.conversation.entity.Conversation;

import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final ConversationMemberRepository members;
    private final ConversationRepository conversations;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<ConversationMember> list(UUID conversationId) {
        return members.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID conversationId, UUID userId) {
        return members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId);
    }

    /** True if the user may read this conversation. CHANNELs are open to all authenticated users. */
    @Transactional(readOnly = true)
    public boolean canRead(UUID conversationId, UUID userId) {
        return conversations.findById(conversationId)
                .filter(c -> c.getDeletedAt() == null)
                .map(c -> c.getType() == ConversationType.CHANNEL || isActiveMember(conversationId, userId))
                .orElse(false);
    }

    @Transactional
    public ConversationMember add(UUID conversationId, UUID userId, MemberRole role) {
        Conversation c = conversations.findById(conversationId)
                .filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (c.getType() == ConversationType.DIRECT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add members to a DM");
        }
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return members.findByConversationIdAndUserId(conversationId, userId).map(existing -> {
            if (existing.getLeftAt() != null) existing.setLeftAt(null);  // re-joined
            if (role != null) existing.setRole(role);
            return existing;
        }).orElseGet(() -> {
            ConversationMember m = ConversationMember.builder()
                    .id(new ConversationMemberId(conversationId, userId))
                    .conversation(c)
                    .user(u)
                    .role(role == null ? MemberRole.MEMBER : role)
                    .build();
            return members.save(m);
        });
    }

    @Transactional
    public void remove(UUID conversationId, UUID userId) {
        ConversationMember m = members.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (m.getLeftAt() == null) m.setLeftAt(Instant.now());
    }

    @Transactional
    public void markRead(UUID conversationId, UUID userId, UUID lastReadMessageId) {
        ConversationMember m = members.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member"));
        m.setLastReadMessageId(lastReadMessageId);
        m.setLastReadAt(Instant.now());
    }
}
