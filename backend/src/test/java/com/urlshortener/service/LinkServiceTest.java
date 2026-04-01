package com.urlshortener.service;

import com.urlshortener.dto.LinkRequest;
import com.urlshortener.dto.LinkResponse;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.entity.User;
import com.urlshortener.entity.Plan;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LinkServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LinkService linkService;

    private User testUser;
    private LinkRequest linkRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .plan(Plan.PRO)
                .build();

        linkRequest = new LinkRequest();
        linkRequest.setOriginalUrl("https://google.com");
        linkRequest.setTitle("Google");
        linkRequest.setCustomAlias("goog");
    }

    @Test
    void createLink_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(shortLinkRepository.existsByCustomAlias("goog")).thenReturn(false);
        when(shortLinkRepository.existsByShortCode("goog")).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(i -> {
            ShortLink s = i.getArgument(0);
            s.setCreatedAt(LocalDateTime.now());
            return s;
        });

        LinkResponse result = linkService.createLink(linkRequest, 1L);

        assertNotNull(result);
        assertEquals("https://google.com", result.getOriginalUrl());
        assertEquals("goog", result.getShortCode());
        verify(shortLinkRepository, times(1)).save(any(ShortLink.class));
    }

    @Test
    void createLink_AliasAlreadyExists_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(shortLinkRepository.existsByCustomAlias("goog")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> linkService.createLink(linkRequest, 1L));
    }

    @Test
    void toggleLinkStatus_Success() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).active(1).shortCode("goog").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        linkService.toggleLinkStatus(1L, 1L, false);

        assertEquals(0, link.getActive());
        verify(shortLinkRepository, times(1)).save(link);
    }
}
