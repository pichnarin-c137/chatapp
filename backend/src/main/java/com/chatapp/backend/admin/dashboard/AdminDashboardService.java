package com.chatapp.backend.admin.dashboard;

import com.chatapp.backend.admin.moderation.MessageReport;
import com.chatapp.backend.admin.moderation.MessageReportRepository;
import com.chatapp.backend.message.MessageRepository;
import com.chatapp.backend.room.ChatRoomRepository;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository users;
    private final ChatRoomRepository rooms;
    private final MessageRepository messages;
    private final MessageReportRepository reports;

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        return new DashboardStats(
                users.count(),
                rooms.count(),
                messages.count(),
                reports.countByStatus(MessageReport.Status.OPEN)
        );
    }

    public record DashboardStats(long users, long rooms, long messages, long openReports) {}
}
