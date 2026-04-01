package com.urlshortener.controller;

import com.urlshortener.dto.AuditLogDto;
import com.urlshortener.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Audit Logs", description = "Administrative endpoints for viewing and exporting platform audit trails")
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Get all audit logs (Admin)", description = "Returns a paginated list of system audit logs with optional filtering by search query, action type, and date range.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAllAuditLogs(
            @Parameter(description = "Search term for user name or email") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by specific action (e.g., LINK_CREATE, LOGIN)") @RequestParam(required = false) String action,
            @Parameter(description = "Start date for filtering logs") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "End date for filtering logs") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            Pageable pageable) {
        
        Page<AuditLogDto> dtos = auditLogService.getAllLogsForAdmin(query, action, startDate, endDate, pageable)
                .map(log -> AuditLogDto.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .email(log.getEmail())
                        .userName(log.getUserName())
                        .action(log.getAction())
                        .details(log.getDetails())
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Export audit logs", description = "Generates a CSV or PDF report of the audit logs based on the specified filters.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam String format,
            @Parameter(description = "Search term for user name or email") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by specific action") @RequestParam(required = false) String action,
            @Parameter(description = "Start date for filtering logs") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "End date for filtering logs") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        
        byte[] data;
        String contentType;
        String filename = "audit_log_report_" + java.time.LocalDate.now();

        if ("CSV".equalsIgnoreCase(format)) {
            data = auditLogService.generateAuditLogCsv(query, action, startDate, endDate);
            contentType = "text/csv";
            filename += ".csv";
        } else {
            data = auditLogService.generateAuditLogPdf(query, action, startDate, endDate);
            contentType = "application/pdf";
            filename += ".pdf";
        }

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(data);
    }
}
