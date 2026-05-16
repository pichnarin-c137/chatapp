package com.chatapp.backend.message;

import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messages;
    private final UserRepository users;

    @Transactional
    public MessageDto save(UUID roomId, UUID senderId, String content, UUID replyTo, String senderUsername) {
        Message msg = Message.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .type(Message.Type.TEXT)
                .replyTo(replyTo)
                .build();
        Message saved = messages.save(msg);
        return MessageDto.from(saved, senderUsername);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> history(UUID roomId, int page, int size) {
        Page<Message> result = messages.findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(page, size));
        Set<UUID> senderIds = result.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<UUID, String> usernames = new HashMap<>();
        users.findAllById(senderIds).forEach(u -> usernames.put(u.getId(), u.getUsername()));
        return result.stream()
                .map(m -> MessageDto.from(m, usernames.getOrDefault(m.getSenderId(), "unknown")))
                .toList();
    }
}
