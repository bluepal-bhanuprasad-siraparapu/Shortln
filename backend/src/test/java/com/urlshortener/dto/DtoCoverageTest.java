package com.urlshortener.dto;

import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DtoCoverageTest {

    @Test
    void registerRequest_Coverage() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setPlan(Plan.FREE);

        assertEquals("Test", req.getName());
        assertEquals("test@example.com", req.getEmail());
        assertEquals(Plan.FREE, req.getPlan());
    }

    @Test
    void loginRequest_Coverage() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");

        assertEquals("test@example.com", req.getEmail());
    }

    @Test
    void linkRequest_Coverage() {
        LinkRequest req = new LinkRequest();
        req.setOriginalUrl("https://google.com");
        req.setCustomAlias("goog");
        req.setTitle("Title");
        req.setExpiresAt(LocalDateTime.now().plusDays(1));

        assertEquals("https://google.com", req.getOriginalUrl());
    }

    @Test
    void linkResponse_Coverage() {
        LinkResponse resp = new LinkResponse();
        resp.setId(1L);
        resp.setShortCode("abc");
        resp.setOriginalUrl("https://google.com");
        resp.setClickCount(10L);
        resp.setActive(1);

        assertEquals("abc", resp.getShortCode());
        assertEquals(10L, resp.getClickCount());
    }

    @Test
    void userDto_Coverage() {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setEmail("test@example.com");
        dto.setName("Name");
        dto.setRole(Role.USER);
        dto.setPlan("PRO");

        assertEquals("PRO", dto.getPlan());
        assertEquals(Role.USER, dto.getRole());
    }

    @Test
    void messageResponse_Coverage() {
        MessageResponse resp = new MessageResponse("Hello");
        assertEquals("Hello", resp.getMessage());
        resp.setMessage("World");
        assertEquals("World", resp.getMessage());
    }
}
