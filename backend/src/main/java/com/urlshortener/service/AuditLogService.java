package com.urlshortener.service;

import com.urlshortener.entity.AuditLog;
import com.urlshortener.repository.AuditLogRepository;
import com.urlshortener.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String action, String details) {
        log(action, details, null, null, null);
    }

    public void log(String action, String details, Long userId, String email, String ipAddress) {
        try {
            String userName = null;
            // Auto-fill from security context if missing
            if (userId == null || email == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
                    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                    if (userId == null) userId = userDetails.getId();
                    if (email == null) email = userDetails.getEmail();
                    userName = userDetails.getName();
                }
            }

            // Auto-fill IP if missing
            if (ipAddress == null) {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    ipAddress = request.getRemoteAddr();
                    String xForwardedFor = request.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                        ipAddress = xForwardedFor.split(",")[0];
                    }
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .details(details)
                    .userId(userId)
                    .email(email)
                    .userName(userName)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("AuditLog saved: {} by {}", action, email != null ? email : "Anonymous");
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", action, e);
        }
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<AuditLog> getAllLogsForAdmin(String query, String action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim() + "%" : null;
        String actionPattern = (action != null && !action.equals("ALL")) ? action : null;
        return auditLogRepository.searchAuditLogs(searchPattern, actionPattern, startDate, endDate, pageable);
    }

    public byte[] generateAuditLogCsv(String query, String action, LocalDateTime startDate, LocalDateTime endDate) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim() + "%" : null;
        String actionPattern = (action != null && !action.equals("ALL")) ? action + "%" : null;
        List<AuditLog> logs = auditLogRepository.searchAuditLogsList(searchPattern, actionPattern, startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append("Log ID,Action,Details,User Name,User Email,Timestamp\n");
        for (AuditLog log : logs) {
            sb.append(log.getId()).append(",")
                    .append(escapeCsv(log.getAction())).append(",")
                    .append(escapeCsv(log.getDetails())).append(",")
                    .append(escapeCsv(log.getUserName() != null ? log.getUserName() : "SYSTEM")).append(",")
                    .append(escapeCsv(log.getEmail())).append(",")
                    .append(log.getCreatedAt()).append("\n");
        }
        return sb.toString().getBytes();
    }

    public byte[] generateAuditLogPdf(String query, String action, LocalDateTime startDate, LocalDateTime endDate) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim() + "%" : null;
        String actionPattern = (action != null && !action.equals("ALL")) ? action + "%" : null;
        List<AuditLog> logs = auditLogRepository.searchAuditLogsList(searchPattern, actionPattern, startDate, endDate);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Detailed Audit History Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 3f, 3f, 2.5f});

            String[] headers = {"ID", "Action", "Description", "User Identity", "Timestamp"};
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(new Color(11, 22, 41)); // brand-dark
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (AuditLog log : logs) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(log.getId()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(log.getAction(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(log.getDetails(), cellFont)));
                String userIdentity = (log.getUserName() != null ? log.getUserName() : "") + 
                                     (log.getEmail() != null ? " (" + log.getEmail() + ")" : "SYSTEM");
                table.addCell(new PdfPCell(new Phrase(userIdentity, cellFont)));
                table.addCell(new PdfPCell(new Phrase(log.getCreatedAt().toString(), cellFont)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Audit Log PDF", e);
            throw new RuntimeException("Error generating Audit Log PDF", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
