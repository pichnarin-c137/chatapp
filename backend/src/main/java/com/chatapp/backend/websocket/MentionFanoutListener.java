package com.chatapp.backend.websocket;

import com.chatapp.backend.message.dto.MentionDto;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * For each @mention in a freshly sent message, push a notification frame
 * to the mentioned user's personal queue. Subscribers should listen on
 * /user/queue/notifications (Spring rewrites that to the per-session
 * destination keyed by Principal.getName()).
 *
 * Principal.getName() resolves to the user's id (see JwtChannelInterceptor),
 * so we address by id.
 */
@Component
@RequiredArgsConstructor
public class MentionFanoutListener {

    private static final int PREVIEW_LEN = 160;

    private final SimpMessagingTemplate broker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        MessageDto msg = event.message();
        List<MentionDto> mentions = msg.mentions();
        if (mentions == null || mentions.isEmpty()) return;

        String preview = previewOf(msg.body());
        for (MentionDto mention : mentions) {
            if (mention.userId() == null) continue;
            // Don't notify someone who mentioned themselves.
            if (mention.userId().equals(msg.senderId())) continue;

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("event", "mention");
            envelope.put("convId", msg.conversationId());
            envelope.put("messageId", msg.id());
            envelope.put("fromUserId", msg.senderId());
            envelope.put("fromUsername", msg.senderUsername());
            envelope.put("preview", preview);
            broker.convertAndSendToUser(
                    mention.userId().toString(),
                    "/queue/notifications",
                    envelope);
        }
    }

    private static String previewOf(String body) {
        if (body == null) return "";
        String collapsed = body.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= PREVIEW_LEN ? collapsed : collapsed.substring(0, PREVIEW_LEN) + "…";
    }
}
