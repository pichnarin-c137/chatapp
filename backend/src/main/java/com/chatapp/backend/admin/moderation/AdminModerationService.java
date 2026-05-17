package com.chatapp.backend.admin.moderation;

import com.chatapp.backend.message.Message;
import com.chatapp.backend.message.MessageRepository;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private static final int PAGE_SIZE = 20;

    private final MessageReportRepository reports;
    private final MessageRepository messages;
    private final UserRepository users;

    public record ReportRow(
            MessageReport report,
            Message message,
            String reporterUsername,
            String senderUsername
    ) {}

    @Transactional(readOnly = true)
    public Page<ReportRow> listOpen(int page) {
        Page<MessageReport> reportPage = reports.findByStatusOrderByCreatedAtDesc(
                MessageReport.Status.OPEN, PageRequest.of(Math.max(page, 0), PAGE_SIZE));

        Set<UUID> messageIds = reportPage.stream().map(MessageReport::getMessageId).collect(Collectors.toSet());
        Map<UUID, Message> messagesById = messages.findAllById(messageIds).stream()
                .collect(Collectors.toMap(Message::getId, m -> m));

        Set<UUID> userIds = new HashSet<>();
        reportPage.forEach(r -> userIds.add(r.getReporterId()));
        messagesById.values().forEach(m -> userIds.add(m.getSenderId()));
        Map<UUID, String> usernames = users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return reportPage.map(r -> {
            Message m = messagesById.get(r.getMessageId());
            return new ReportRow(
                    r,
                    m,
                    usernames.getOrDefault(r.getReporterId(), "(unknown)"),
                    m == null ? "(deleted)" : usernames.getOrDefault(m.getSenderId(), "(unknown)")
            );
        });
    }

    @Transactional
    public void deleteMessage(UUID messageId) {
        if (!messages.existsById(messageId)) {
            // Already gone — still resolve any open reports for it.
            reports.resolveAllForMessage(messageId, Instant.now());
            return;
        }
        messages.deleteById(messageId);
        reports.resolveAllForMessage(messageId, Instant.now());
    }

    @Transactional
    public void resolveReport(UUID reportId) {
        MessageReport r = reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (r.getStatus() == MessageReport.Status.OPEN) {
            r.setStatus(MessageReport.Status.RESOLVED);
            r.setResolvedAt(Instant.now());
            reports.save(r);
        }
    }
}
