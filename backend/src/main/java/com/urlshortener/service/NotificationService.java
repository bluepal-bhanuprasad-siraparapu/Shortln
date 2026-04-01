package com.urlshortener.service;

import com.urlshortener.entity.Notification;
import com.urlshortener.entity.User;
import com.urlshortener.entity.Role;
import com.urlshortener.repository.NotificationRepository;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urlshortener.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<Notification> getAllNotificationsForAdmin(String query, String type, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        String q = (query != null && !query.trim().isEmpty()) ? "%" + query.trim().toLowerCase() + "%" : null;
        String t = (type != null && !type.trim().isEmpty() && !type.equals("ALL")) ? type.trim() : null;
        return notificationRepository.searchByAdmin(q, t, start, end, pageable);
    }

    public List<Notification> getAllNotificationsForAdminList(String query, String type, LocalDateTime start, LocalDateTime end) {
        String q = (query != null && !query.trim().isEmpty()) ? "%" + query.trim().toLowerCase() + "%" : null;
        String t = (type != null && !type.trim().isEmpty() && !type.equals("ALL")) ? type.trim() : null;
        return notificationRepository.searchByAdminList(q, t, start, end);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUserId().equals(userId)) {
                notification.setRead(true);
                notificationRepository.save(notification);
                auditLogService.log("NOTIFICATION_READ", "Notification " + notificationId + " marked as read");
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        unread.stream().filter(n -> !n.isRead()).forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        auditLogService.log("NOTIFICATION_READ_ALL", "All notifications marked as read for user " + userId);
    }

    @Transactional
    public void notifyAdmins(String message, String type) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (com.urlshortener.entity.User admin : admins) {
            Notification notification = Notification.builder()
                    .userId(admin.getId())
                    .message(message)
                    .type(type)
                    .build();
            notificationRepository.save(notification);
        }
        auditLogService.log("NOTIFICATION_CREATE", "Admin notification created: " + type);
    }

    public byte[] generateNotificationReportPdf(String query, String type, LocalDateTime start, LocalDateTime end) {
        List<Notification> notifications = getAllNotificationsForAdminList(query, type, start, end);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, out);

        document.open();
        com.lowagie.text.Font font = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD);
        font.setSize(18);

        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph("System Notifications Report", font);
        p.setAlignment(com.lowagie.text.Paragraph.ALIGN_CENTER);
        document.add(p);

        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] {1.0f, 4.0f, 2.0f, 3.0f});
        table.setSpacingBefore(10);

        table.addCell("User ID");
        table.addCell("Message");
        table.addCell("Type");
        table.addCell("Date");

        for (Notification n : notifications) {
            table.addCell(String.valueOf(n.getUserId()));
            table.addCell(n.getMessage());
            table.addCell(n.getType());
            table.addCell(n.getCreatedAt().toString());
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    public byte[] generateNotificationReportCsv(String query, String type, LocalDateTime start, LocalDateTime end) {
        List<Notification> notifications = getAllNotificationsForAdminList(query, type, start, end);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,User ID,Message,Type,Created At\n");
        for (Notification n : notifications) {
            sb.append(n.getId()).append(",")
              .append(n.getUserId()).append(",")
              .append("\"").append(n.getMessage().replace("\"", "\"\"")).append("\",")
              .append(n.getType()).append(",")
              .append(n.getCreatedAt()).append("\n");
        }
        return sb.toString().getBytes();
    }

    @Transactional
    public void deleteUserNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
