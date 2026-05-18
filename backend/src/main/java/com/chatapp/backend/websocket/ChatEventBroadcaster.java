package com.chatapp.backend.websocket;

import com.chatapp.backend.message.event.MessageDeletedEvent;
import com.chatapp.backend.message.event.MessageEditedEvent;
import com.chatapp.backend.message.event.MessagePinnedEvent;
import com.chatapp.backend.message.event.MessageSeenEvent;
import com.chatapp.backend.message.event.MessageSentEvent;
import com.chatapp.backend.message.event.MessageUnpinnedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for domain events and pushes STOMP envelopes to /topic/conversations/{id}.
 * Using @TransactionalEventListener(AFTER_COMMIT) so we never broadcast a write
 * that subsequently rolled back.
 */
@Component
@RequiredArgsConstructor
public class ChatEventBroadcaster {

    private final SimpMessagingTemplate broker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent e) {
        send(e.conversationId(), Map.of(
                "event", "message.sent",
                "convId", e.conversationId(),
                "message", e.message()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageEdited(MessageEditedEvent e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "message.edited");
        body.put("convId", e.conversationId());
        body.put("messageId", e.messageId());
        body.put("body", e.body());
        body.put("editedAt", e.editedAt());
        body.put("editorId", e.editorId());
        send(e.conversationId(), body);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeletedEvent e) {
        send(e.conversationId(), Map.of(
                "event", "message.deleted",
                "convId", e.conversationId(),
                "messageId", e.messageId(),
                "deletedBy", e.deletedBy(),
                "deletedAt", e.deletedAt(),
                "scope", "EVERYONE"
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagePinned(MessagePinnedEvent e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "message.pinned");
        body.put("convId", e.conversationId());
        body.put("messageId", e.messageId());
        body.put("pinnedBy", e.pinnedBy());
        body.put("pinnedAt", e.pinnedAt());
        body.put("pin", e.pin());
        send(e.conversationId(), body);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageUnpinned(MessageUnpinnedEvent e) {
        send(e.conversationId(), Map.of(
                "event", "message.unpinned",
                "convId", e.conversationId(),
                "messageId", e.messageId(),
                "unpinnedBy", e.unpinnedBy()
        ));
    }

    @EventListener
    public void onMessageSeen(MessageSeenEvent e) {
        // Seen receipts may fire outside a transaction; use a plain listener.
        send(e.conversationId(), Map.of(
                "event", "message.seen",
                "convId", e.conversationId(),
                "userId", e.userId(),
                "lastSeenMessageId", e.lastSeenMessageId(),
                "seenAt", e.seenAt()
        ));
    }

    private void send(UUID conversationId, Object payload) {
        broker.convertAndSend("/topic/conversations/" + conversationId, payload);
    }
}
