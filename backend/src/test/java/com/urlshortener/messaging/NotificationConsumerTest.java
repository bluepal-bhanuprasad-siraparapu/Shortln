package com.urlshortener.messaging;

import com.urlshortener.dto.LinkNotificationEvent;
import com.urlshortener.repository.NotificationRepository;
import com.urlshortener.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    private LinkNotificationEvent event;

    @BeforeEach
    void setUp() {
        event = LinkNotificationEvent.builder()
                .userId(1L)
                .userEmail("test@example.com")
                .userName("Test User")
                .linkTitle("My Link")
                .shortCode("abc12")
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
    }

    @Test
    void consumeLinkExpiryNotification_Success() {
        notificationConsumer.consumeLinkExpiryNotification(event);

        verify(emailService).sendLinkExpiryEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq("My Link"),
                eq("abc12"),
                anyString()
        );

        verify(notificationRepository).save(any(com.urlshortener.entity.Notification.class));
    }

    @Test
    void consumeLinkExpiryNotification_NoUserId_DoesNotSaveToRepo() {
        event.setUserId(null);
        
        notificationConsumer.consumeLinkExpiryNotification(event);

        verify(emailService).sendLinkExpiryEmail(any(), any(), any(), any(), any());
        verify(notificationRepository, never()).save(any());
    }
}
