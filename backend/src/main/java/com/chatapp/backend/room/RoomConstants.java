package com.chatapp.backend.room;

import java.util.UUID;

public final class RoomConstants {
    private RoomConstants() {}

    /** Deterministic UUID for the default "Lobby" room used in Phase 2 one-room chat. */
    public static final UUID LOBBY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String LOBBY_NAME = "Lobby";
}
