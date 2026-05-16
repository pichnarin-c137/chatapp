package com.chatapp.backend.room;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final ChatRoomRepository rooms;

    @GetMapping("/lobby")
    public RoomDto lobby() {
        ChatRoom room = rooms.findById(RoomConstants.LOBBY_ID).orElseThrow();
        return RoomDto.from(room);
    }

    public record RoomDto(UUID id, String name, String type, Instant createdAt) {
        static RoomDto from(ChatRoom r) {
            return new RoomDto(r.getId(), r.getName(), r.getType().name(), r.getCreatedAt());
        }
    }
}
