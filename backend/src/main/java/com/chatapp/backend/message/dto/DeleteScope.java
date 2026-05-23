package com.chatapp.backend.message.dto;

public enum DeleteScope {
    /** Hide the message for the requesting user only. Not broadcast. */
    FOR_ME,
    /** Tombstone the message for everyone. Broadcasts message.deleted. */
    FOR_EVERYONE
}
