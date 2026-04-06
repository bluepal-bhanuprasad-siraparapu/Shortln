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
    void createLink_FreePlanLimitReached_ThrowsException() {
        testUser.setPlan(Plan.FREE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(shortLinkRepository.countByUserId(1L)).thenReturn(5L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> linkService.createLink(linkRequest, 1L));
        assertTrue(exception.getMessage().contains("Free plan limit reached"));
    }

    @Test
    void updateLink_Success() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).shortCode("abc").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));
        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(link);

        LinkRequest updateReq = new LinkRequest();
        updateReq.setTitle("New Title");
        updateReq.setOriginalUrl("https://newurl.com");

        LinkResponse result = linkService.updateLink(1L, updateReq, 1L, false);

        assertEquals("New Title", result.getTitle());
        verify(shortLinkRepository).save(link);
    }

    @Test
    void updateLink_AccessDenied_ThrowsException() {
        ShortLink link = ShortLink.builder().id(1L).userId(2L).shortCode("abc").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> linkService.updateLink(1L, new LinkRequest(), 1L, false));
    }

    @Test
    void deleteLink_Success() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).shortCode("abc").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        linkService.deleteLink(1L, 1L, false);

        verify(shortLinkRepository).delete(link);
    }

    @Test
    void getUserLinks_Success() {
        org.springframework.data.domain.Page<ShortLink> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(
            ShortLink.builder().id(1L).userId(1L).shortCode("abc").build()
        ));
        when(shortLinkRepository.searchUserLinks(eq(1L), any(), any(), any(), any(), any())).thenReturn(page);

        org.springframework.data.domain.Page<LinkResponse> result = linkService.getUserLinks(1L, "query", 1, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllLinks_Success() {
        org.springframework.data.domain.Page<ShortLink> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(
            ShortLink.builder().id(1L).userId(1L).shortCode("abc").build()
        ));
        when(shortLinkRepository.searchLinks(any(), any(), any(), any(), any())).thenReturn(page);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        org.springframework.data.domain.Page<LinkResponse> result = linkService.getAllLinks("query", 1, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void createLink_CustomAliasTaken_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(shortLinkRepository.existsByCustomAlias("goog")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, 
            () -> linkService.createLink(linkRequest, 1L));
    }

    @Test
    void createLink_InvalidExpiry_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        linkRequest.setExpiresAt(LocalDateTime.now().plusHours(10)); // Less than 24h

        assertThrows(IllegalArgumentException.class, 
            () -> linkService.createLink(linkRequest, 1L));
    }

    @Test
    void updateLink_CustomAliasTaken_ThrowsException() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).shortCode("abc").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));
        when(shortLinkRepository.existsByCustomAlias("taken")).thenReturn(true);

        LinkRequest updateReq = new LinkRequest();
        updateReq.setCustomAlias("taken");

        assertThrows(IllegalArgumentException.class, 
            () -> linkService.updateLink(1L, updateReq, 1L, false));
    }

    @Test
    void toggleLinkStatus_Success() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).active(1).build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        linkService.toggleLinkStatus(1L, 1L, false);

        assertEquals(0, link.getActive());
        verify(shortLinkRepository).save(link);
    }

    @Test
    void getLinkById_Success() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        LinkResponse result = linkService.getLinkById(1L, 1L, false);

        assertNotNull(result);
        verify(shortLinkRepository).findById(1L);
    }

    @Test
    void getUserLinksList_Success() {
        when(shortLinkRepository.findByUserId(1L)).thenReturn(java.util.List.of(
            ShortLink.builder().id(1L).userId(1L).build()
        ));

        java.util.List<LinkResponse> result = linkService.getUserLinksList(1L, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getLinkById_NonOwner_ThrowsAccessDeniedException() {
        ShortLink link = ShortLink.builder().id(1L).userId(2L).build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> linkService.getLinkById(1L, 1L, false));
    }

    @Test
    void toggleLinkStatus_NonOwner_ThrowsAccessDeniedException() {
        ShortLink link = ShortLink.builder().id(1L).userId(2L).active(1).build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> linkService.toggleLinkStatus(1L, 1L, false));
    }

    @Test
    void invalidateCache_RedisError_DoesNotThrow() {
        ShortLink link = ShortLink.builder().id(1L).userId(1L).shortCode("abc").build();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));
        doThrow(new RuntimeException("Redis down")).when(redisTemplate).delete(anyString());

        // Should not throw exception
        assertDoesNotThrow(() -> linkService.deleteLink(1L, 1L, false));
    }

    // --- Additional Gaps ---

    @Test
    void createLink_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(com.urlshortener.exception.ResourceNotFoundException.class, 
            () -> linkService.createLink(linkRequest, 1L));
    }

    @Test
    void createLink_WithShortCodeCollision_RetriesGeneration() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        linkRequest.setCustomAlias(null); // Force random generation
        
        when(shortCodeGenerator.generate()).thenReturn("coll", "unique");
        when(shortLinkRepository.existsByShortCode("coll")).thenReturn(true);
        when(shortLinkRepository.existsByShortCode("unique")).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(ShortLink.builder().shortCode("unique").build());

        linkService.createLink(linkRequest, 1L);

        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void getAllLinks_WithNullUserName_UsesEmail() {
        org.springframework.data.domain.Page<ShortLink> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(
            ShortLink.builder().id(10L).userId(1L).shortCode("abc").build()
        ));
        when(shortLinkRepository.searchLinks(any(), any(), any(), any(), any())).thenReturn(page);
        
        User userWithNoName = User.builder().id(1L).email("noname@example.com").name(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithNoName));

        org.springframework.data.domain.Page<LinkResponse> result = linkService.getAllLinks(null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals("noname@example.com", result.getContent().get(0).getUsername());
    }

    @Test
    void updateLink_NotFound_ThrowsResourceNotFoundException() {
        when(shortLinkRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.urlshortener.exception.ResourceNotFoundException.class,
            () -> linkService.updateLink(99L, linkRequest, 1L, true));
    }

    @Test
    void invalidateCache_NullCode_ReturnsEarly() {
        // Mock deleteLink to call updateLink which calls invalidateCache
        ShortLink link = ShortLink.builder().id(1L).userId(1L).shortCode(null).build();
        when(shortLinkRepository.findById(10L)).thenReturn(Optional.of(link));
        
        linkService.deleteLink(10L, 1L, true);

        verify(redisTemplate, never()).delete(anyString());
    }
}
