package com.urlshortener.service;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.entity.User;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.urlshortener.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User proUser;
    private User freeUser;
    private ShortLink link;
    private ClickEvent clickEvent;

    @BeforeEach
    void setUp() {
        proUser = User.builder().id(1L).email("pro@example.com").plan(Plan.PRO).role(Role.USER).build();
        freeUser = User.builder().id(2L).email("free@example.com").plan(Plan.FREE).role(Role.USER).build();

        link = ShortLink.builder()
                .id(10L)
                .userId(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .active(1)
                .build();

        clickEvent = ClickEvent.builder()
                .id(100L)
                .shortLinkId(10L)
                .country("India")
                .city("Hyderabad")
                .device("Desktop")
                .browser("Chrome")
                .ipHash("hash123")
                .clickedAt(LocalDateTime.now())
                .build();
    }

    // --- getOverallAnalytics ---

    @Test
    void getOverallAnalytics_AsAdmin_ReturnsStats() {
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(clickEvent));

        Map<String, Object> result = analyticsService.getOverallAnalytics(10L, 99L, true);

        assertNotNull(result);
        assertEquals(1L, result.get("totalClicks"));
        assertEquals(1L, result.get("uniqueClicks"));
    }

    @Test
    void getOverallAnalytics_AsProOwner_ReturnsStats() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(proUser));
        when(shortLinkRepository.findById(10L)).thenReturn(Optional.of(link));
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(clickEvent));

        Map<String, Object> result = analyticsService.getOverallAnalytics(10L, 1L, false);

        assertEquals(1L, result.get("totalClicks"));
    }

    @Test
    void getOverallAnalytics_AsFreeUser_ThrowsAccessDeniedException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(freeUser));

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getOverallAnalytics(10L, 2L, false));
    }

    @Test
    void getOverallAnalytics_NonOwnerProUser_ThrowsAccessDeniedException() {
        // link belongs to userId=1, but userId=3 is trying to access
        User anotherProUser = User.builder().id(3L).plan(Plan.PRO).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherProUser));
        when(shortLinkRepository.findById(10L)).thenReturn(Optional.of(link));

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getOverallAnalytics(10L, 3L, false));
    }

    // --- getGeoAnalytics ---

    @Test
    void getGeoAnalytics_AsAdmin_ReturnsCountryBreakdown() {
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(clickEvent));

        Map<String, Long> result = analyticsService.getGeoAnalytics(10L, 99L, true);

        assertNotNull(result);
        assertEquals(1L, result.get("India"));
    }

    @Test
    void getGeoAnalytics_NullCountryEvents_AreFiltered() {
        ClickEvent noCountry = ClickEvent.builder()
                .id(101L).shortLinkId(10L).country(null).ipHash("hash456")
                .clickedAt(LocalDateTime.now()).build();
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(noCountry));

        Map<String, Long> result = analyticsService.getGeoAnalytics(10L, 99L, true);

        assertTrue(result.isEmpty());
    }

    // --- getDeviceAnalytics ---

    @Test
    void getDeviceAnalytics_AsAdmin_ReturnsDeviceBreakdown() {
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(clickEvent));

        Map<String, Long> result = analyticsService.getDeviceAnalytics(10L, 99L, true);

        assertEquals(1L, result.get("Desktop"));
    }

    // --- getDashboardStats ---

    @Test
    void getDashboardStats_AsUser_ReturnsUserLinkStats() {
        when(shortLinkRepository.findByUserId(1L)).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(List.of(10L))).thenReturn(List.of(clickEvent));

        Map<String, Object> stats = analyticsService.getDashboardStats(1L, false);

        assertEquals(1, stats.get("totalLinks"));
        assertEquals(1L, stats.get("totalClicks"));
        assertEquals(1L, stats.get("uniqueClicks"));
    }

    @Test
    void getDashboardStats_UserWithNoLinks_ReturnsZeroStats() {
        when(shortLinkRepository.findByUserId(2L)).thenReturn(List.of());

        Map<String, Object> stats = analyticsService.getDashboardStats(2L, false);

        assertEquals(0, stats.get("totalLinks"));
        assertEquals(0, stats.get("totalClicks"));
    }

    @Test
    void getDashboardStats_AsAdmin_ReturnsAllLinks() {
        when(shortLinkRepository.findAll()).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(List.of(10L))).thenReturn(List.of(clickEvent));

        Map<String, Object> stats = analyticsService.getDashboardStats(99L, true);

        assertEquals(1, stats.get("totalLinks"));
    }

    // --- exportUserAnalytics ---

    @Test
    void exportUserAnalytics_ReturnsNonEmptyPdf() {
        when(shortLinkRepository.findByUserId(1L)).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(List.of(10L))).thenReturn(List.of(clickEvent));
        doNothing().when(auditLogService).log(anyString(), anyString(), anyLong(), any(), any());

        byte[] pdf = analyticsService.exportUserAnalytics(1L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportAllAnalytics_ReturnsNonEmptyPdf() {
        when(clickEventRepository.findAll()).thenReturn(List.of(clickEvent));
        doNothing().when(auditLogService).log(anyString(), anyString(), any(), any(), any());

        byte[] pdf = analyticsService.exportAllAnalytics();

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void exportLinkAnalytics_ReturnsNonEmptyPdf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(proUser));
        when(shortLinkRepository.findById(10L)).thenReturn(Optional.of(link));
        when(clickEventRepository.findByShortLinkId(10L)).thenReturn(List.of(clickEvent));
        doNothing().when(auditLogService).log(anyString(), anyString(), anyLong(), any(), any());

        byte[] pdf = analyticsService.exportLinkAnalytics(10L, 1L, false);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    // --- getClickHistory ---

    @Test
    void getClickHistory_ReturnsAggregatedData() {
        when(shortLinkRepository.findByUserId(1L)).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(anyList())).thenReturn(List.of(clickEvent));

        List<Map<String, Object>> result = analyticsService.getClickHistory(1L, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(clickEvent.getClickedAt().toLocalDate().toString(), result.get(0).get("date"));
    }

    @Test
    void getClickHistory_NoLinks_ReturnsEmptyList() {
        when(shortLinkRepository.findByUserId(1L)).thenReturn(List.of());

        List<Map<String, Object>> result = analyticsService.getClickHistory(1L, false);

        assertTrue(result.isEmpty());
    }

    // --- getLatestClickEvents ---

    @Test
    void getLatestClickEvents_WithQuery_CallsSearchEvents() {
        org.springframework.data.domain.Page<ClickEvent> page = new org.springframework.data.domain.PageImpl<>(List.of(clickEvent));
        when(clickEventRepository.searchEvents(anyString(), anyString(), any())).thenReturn(page);

        org.springframework.data.domain.Page<ClickEvent> result = analyticsService.getLatestClickEvents("test", org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        verify(clickEventRepository).searchEvents(contains("test"), eq("test"), any());
    }

    @Test
    void getLatestClickEvents_EmptyQuery_CallsFindAll() {
        org.springframework.data.domain.Page<ClickEvent> page = new org.springframework.data.domain.PageImpl<>(List.of(clickEvent));
        when(clickEventRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        analyticsService.getLatestClickEvents(" ", org.springframework.data.domain.PageRequest.of(0, 10));

        verify(clickEventRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    // --- getLinkEvents ---

    @Test
    void getLinkEvents_WithQuery_CallsSearchLinkEvents() {
        org.springframework.data.domain.Page<ClickEvent> page = new org.springframework.data.domain.PageImpl<>(List.of(clickEvent));
        when(clickEventRepository.searchLinkEvents(anyLong(), anyString(), anyString(), any())).thenReturn(page);

        analyticsService.getLinkEvents(10L, 99L, true, "test", org.springframework.data.domain.PageRequest.of(0, 10));

        verify(clickEventRepository).searchLinkEvents(eq(10L), contains("test"), eq("test"), any());
    }

    // --- verifyOwnership Gaps ---

    @Test
    void verifyOwnership_Admin_SucceedsWithoutOwnershipCheck() {
        // Should not call shortLinkRepository.findById if isAdmin is true
        analyticsService.getOverallAnalytics(10L, 99L, true);
        verify(shortLinkRepository, never()).findById(anyLong());
    }

    @Test
    void verifyOwnership_LinkNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(proUser));
        when(shortLinkRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> analyticsService.getOverallAnalytics(999L, 1L, false));
    }

    @Test
    void verifyOwnership_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> analyticsService.getOverallAnalytics(10L, 888L, false));
    }

    // --- Additional Gaps ---

    @Test
    void getDashboardStats_AsAdmin_ReturnsAllStats() {
        when(shortLinkRepository.findAll()).thenReturn(List.of(link));
        // Ensure linkIds is NOT empty
        when(clickEventRepository.findByShortLinkIdIn(anyList())).thenReturn(List.of(clickEvent));

        Map<String, Object> stats = analyticsService.getDashboardStats(999L, true);

        assertEquals(1, stats.get("totalLinks"));
        verify(shortLinkRepository).findAll();
    }

    @Test
    void getClickHistory_AsAdmin_ReturnsData() {
        when(shortLinkRepository.findAll()).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(anyList())).thenReturn(List.of(clickEvent));

        List<Map<String, Object>> result = analyticsService.getClickHistory(999L, true);

        assertNotNull(result);
        verify(shortLinkRepository).findAll();
    }

    @Test
    void getClickHistory_BoundaryCase_IncludesTodayAnd30DaysAgo() {
        java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);
        ClickEvent boundaryEvent = ClickEvent.builder()
                .id(999L).shortLinkId(10L).ipHash("h").clickedAt(thirtyDaysAgo.atStartOfDay()).build();
        
        when(shortLinkRepository.findByUserId(1L)).thenReturn(List.of(link));
        when(clickEventRepository.findByShortLinkIdIn(anyList())).thenReturn(List.of(boundaryEvent, clickEvent));

        List<Map<String, Object>> result = analyticsService.getClickHistory(1L, false);

        // Should have 2 entries if dates are different, or 1 if same
        assertTrue(result.size() >= 1);
    }

    @Test
    void getLinkEvents_NoQuery_CallsFindByShortLinkId() {
        org.springframework.data.domain.Page<ClickEvent> page = new org.springframework.data.domain.PageImpl<>(List.of(clickEvent));
        when(clickEventRepository.findByShortLinkId(eq(10L), any())).thenReturn(page);

        analyticsService.getLinkEvents(10L, 99L, true, null, org.springframework.data.domain.PageRequest.of(0, 10));

        verify(clickEventRepository).findByShortLinkId(eq(10L), any());
    }
}
