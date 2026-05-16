package com.chatapp.backend.dm;

import com.chatapp.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageStompController {

    private final DirectMessageService service;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/dm.send")
    public void sendDirect(@Valid @Payload SendDirectMessagePayload payload, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User sender)) {
            log.warn("Rejecting unauthenticated dm.send");
            return;
        }
        if (payload.conversationId() == null
                || payload.content() == null
                || payload.content().isBlank()) {
            return;
        }
        try {
            DirectMessageDto saved = service.saveAndTouch(
                    payload.conversationId(),
                    sender,
                    payload.content().trim(),
                    payload.replyTo()
            );
            broker.convertAndSend("/topic/dm." + saved.conversationId(), saved);
        } catch (Exception e) {
            log.warn("Failed to deliver dm.send: {}", e.getMessage());
        }
    }
}
