package com.chatapp.backend.message.service;

import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.repository.ConversationRepository;
import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageForward;
import com.chatapp.backend.message.entity.MessageType;
import com.chatapp.backend.message.event.MessageSentEvent;
import com.chatapp.backend.message.repository.MessageForwardRepository;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageForwardService {

    /** Hard cap on targets per call — keeps a forward storm bounded. */
    private static final int MAX_TARGETS = 5;

    private final MessageRepository messages;
    private final MessageForwardRepository forwards;
    private final ConversationRepository conversations;
    private final MembershipService memberships;
    private final UserRepository users;
    private final MessageDtoAssembler assembler;
    private final ApplicationEventPublisher events;

    @Transactional
    public List<MessageDto> forward(UUID originalMessageId, UUID forwarderId, List<UUID> targetConversationIds) {
        if (targetConversationIds == null || targetConversationIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No target conversations");
        }
        if (targetConversationIds.size() > MAX_TARGETS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At most " + MAX_TARGETS + " targets per forward");
        }

        Message original = messages.findById(originalMessageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Original message not found"));
        if (original.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Cannot forward a deleted message");
        }

        UUID originalConvId = original.getConversation().getId();
        if (!memberships.canRead(originalConvId, forwarderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot read the original conversation");
        }

        User forwarder = users.findById(forwarderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Forwarder not found"));

        List<MessageDto> created = new ArrayList<>(targetConversationIds.size());

        for (UUID targetId : targetConversationIds) {
            Conversation target = conversations.findById(targetId)
                    .filter(c -> c.getDeletedAt() == null)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target conversation not found"));

            if (target.getType() != ConversationType.CHANNEL
                    && !memberships.isActiveMember(targetId, forwarderId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Not a member of target " + targetId);
            }

            UUID originalSenderId = original.getSender() == null ? null : original.getSender().getId();
            if (originalSenderId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot forward system messages");
            }

            Message copy = Message.builder()
                    .conversation(target)
                    .sender(forwarder)
                    .type(original.getType() == MessageType.SYSTEM ? MessageType.TEXT : original.getType())
                    .body(original.getBody())
                    .build();
            Message saved = messages.save(copy);
            target.setLastMessageAt(saved.getSentAt());

            forwards.save(MessageForward.builder()
                    .forwardedMessageId(saved.getId())
                    .originalMessageId(original.getId())
                    .originalSenderId(originalSenderId)
                    .originalConversationId(originalConvId)
                    .build());

            MessageDto dto = assembler.assembleOne(saved);
            events.publishEvent(new MessageSentEvent(targetId, dto));
            created.add(dto);
        }

        return created;
    }
}
