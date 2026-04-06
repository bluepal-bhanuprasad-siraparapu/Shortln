package com.urlshortener.service;

import com.urlshortener.entity.AuditLog;
import com.urlshortener.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.urlshortener.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // --- log (action, details, userId, email, ipAddress) ---

    @Test
    void log_WithAllParams_SavesAuditLog() {
        auditLogService.log("LINK_CREATED", "User created a link", 1L, "user@example.com", "192.168.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("LINK_CREATED", saved.getAction());
        assertEquals("User created a link", saved.getDetails());
        assertEquals(1L, saved.getUserId());
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("192.168.0.1", saved.getIpAddress());
    }

    @Test
    void log_WithNullOptionalParams_StillSavesLog() {
        auditLogService.log("USER_LOGIN", "User logged in", null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("USER_LOGIN", saved.getAction());
        assertEquals("User logged in", saved.getDetails());
    }

    @Test
    void log_RepositoryThrowsException_DoesNotPropagate() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() ->
            auditLogService.log("LINK_CREATED", "Some details", 1L, "user@example.com", "127.0.0.1")
        );
    }

    // --- getAllLogs ---

    @Test
    void getAllLogs_ReturnsSortedListFromRepository() {
        AuditLog log1 = AuditLog.builder().id(1L).action("LOGIN").details("User logged in").build();
        AuditLog log2 = AuditLog.builder().id(2L).action("LINK_CREATED").details("Created link").build();
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log2, log1));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertEquals(2, result.size());
        assertEquals("LINK_CREATED", result.get(0).getAction());
        verify(auditLogRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllLogs_EmptyRepository_ReturnsEmptyList() {
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<AuditLog> result = auditLogService.getAllLogs();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- generateAuditLogCsv ---

    @Test
    void generateAuditLogCsv_WithLogs_ReturnsCsvBytes() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .action("LOGIN")
                .details("User logged in")
                .userName("John Doe")
                .email("john@example.com")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        when(auditLogRepository.searchAuditLogsList(any(), any(), any(), any())).thenReturn(List.of(log));

        byte[] result = auditLogService.generateAuditLogCsv(null, "ALL", null, null);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Log ID,Action,Details,User Name,User Email,Timestamp"));
        assertTrue(csvContent.contains("LOGIN"));
        assertTrue(csvContent.contains("john@example.com"));
    }

    @Test
    void generateAuditLogCsv_EmptyLogs_ReturnsHeaderOnly() {
        when(auditLogRepository.searchAuditLogsList(any(), any(), any(), any())).thenReturn(List.of());

        byte[] result = auditLogService.generateAuditLogCsv("query", "ALL", null, null);

        String csvContent = new String(result);
        assertTrue(csvContent.startsWith("Log ID,Action,Details,User Name,User Email,Timestamp"));
        assertEquals(1, csvContent.trim().split("\n").length);
    }

    @Test
    void generateAuditLogCsv_NullUserName_FallsBackToSystem() {
        AuditLog log = AuditLog.builder()
                .id(2L)
                .action("SYSTEM_EVENT")
                .details("Automated task ran")
                .userName(null)
                .email("admin@example.com")
                .createdAt(LocalDateTime.now())
                .build();
        when(auditLogRepository.searchAuditLogsList(any(), any(), any(), any())).thenReturn(List.of(log));

        byte[] result = auditLogService.generateAuditLogCsv(null, "ALL", null, null);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("SYSTEM"));
    }

    // --- getAllLogsForAdmin (paging) ---

    @Test
    void getAllLogsForAdmin_CallsRepositoryWithCorrectPatterns() {
        org.springframework.data.domain.Page<AuditLog> page = mock(org.springframework.data.domain.Page.class);
        org.springframework.data.domain.Pageable pageable = mock(org.springframework.data.domain.Pageable.class);
        
        when(auditLogRepository.searchAuditLogs(anyString(), anyString(), any(), any(), eq(pageable)))
                .thenReturn(page);

        // Test with real filters
        auditLogService.getAllLogsForAdmin("search query", "LOGIN", null, null, pageable);

        verify(auditLogRepository).searchAuditLogs(eq("%search query%"), eq("LOGIN"), any(), any(), eq(pageable));

        // Test with "ALL" action (should result in null pattern)
        auditLogService.getAllLogsForAdmin("", "ALL", null, null, pageable);
        verify(auditLogRepository).searchAuditLogs(isNull(), isNull(), any(), any(), eq(pageable));
    }

    // --- generateAuditLogPdf ---

    @Test
    void generateAuditLogPdf_ReturnsNonEmptyBytes() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .action("SECURITY_ALERT")
                .details("Multiple failed logins")
                .userName("Target User")
                .email("target@example.com")
                .createdAt(LocalDateTime.now())
                .build();
        when(auditLogRepository.searchAuditLogsList(any(), any(), any(), any())).thenReturn(List.of(log));

        byte[] result = auditLogService.generateAuditLogPdf(null, "ALL", null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Valid PDF header check: %PDF-
        assertEquals('%', (char)result[0]);
        assertEquals('P', (char)result[1]);
        assertEquals('D', (char)result[2]);
        assertEquals('F', (char)result[3]);
    }

    // --- log with static mocking ---

    @Test
    void log_AutoFillsFromSecurityContext() {
        // Mock SecurityContextHolder
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);

            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            
            when(userDetails.getId()).thenReturn(100L);
            when(userDetails.getEmail()).thenReturn("context@example.com");
            when(userDetails.getName()).thenReturn("Context User");

            // log(action, details, null, null, "1.1.1.1") -> triggers auto-fill for user
            auditLogService.log("LOGIN", "Success", null, null, "1.1.1.1");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            assertEquals(100L, saved.getUserId());
            assertEquals("context@example.com", saved.getEmail());
            assertEquals("Context User", saved.getUserName());
        }
    }

    @Test
    void log_AutoFillsIpFromRequestContext() {
         // Mock RequestContextHolder
        try (MockedStatic<RequestContextHolder> mockedRequest = mockStatic(RequestContextHolder.class)) {
            ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
            HttpServletRequest request = mock(HttpServletRequest.class);

            mockedRequest.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getRequest()).thenReturn(request);
            when(request.getRemoteAddr()).thenReturn("2.2.2.2");
            when(request.getHeader("X-Forwarded-For")).thenReturn("3.3.3.3, 4.4.4.4");

            // log(action, details, 1L, "e@e.com", null) -> triggers auto-fill for IP
            auditLogService.log("TEST", "IP Detection", 1L, "e@e.com", null);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();

            // X-Forwarded-For takes precedence, and first IP in list is used
            assertEquals("3.3.3.3", saved.getIpAddress());
        }
    }
}
