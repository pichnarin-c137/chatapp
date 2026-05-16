package com.chatapp.backend.message;

import com.chatapp.backend.user.User;
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
public class ChatStompController {

    private final MessageService messageService;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/group.send")
    public void sendGroup(@Payload SendMessagePayload payload, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User sender)) {
            log.warn("Rejecting unauthenticated group.send");
            return;
        }
        if (payload.roomId() == null || payload.content() == null || payload.content().isBlank()) {
            return;
        }

        MessageDto saved = messageService.save(
                payload.roomId(),
                sender.getId(),
                payload.content().trim(),
                payload.replyTo(),
                sender.getUsername()
        );
        broker.convertAndSend("/topic/group." + saved.roomId(), saved);
    }
}
