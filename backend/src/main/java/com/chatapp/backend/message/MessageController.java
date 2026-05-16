package com.chatapp.backend.message;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{roomId}/messages")
    public List<MessageDto> history(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<MessageDto> newestFirst = messageService.history(roomId, page, size);
        List<MessageDto> reversed = new java.util.ArrayList<>(newestFirst);
        Collections.reverse(reversed);
        return reversed;
    }
}
