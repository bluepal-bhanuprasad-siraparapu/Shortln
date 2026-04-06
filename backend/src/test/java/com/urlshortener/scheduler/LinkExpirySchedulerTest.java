package com.urlshortener.scheduler;

import com.urlshortener.dto.LinkNotificationEvent;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.entity.User;
import com.urlshortener.messaging.NotificationProducer;
import com.urlshortener.repository.ShortLinkRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkExpirySchedulerTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private LinkExpiryScheduler linkExpiryScheduler;

    private User testUser;
    private ShortLink expiringLink;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        expiringLink = ShortLink.builder()
                .id(101L)
                .userId(1L)
                .shortCode("abc")
                .originalUrl("https://google.com")
                .title("My Link")
                .expiresAt(LocalDateTime.now().plusDays(7).plusHours(12)) 
                .build();
    }

    @Test
    void checkExpiringLinks_7DayMilestone_SendsNotification() {
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        linkExpiryScheduler.checkExpiringLinks();

        verify(notificationProducer, times(1)).sendLinkExpiryNotification(any(LinkNotificationEvent.class));
        verify(shortLinkRepository, times(1)).save(expiringLink);
        assertEquals(7, expiringLink.getLastExpiryMilestone());
    }

    @Test
    void checkExpiringLinks_3DayMilestone_SendsNotification() {
        expiringLink.setExpiresAt(LocalDateTime.now().plusDays(3).plusHours(12));
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        linkExpiryScheduler.checkExpiringLinks();

        verify(notificationProducer, times(1)).sendLinkExpiryNotification(any(LinkNotificationEvent.class));
        assertEquals(3, expiringLink.getLastExpiryMilestone());
    }

    @Test
    void checkExpiringLinks_TodayMilestone_SendsNotification() {
        expiringLink.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        linkExpiryScheduler.checkExpiringLinks();

        ArgumentCaptor<LinkNotificationEvent> captor = ArgumentCaptor.forClass(LinkNotificationEvent.class);
        verify(notificationProducer).sendLinkExpiryNotification(captor.capture());
        assertEquals("Your link is expiring today!", captor.getValue().getMessage());
        assertEquals(0, expiringLink.getLastExpiryMilestone());
    }

    @Test
    void checkExpiringLinks_AlreadySentForMilestone_DoesNothing() {
        expiringLink.setExpiresAt(LocalDateTime.now().plusDays(7).plusHours(12));
        expiringLink.setLastExpiryMilestone(7); 
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));

        linkExpiryScheduler.checkExpiringLinks();

        verify(notificationProducer, never()).sendLinkExpiryNotification(any());
    }

    @Test
    void checkExpiringLinks_UserNotFound_ContinuesWithoutSending() {
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.empty());

        linkExpiryScheduler.checkExpiringLinks();

        verify(notificationProducer, never()).sendLinkExpiryNotification(any());
    }

    @Test
    void checkExpiringLinks_Exception_HandleGracefully() {
        expiringLink.setExpiresAt(LocalDateTime.now().plusDays(7).plusHours(12));
        when(shortLinkRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiringLink));
        lenient().when(userRepository.findById(1L)).thenThrow(new RuntimeException("DB down"));

        linkExpiryScheduler.checkExpiringLinks();

        verify(notificationProducer, never()).sendLinkExpiryNotification(any());
    }
}
