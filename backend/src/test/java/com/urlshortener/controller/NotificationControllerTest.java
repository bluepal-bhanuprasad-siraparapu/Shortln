package com.urlshortener.controller;

import com.urlshortener.entity.Notification;
import com.urlshortener.repository.NotificationRepository;
import com.urlshortener.security.JwtAuthenticationFilter;
import com.urlshortener.security.TestSecurityUtils;
import com.urlshortener.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clear();
    }

    @Test
    void getUserNotifications() throws Exception {
        TestSecurityUtils.setupMockUser();
        Notification notification = Notification.builder()
                .id(1L)
                .message("Test Message")
                .isRead(false)
                .build();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationService.getUserNotifications(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value("Test Message"));
    }

    @Test
    void getUnreadCount() throws Exception {
        TestSecurityUtils.setupMockUser();
        when(notificationService.getUnreadCount(any())).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void markAsRead() throws Exception {
        TestSecurityUtils.setupMockUser();
        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk());
                
        verify(notificationService).markAsRead(eq(1L), any());
    }

    @Test
    void markAllAsRead() throws Exception {
        TestSecurityUtils.setupMockUser();
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk());
                
        verify(notificationService).markAllAsRead(any());
    }

    @Test
    void adminGetAllNotifications() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        Notification notification = Notification.builder().message("Admin notification").build();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        
        when(notificationService.getAllNotificationsForAdmin(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/notifications/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value("Admin notification"));
    }

    @Test
    void exportNotificationsCsv() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(notificationService.generateNotificationReportCsv(any(), any(), any(), any()))
                .thenReturn("csv,data".getBytes());

        mockMvc.perform(get("/api/notifications/admin/export").param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notification_report.csv"))
                .andExpect(content().bytes("csv,data".getBytes()));
    }

    @Test
    void exportNotificationsPdf() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(notificationService.generateNotificationReportPdf(any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/notifications/admin/export").param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notification_report.pdf"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}
