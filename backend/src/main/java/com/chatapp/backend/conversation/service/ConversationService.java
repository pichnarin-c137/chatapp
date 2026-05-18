package com.chatapp.backend.conversation.service;
import com.chatapp.backend.conversation.repository.ConversationMemberRepository;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.entity.MemberRole;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.entity.ConversationMemberId;
import com.chatapp.backend.conversation.entity.ConversationMember;
import com.chatapp.backend.conversation.entity.Conversation;

import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.conversation.dto.ConversationDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.entity.UserProfile;
import com.chatapp.backend.user.repository.UserProfileRepository;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final MessageRepository messages;

    @Transactional(readOnly = true)
    public Conversation get(UUID id) {
        return conversations.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    @Transactional(readOnly = true)
    public List<ConversationMember> listForUser(UUID userId) {
        return members.findByUserIdAndLeftAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<Conversation> listPublicChannels() {
        return conversations.findAllChannels();
    }

    /**
     * For a DIRECT conversation, return the participant who isn't the viewer
     * as a fully-materialized DTO (so callers don't dereference lazy proxies
     * after the transaction closes). Returns null for non-DIRECTs.
     */
    @Transactional(readOnly = true)
    public ConversationDto.DmOther findDmOther(UUID conversationId, UUID viewerId) {
        return members.findByConversationId(conversationId).stream()
                .filter(m -> m.getLeftAt() == null)
                .map(ConversationMember::getUser)
                .filter(u -> u != null && !u.getId().equals(viewerId))
                .findFirst()
                .map(this::toDmOther)
                .orElse(null);
    }

    /**
     * Build the complete view for a single conversation, including DM counterpart
     * info when applicable. All proxy traversal happens inside the transaction.
     */
    @Transactional(readOnly = true)
    public ConversationDto getView(UUID id, UUID viewerId) {
        Conversation c = get(id);
        return ConversationDto.from(c, dmOtherFor(c, viewerId), lastMessageFor(c, viewerId));
    }

    /**
     * List everything visible to the viewer (their memberships + public channels)
     * as fully-materialized DTOs. Replaces the older two-step list-then-map pattern
     * that broke whenever lazy proxies were touched outside the service tx.
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> listVisibleAsDtos(UUID viewerId) {
        List<Conversation> all = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (ConversationMember m : members.findByUserIdAndLeftAtIsNull(viewerId)) {
            Conversation c = m.getConversation();
            if (c.getDeletedAt() != null) continue;
            if (seen.add(c.getId())) all.add(c);
        }
        for (Conversation c : conversations.findAllChannels()) {
            if (seen.add(c.getId())) all.add(c);
        }
        List<ConversationDto> out = new ArrayList<>(all.size());
        for (Conversation c : all) {
            out.add(ConversationDto.from(c, dmOtherFor(c, viewerId), lastMessageFor(c, viewerId)));
        }
        return out;
    }

    /**
     * Latest message visible to the viewer (skips delete-for-me and tombstones).
     * Returns null when the conversation is empty from the viewer's perspective.
     */
    @Transactional(readOnly = true)
    public ConversationDto.LastMessage lastMessageFor(Conversation c, UUID viewerId) {
        List<Message> latest = messages.findLatestPage(c.getId(), viewerId, PageRequest.of(0, 1));
        if (latest.isEmpty()) return null;
        Message m = latest.get(0);
        UUID senderId = m.getSender() == null ? null : m.getSender().getId();
        String username = m.getSender() == null ? "system" : m.getSender().getUsername();
        return new ConversationDto.LastMessage(
                m.getId(), senderId, username, m.getBody(), m.getSentAt(),
                m.getDeletedAt() != null);
    }

    private ConversationDto.DmOther dmOtherFor(Conversation c, UUID viewerId) {
        if (c.getType() != ConversationType.DIRECT) return null;
        return findDmOther(c.getId(), viewerId);
    }

    private ConversationDto.DmOther toDmOther(User u) {
        String avatar = profiles.findById(u.getId()).map(UserProfile::getAvatarUrl).orElse(null);
        return new ConversationDto.DmOther(u.getId(), u.getUsername(), avatar);
    }

    @Transactional
    public Conversation createGroup(String name, String topic, UUID creatorId) {
        User creator = users.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));
        Conversation c = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(name == null ? null : name.trim())
                .topic(topic == null ? null : topic.trim())
                .lastMessageAt(Instant.now())
                .build();
        conversations.save(c);
        ConversationMember owner = ConversationMember.builder()
                .id(new ConversationMemberId(c.getId(), creator.getId()))
                .conversation(c)
                .user(creator)
                .role(MemberRole.OWNER)
                .build();
        members.save(owner);
        return c;
    }

    /**
     * Get-or-create a DIRECT conversation between two users. Idempotent — two
     * calls with the same pair return the same conversation row.
     */
    @Transactional
    public Conversation getOrCreateDirect(UUID userAId, UUID userBId) {
        if (userAId.equals(userBId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a DM with yourself");
        }
        // Look for an existing DIRECT with exactly these two members.
        List<ConversationMember> aMemberships = members.findByUserIdAndLeftAtIsNull(userAId);
        for (ConversationMember m : aMemberships) {
            Conversation c = m.getConversation();
            if (c.getType() != ConversationType.DIRECT || c.getDeletedAt() != null) continue;
            if (members.countActive(c.getId()) == 2
                    && members.existsByConversationIdAndUserIdAndLeftAtIsNull(c.getId(), userBId)) {
                return c;
            }
        }
        User a = users.findById(userAId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User A not found"));
        User b = users.findById(userBId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User B not found"));
        Conversation c = Conversation.builder()
                .type(ConversationType.DIRECT)
                .lastMessageAt(Instant.now())
                .build();
        conversations.save(c);
        members.save(ConversationMember.builder()
                .id(new ConversationMemberId(c.getId(), a.getId()))
                .conversation(c).user(a).role(MemberRole.MEMBER).build());
        members.save(ConversationMember.builder()
                .id(new ConversationMemberId(c.getId(), b.getId()))
                .conversation(c).user(b).role(MemberRole.MEMBER).build());
        return c;
    }

    @Transactional
    public Conversation update(UUID id, String name, String topic) {
        Conversation c = get(id);
        if (c.getType() == ConversationType.DIRECT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rename a DM");
        }
        if (name != null) c.setName(name.trim());
        if (topic != null) c.setTopic(topic.trim());
        return c;
    }

    @Transactional
    public void softDelete(UUID id) {
        if (Conversation.LOBBY_ID.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the Lobby");
        }
        Conversation c = get(id);
        c.markDeleted(currentUserId());
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
