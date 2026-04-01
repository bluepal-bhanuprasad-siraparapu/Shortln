package com.urlshortener.controller;

import com.urlshortener.entity.Notification;
import com.urlshortener.repository.NotificationRepository;
import com.urlshortener.service.NotificationService;
import com.urlshortener.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@Tag(name = "Notifications", description = "Endpoints for user notifications, unread counts, and administrative notification management")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Operation(summary = "Get user notifications", description = "Returns a paginated list of notifications for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<Notification>> getUserNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userDetails.getId(), pageable));
    }

    @Operation(summary = "Get unread count", description = "Returns the number of unread notifications for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userDetails.getId()));
    }

    @Operation(summary = "Mark notification as read", description = "Updates the status of a specific notification to 'read'")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAsRead(id, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all notifications as read", description = "Updates the status of all unread notifications for the user to 'read'")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all notifications (Admin)", description = "Returns a paginated list of all system notifications for administrative monitoring.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<Page<Notification>> adminGetAllNotifications(
            @Parameter(description = "Search term for content or user") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by notification type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by creation start date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "Filter by creation end date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getAllNotificationsForAdmin(query, type, startDate, endDate, pageable));
    }

    @Operation(summary = "Export notification report", description = "Generates a CSV or PDF report of system notifications.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/export")
    public ResponseEntity<byte[]> exportNotifications(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam String format,
            @Parameter(description = "Search term") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type,
            @Parameter(description = "Start date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        
        byte[] data;
        String contentType;
        String filename;

        if ("CSV".equalsIgnoreCase(format)) {
            data = notificationService.generateNotificationReportCsv(query, type, startDate, endDate);
            contentType = "text/csv";
            filename = "notification_report.csv";
        } else {
            data = notificationService.generateNotificationReportPdf(query, type, startDate, endDate);
            contentType = "application/pdf";
            filename = "notification_report.pdf";
        }

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(data);
    }
}
