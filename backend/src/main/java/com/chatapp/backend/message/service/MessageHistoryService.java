package com.chatapp.backend.message.service;

import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.dto.MessagePage;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.repository.MessageRepository;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Cursor-paginated history reads. Encodes (sentAt, id) into a base64 cursor
 * so the client can ask for messages strictly before (or after) a known anchor.
 */
@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    public enum Direction { BEFORE, AFTER }

    private final MessageRepository messages;
    private final MessageDtoAssembler assembler;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public MessagePage history(UUID conversationId, UUID userId, String cursor, int limit, Direction direction) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 200);
        Cursor c = decode(cursor);
        PageRequest page = PageRequest.of(0, effectiveLimit + 1);
        List<Message> rows;
        if (c.sentAt == null || c.id == null) {
            // No cursor: latest page (only meaningful with direction=BEFORE).
            rows = messages.findLatestPage(conversationId, userId, page);
        } else if (direction == Direction.AFTER) {
            rows = messages.findHistoryAfter(conversationId, userId, c.sentAt, c.id, page);
        } else {
            rows = messages.findHistoryBefore(conversationId, userId, c.sentAt, c.id, page);
        }

        boolean hasMore = rows.size() > effectiveLimit;
        if (hasMore) rows = rows.subList(0, effectiveLimit);

        List<MessageDto> items = assembler.assemble(rows);

        String nextCursor = null;
        if (hasMore && !rows.isEmpty()) {
            Message last = rows.get(rows.size() - 1);
            nextCursor = encode(new Cursor(last.getSentAt(), last.getId()));
        }
        return new MessagePage(items, nextCursor, hasMore);
    }

    private Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return new Cursor(null, null);
        try {
            byte[] raw = Base64.getUrlDecoder().decode(cursor);
            return mapper.readValue(raw, Cursor.class);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private String encode(Cursor c) {
        try {
            byte[] raw = mapper.writeValueAsBytes(c);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Cursor(@JsonProperty("sentAt") Instant sentAt, @JsonProperty("id") UUID id) {
        @JsonCreator
        public Cursor {}
    }
}
