package com.urlshortener.service;

import com.urlshortener.entity.ShortLink;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GeoIpService geoIpService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RedirectService redirectService;

    private ShortLink activeLink;

    @BeforeEach
    void setUp() {
        activeLink = ShortLink.builder()
                .id(1L)
                .userId(10L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(1)
                .build();
    }

    @Test
    void getOriginalUrl_CacheHit_ReturnsCachedUrl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shortlink:abc123")).thenReturn("https://example.com");

        String result = redirectService.getOriginalUrl("abc123");

        assertEquals("https://example.com", result);
        verify(shortLinkRepository, never()).findByShortCodeOrCustomAlias(any(), any());
    }

    @Test
    void getOriginalUrl_CacheMiss_FallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shortlink:abc123")).thenReturn(null);
        when(shortLinkRepository.findByShortCodeOrCustomAlias("abc123", "abc123"))
                .thenReturn(Optional.of(activeLink));
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        String result = redirectService.getOriginalUrl("abc123");

        assertEquals("https://example.com", result);
        verify(shortLinkRepository, times(1)).findByShortCodeOrCustomAlias("abc123", "abc123");
    }

    @Test
    void getOriginalUrl_LinkNotFound_ThrowsResourceNotFoundException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shortlink:notexist")).thenReturn(null);
        when(shortLinkRepository.findByShortCodeOrCustomAlias("notexist", "notexist"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> redirectService.getOriginalUrl("notexist"));
    }

    @Test
    void getOriginalUrl_ExpiredLink_ThrowsResourceNotFoundException() {
        activeLink.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shortlink:abc123")).thenReturn(null);
        when(shortLinkRepository.findByShortCodeOrCustomAlias("abc123", "abc123"))
                .thenReturn(Optional.of(activeLink));

        assertThrows(ResourceNotFoundException.class, () -> redirectService.getOriginalUrl("abc123"));
    }

    @Test
    void getOriginalUrl_InactiveLink_ThrowsResourceNotFoundException() {
        activeLink.setActive(0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shortlink:abc123")).thenReturn(null);
        when(shortLinkRepository.findByShortCodeOrCustomAlias("abc123", "abc123"))
                .thenReturn(Optional.of(activeLink));

        assertThrows(ResourceNotFoundException.class, () -> redirectService.getOriginalUrl("abc123"));
    }

    @Test
    void getOriginalUrl_RedisUnavailable_FallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        when(shortLinkRepository.findByShortCodeOrCustomAlias("abc123", "abc123"))
                .thenReturn(Optional.of(activeLink));

        String result = redirectService.getOriginalUrl("abc123");

        assertEquals("https://example.com", result);
    }

    @Test
    void trackClick_Success() {
        when(shortLinkRepository.findByShortCodeOrCustomAlias("abc123", "abc123"))
                .thenReturn(Optional.of(activeLink));
        when(geoIpService.getCityResponse("1.2.3.4")).thenReturn(null);

        redirectService.trackClick("abc123", "1.2.3.4", "Mozilla/5.0", null);

        verify(clickEventRepository, times(1)).save(any());
    }
}
