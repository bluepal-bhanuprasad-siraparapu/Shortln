package com.urlshortener.messaging;

import com.urlshortener.dto.LinkNotificationEvent;
import com.urlshortener.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final com.urlshortener.repository.NotificationRepository notificationRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @KafkaListener(topics = "link-expiry-notifications", groupId = "notification-group")
    public void consumeLinkExpiryNotification(LinkNotificationEvent event) {
        log.info("Received link expiry notification from Kafka for user: {}", event.getUserEmail());
        
        String expiryDateStr = event.getExpiresAt().format(formatter);
        
        // 1. Send Email
        emailService.sendLinkExpiryEmail(
                event.getUserEmail(),
                event.getUserName(),
                event.getLinkTitle(),
                event.getShortCode(),
                expiryDateStr
        );

        // 2. Persist In-App Notification
        if (event.getUserId() != null) {
            com.urlshortener.entity.Notification notification = com.urlshortener.entity.Notification.builder()
                    .userId(event.getUserId())
                    .message("Your link '" + event.getLinkTitle() + "' (/" + event.getShortCode() + ") is expiring soon on " + expiryDateStr)
                    .type("EXPIRY")
                    .build();
            notificationRepository.save(notification);
        }
    }
}
