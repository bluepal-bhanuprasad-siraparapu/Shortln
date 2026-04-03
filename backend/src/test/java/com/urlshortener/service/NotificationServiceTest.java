package com.urlshortener.service;

import com.urlshortener.entity.Notification;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.repository.NotificationRepository;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private NotificationService notificationService;

    private User adminUser;
    private Notification unreadNotification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        unreadNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .message("Test notification")
                .type("SUBSCRIPTION")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        readNotification = Notification.builder()
                .id(2L)
                .userId(1L)
                .message("Old notification")
                .type("INFO")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- getUnreadCount ---

    @Test
    void getUnreadCount_ReturnsCountFromRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(3L, count);
    }

    @Test
    void getUnreadCount_NoUnread_ReturnsZero() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0L);

        assertEquals(0L, notificationService.getUnreadCount(1L));
    }

    // --- markAsRead ---

    @Test
    void markAsRead_ValidNotification_MarksAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(unreadNotification));

        notificationService.markAsRead(1L, 1L);

        assertTrue(unreadNotification.isRead());
        verify(notificationRepository, times(1)).save(unreadNotification);
    }

    @Test
    void markAsRead_WrongUser_DoesNotMarkAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(unreadNotification));

        // userId=99 is NOT the owner of notificationId=1 (which belongs to userId=1)
        notificationService.markAsRead(1L, 99L);

        assertFalse(unreadNotification.isRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NotificationNotFound_DoesNotThrow() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.markAsRead(999L, 1L));
        verify(notificationRepository, never()).save(any());
    }

    // --- markAllAsRead ---

    @Test
    void markAllAsRead_MarksAllUnreadForUser() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(unreadNotification, readNotification));

        notificationService.markAllAsRead(1L);

        assertTrue(unreadNotification.isRead());
        // readNotification was already read, verify it was also saved (it gets set to true again)
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_NoNotifications_DoesNotThrow() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> notificationService.markAllAsRead(1L));
        verify(notificationRepository, never()).save(any());
    }

    // --- notifyAdmins ---

    @Test
    void notifyAdmins_WithAdmins_CreatesNotificationForEach() {
        User admin2 = User.builder().id(2L).name("Admin2").email("admin2@example.com").role(Role.ADMIN).build();
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminUser, admin2));

        notificationService.notifyAdmins("PRO upgrade!", "SUBSCRIPTION");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertEquals("PRO upgrade!", saved.get(0).getMessage());
        assertEquals("SUBSCRIPTION", saved.get(0).getType());
        assertEquals("PRO upgrade!", saved.get(1).getMessage());
    }

    @Test
    void notifyAdmins_NoAdmins_SavesNothing() {
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());

        notificationService.notifyAdmins("An event occurred", "INFO");

        verify(notificationRepository, never()).save(any());
    }

    // --- deleteUserNotifications ---

    @Test
    void deleteUserNotifications_CallsRepositoryDelete() {
        notificationService.deleteUserNotifications(1L);

        verify(notificationRepository, times(1)).deleteByUserId(1L);
    }
}
