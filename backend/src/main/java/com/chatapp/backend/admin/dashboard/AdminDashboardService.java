package com.chatapp.backend.admin.dashboard;

import com.chatapp.backend.conversation.ConversationRepository;
import com.chatapp.backend.message.MessageRepository;
import com.chatapp.backend.moderation.MessageReportRepository;
import com.chatapp.backend.moderation.ReportStatus;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository users;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final MessageReportRepository reports;

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        return new DashboardStats(
                users.count(),
                conversations.countByDeletedAtIsNull(),
                messages.countByDeletedAtIsNull(),
                reports.countByStatus(ReportStatus.OPEN)
        );
    }

    public record DashboardStats(long users, long conversations, long messages, long openReports) {}
}
