package com.chatapp.backend.admin.moderation.service;

import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.repository.MessageRepository;
import com.chatapp.backend.moderation.entity.MessageReport;
import com.chatapp.backend.moderation.repository.MessageReportRepository;
import com.chatapp.backend.moderation.entity.ReportStatus;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private static final int PAGE_SIZE = 20;

    private final MessageReportRepository reports;
    private final MessageRepository messages;
    private final UserRepository users;

    public record ReportRow(MessageReport report, Message message,
                            String reporterUsername, String senderUsername) {}

    @Transactional(readOnly = true)
    public Page<ReportRow> listOpen(int page) {
        Page<MessageReport> reportPage = reports.findByStatusOrderByCreatedAtDesc(
                ReportStatus.OPEN, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        return reportPage.map(r -> {
            Message m = r.getMessage();
            String senderUsername = (m != null && m.getSender() != null) ? m.getSender().getUsername() : "(deleted)";
            String reporterUsername = r.getReporter() != null ? r.getReporter().getUsername() : "(unknown)";
            return new ReportRow(r, m, reporterUsername, senderUsername);
        });
    }

    @Transactional
    public void deleteMessage(UUID messageId, Authentication auth) {
        Message m = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        m.markDeleted(currentUserId(auth));
        reports.resolveAllForMessage(messageId, Instant.now());
    }

    @Transactional
    public void resolveReport(UUID reportId, Authentication auth) {
        MessageReport r = reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (r.getStatus() == ReportStatus.OPEN) {
            r.setStatus(ReportStatus.DISMISSED);
            r.setResolvedAt(Instant.now());
            UUID actor = currentUserId(auth);
            if (actor != null) {
                users.findById(actor).ifPresent(r::setResolvedBy);
            }
        }
    }

    private UUID currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}
