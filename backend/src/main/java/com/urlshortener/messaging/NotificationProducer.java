package com.urlshortener.messaging;

import com.urlshortener.dto.LinkNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "link-expiry-notifications";

    public void sendLinkExpiryNotification(LinkNotificationEvent event) {
        log.info("Sending link expiry notification to Kafka for user: {}", event.getUserEmail());
        kafkaTemplate.send(TOPIC, event);
    }
}
