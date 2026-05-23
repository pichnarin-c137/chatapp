package com.chatapp.backend.message.dto;

import java.util.List;

/** Cursor-paginated message slice. nextCursor is null when there's no more history. */
public record MessagePage(List<MessageDto> items, String nextCursor, boolean hasMore) {}
