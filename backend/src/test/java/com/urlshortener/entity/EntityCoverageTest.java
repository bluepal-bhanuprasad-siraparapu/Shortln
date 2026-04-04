package com.urlshortener.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EntityCoverageTest {

    @Test
    void userMethods_Coverage() {
        User user = new User();
        user.setId(1L);
        user.setName("Test");
        user.setEmail("test@example.com");
        user.setPassword("pass");
        user.setRole(Role.USER);
        user.setPlan(Plan.FREE);
        user.setActive(1);
        user.setCreatedAt(LocalDateTime.now());

        assertEquals(1L, user.getId());
        assertEquals("Test", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertEquals(Plan.FREE, user.getPlan());
    }

    @Test
    void shortLinkMethods_Coverage() {
        ShortLink link = new ShortLink();
        link.setId(1L);
        link.setShortCode("abc");
        link.setOriginalUrl("https://google.com");
        link.setUserId(1L);
        link.setActive(1);
        link.setClickCount(10L);
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(LocalDateTime.now().plusDays(30));

        assertEquals("abc", link.getShortCode());
        assertEquals("https://google.com", link.getOriginalUrl());
        assertEquals(10L, link.getClickCount());
    }

    @Test
    void clickEventMethods_Coverage() {
        ClickEvent event = new ClickEvent();
        event.setId(1L);
        event.setShortLinkId(1L);
        event.setIpHash("hash");
        event.setCountry("India");
        event.setCity("Hyderabad");
        event.setDevice("Desktop");
        event.setBrowser("Chrome");
        event.setReferrer("ref");
        event.setClickedAt(LocalDateTime.now());

        assertEquals("India", event.getCountry());
        assertEquals("Hyderabad", event.getCity());
    }

    @Test
    void notificationMethods_Coverage() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setMessage("Test Message");
        notification.setType("INFO");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        assertEquals("Test Message", notification.getMessage());
        assertFalse(notification.isRead());
    }

    @Test
    void auditLogMethods_Coverage() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setAction("TEST_ACTION");
        log.setDetails("Details");
        log.setUserId(1L);
        log.setUserName("User");
        log.setEmail("user@example.com");
        log.setIpAddress("127.0.0.1");
        log.setCreatedAt(LocalDateTime.now());

        assertEquals("TEST_ACTION", log.getAction());
        assertEquals("User", log.getUserName());
    }
}
