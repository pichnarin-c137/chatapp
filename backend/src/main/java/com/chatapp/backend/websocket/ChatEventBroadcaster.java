package com.chatapp.backend.websocket;

import com.chatapp.backend.message.event.MessageDeletedEvent;
import com.chatapp.backend.message.event.MessageEditedEvent;
import com.chatapp.backend.message.event.MessagePinnedEvent;
import com.chatapp.backend.message.event.MessageSeenEvent;
import com.chatapp.backend.message.event.MessageSentEvent;
import com.chatapp.backend.message.event.MessageUnpinnedEvent;
import com.chatapp.backend.message.event.ReactionChangedEvent;
import com.chatapp.backend.presence.event.PresenceChangedEvent;
import com.chatapp.backend.typing.event.TypingChangedEvent;
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
 * Listens for domain events and pushes STOMP envelopes to subscribers.
 * Persistent events use @TransactionalEventListener(AFTER_COMMIT) so we
 * never broadcast a write that subsequently rolled back. Ephemeral events
 * (typing, presence) use plain @EventListener.
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReactionChanged(ReactionChangedEvent e) {
        send(e.conversationId(), Map.of(
                "event", "reaction.changed",
                "convId", e.conversationId(),
                "messageId", e.messageId(),
                "userId", e.userId(),
                "emoji", e.emoji(),
                "action", e.action().name()
        ));
    }

    @EventListener
    public void onTypingChanged(TypingChangedEvent e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", e.action() == TypingChangedEvent.Action.START ? "typing.start" : "typing.stop");
        body.put("convId", e.conversationId());
        body.put("userId", e.userId());
        body.put("username", e.username());
        if (e.expiresAt() != null) body.put("expiresAt", e.expiresAt());
        broker.convertAndSend("/topic/conversations/" + e.conversationId() + "/typing", body);
    }

    @EventListener
    public void onPresenceChanged(PresenceChangedEvent e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "presence.update");
        body.put("userId", e.userId());
        body.put("status", e.presence().status().name());
        body.put("lastSeenAt", e.presence().lastSeenAt());
        broker.convertAndSend("/topic/users/" + e.userId() + "/presence", body);
    }

    private void send(UUID conversationId, Object payload) {
        broker.convertAndSend("/topic/conversations/" + conversationId, payload);
    }
}
