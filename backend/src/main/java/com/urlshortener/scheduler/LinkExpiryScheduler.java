package com.urlshortener.scheduler;

import com.urlshortener.dto.LinkNotificationEvent;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.entity.User;
import com.urlshortener.messaging.NotificationProducer;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkExpiryScheduler {

    private final ShortLinkRepository shortLinkRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    /**
     * Runs daily at 9:00 AM to check for links expiring at specific milestones.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkExpiringLinks() {
        log.info("Starting scheduled link expiry check at 9 AM...");
        
        LocalDateTime now = LocalDateTime.now();
        // Check for links expiring in the future (within 8 days to be safe)
        List<ShortLink> expiringLinks = shortLinkRepository.findByExpiresAtBefore(now.plusDays(8));
        
        log.info("Total potential expiring links to process: {}", expiringLinks.size());

        for (ShortLink link : expiringLinks) {
            try {
                long daysUntilExpiry = java.time.Duration.between(now, link.getExpiresAt()).toDays();
                int milestone = -1;

                if (daysUntilExpiry == 7) milestone = 7;
                else if (daysUntilExpiry == 3) milestone = 3;
                else if (daysUntilExpiry <= 0) milestone = 0;

                // Only send if we haven't sent for this milestone yet and it's a valid milestone
                if (milestone != -1 && (link.getLastExpiryMilestone() == null || link.getLastExpiryMilestone() > milestone)) {
                    User user = userRepository.findById(link.getUserId()).orElse(null);
                    if (user != null) {
                        String milestoneMsg = milestone == 0 ? "Your link is expiring today!" : 
                                             "Your link is expiring in " + milestone + " days.";
                        
                        LinkNotificationEvent event = LinkNotificationEvent.builder()
                                .userId(user.getId())
                                .userEmail(user.getEmail())
                                .userName(user.getName())
                                .linkTitle(link.getTitle() != null ? link.getTitle() : "Your Short Link")
                                .shortCode(link.getShortCode())
                                .originalUrl(link.getOriginalUrl())
                                .expiresAt(link.getExpiresAt())
                                .message(milestoneMsg)
                                .build();

                        notificationProducer.sendLinkExpiryNotification(event);
                        link.setLastExpiryMilestone(milestone);
                        shortLinkRepository.save(link);
                        
                        log.info("Sent alert for milestone {}d to user: {} for link: /{}", 
                                milestone, user.getEmail(), link.getShortCode());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process expiry notification for link ID: {}. Error: {}", link.getId(), e.getMessage());
            }
        }
        
        log.info("Scheduled link expiry check completed.");
    }
}
