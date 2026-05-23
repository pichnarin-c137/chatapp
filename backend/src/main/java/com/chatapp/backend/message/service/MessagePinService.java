package com.chatapp.backend.message.service;

import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.message.dto.PinDto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.PinnedMessage;
import com.chatapp.backend.message.entity.PinnedMessageId;
import com.chatapp.backend.message.event.MessagePinnedEvent;
import com.chatapp.backend.message.event.MessageUnpinnedEvent;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.message.repository.PinnedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessagePinService {

    private final PinnedMessageRepository pins;
    private final MessageRepository messages;
    private final MembershipService memberships;
    private final MessageDtoAssembler assembler;
    private final ApplicationEventPublisher events;

    @Transactional
    public PinDto pin(UUID messageId, UUID userId, boolean globalPinPerm) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        UUID convId = m.getConversation().getId();
        requirePermitted(m.getConversation().getType(), convId, userId, globalPinPerm);

        PinnedMessageId id = new PinnedMessageId(convId, messageId);
        PinnedMessage existing = pins.findById(id).orElse(null);
        PinnedMessage pin = existing != null
                ? existing
                : pins.save(PinnedMessage.builder().id(id).pinnedBy(userId).build());

        PinDto dto = PinDto.of(pin, assembler.assembleOne(m));
        if (existing == null) {
            events.publishEvent(new MessagePinnedEvent(convId, messageId, userId, pin.getPinnedAt(), dto));
        }
        return dto;
    }

    @Transactional
    public void unpin(UUID messageId, UUID userId, boolean globalPinPerm) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        UUID convId = m.getConversation().getId();
        requirePermitted(m.getConversation().getType(), convId, userId, globalPinPerm);

        PinnedMessageId id = new PinnedMessageId(convId, messageId);
        if (!pins.existsById(id)) return;
        pins.deleteById(id);
        events.publishEvent(new MessageUnpinnedEvent(convId, messageId, userId));
    }

    @Transactional(readOnly = true)
    public List<PinDto> listFor(UUID conversationId, UUID viewerUserId) {
        if (!memberships.canRead(conversationId, viewerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        List<PinnedMessage> rows = pins.findByIdConversationIdOrderByPinnedAtDesc(conversationId);
        if (rows.isEmpty()) return List.of();
        List<UUID> ids = rows.stream().map(p -> p.getId().getMessageId()).toList();
        List<Message> msgs = messages.findAllById(ids);
        var dtos = assembler.assemble(msgs);
        var byId = new java.util.HashMap<UUID, com.chatapp.backend.message.dto.MessageDto>();
        for (int i = 0; i < msgs.size(); i++) byId.put(msgs.get(i).getId(), dtos.get(i));
        return rows.stream().map(p -> PinDto.of(p, byId.get(p.getId().getMessageId()))).toList();
    }

    /**
     * Pin authorization rules:
     *   - DIRECT / GROUP: any active member may pin/unpin (no hierarchy needed in
     *     small social spaces; matches WhatsApp / Telegram group behavior).
     *   - CHANNEL: only OWNER/MOD or users carrying the MESSAGE_PIN permission
     *     (channels are broadcast spaces — pinning is moderation).
     */
    private void requirePermitted(ConversationType type, UUID conversationId, UUID userId, boolean globalPinPerm) {
        if (globalPinPerm) return;
        if (memberships.isConversationModerator(conversationId, userId)) return;
        if (type == ConversationType.CHANNEL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only channel moderators can pin in a channel");
        }
        if (!memberships.isActiveMember(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }
    }
}
