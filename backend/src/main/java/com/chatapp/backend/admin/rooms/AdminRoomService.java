package com.chatapp.backend.admin.rooms;

import com.chatapp.backend.room.ChatRoom;
import com.chatapp.backend.room.ChatRoomRepository;
import com.chatapp.backend.room.RoomConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoomService {

    private static final int PAGE_SIZE = 20;

    private final ChatRoomRepository rooms;

    @Transactional(readOnly = true)
    public Page<ChatRoom> list(int page) {
        return rooms.findAll(PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public ChatRoom get(UUID id) {
        return rooms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    @Transactional
    public ChatRoom create(String name, ChatRoom.Type type) {
        ChatRoom room = ChatRoom.builder()
                .name(name == null ? null : name.trim())
                .type(type == null ? ChatRoom.Type.GROUP : type)
                .build();
        return rooms.save(room);
    }

    @Transactional
    public ChatRoom update(UUID id, String name, ChatRoom.Type type) {
        ChatRoom room = get(id);
        room.setName(name == null ? null : name.trim());
        if (type != null) room.setType(type);
        return rooms.save(room);
    }

    @Transactional
    public void delete(UUID id) {
        if (RoomConstants.LOBBY_ID.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the default Lobby room");
        }
        if (!rooms.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        rooms.deleteById(id);
    }
}
