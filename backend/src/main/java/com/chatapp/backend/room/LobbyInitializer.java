package com.chatapp.backend.room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LobbyInitializer {

    @Bean
    public ApplicationRunner ensureLobbyExists(ChatRoomRepository rooms) {
        return args -> {
            if (!rooms.existsById(RoomConstants.LOBBY_ID)) {
                ChatRoom lobby = ChatRoom.builder()
                        .id(RoomConstants.LOBBY_ID)
                        .name(RoomConstants.LOBBY_NAME)
                        .type(ChatRoom.Type.GROUP)
                        .build();
                rooms.save(lobby);
                log.info("Created default Lobby room: {}", RoomConstants.LOBBY_ID);
            }
        };
    }
}
