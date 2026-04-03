package com.urlshortener.service;

import com.urlshortener.entity.AuditLog;
import com.urlshortener.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
