package com.urlshortener.service;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final ShortLinkRepository shortLinkRepository;
    private final com.urlshortener.repository.UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Map<String, Object> getOverallAnalytics(Long linkId, Long userId, boolean isAdmin) {
        log.info("Aggregating overall analytics for link ID: {}", linkId);
        verifyOwnership(linkId, userId, isAdmin);

        List<ClickEvent> events = clickEventRepository.findByShortLinkId(linkId);
        long totalClicks = events.size();
        long uniqueClicks = events.stream().map(ClickEvent::getIpHash).distinct().count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClicks", totalClicks);
        stats.put("uniqueClicks", uniqueClicks);
        return stats;
    }

    public Map<String, Long> getGeoAnalytics(Long linkId, Long userId, boolean isAdmin) {
        verifyOwnership(linkId, userId, isAdmin);

        List<ClickEvent> events = clickEventRepository.findByShortLinkId(linkId);
        return events.stream()
                .filter(e -> e.getCountry() != null)
                .collect(Collectors.groupingBy(ClickEvent::getCountry, Collectors.counting()));
    }

    public Map<String, Long> getDeviceAnalytics(Long linkId, Long userId, boolean isAdmin) {
        verifyOwnership(linkId, userId, isAdmin);

        List<ClickEvent> events = clickEventRepository.findByShortLinkId(linkId);
        return events.stream()
                .filter(e -> e.getDevice() != null)
                .collect(Collectors.groupingBy(ClickEvent::getDevice, Collectors.counting()));
    }

    public byte[] exportUserAnalytics(Long userId) {
        List<ShortLink> links = shortLinkRepository.findByUserId(userId);
        List<Long> linkIds = links.stream().map(ShortLink::getId).collect(Collectors.toList());
        List<ClickEvent> events = clickEventRepository.findByShortLinkIdIn(linkIds);
        byte[] pdf = generatePdf(events);
        auditLogService.log("ANALYTICS_REPORT_EXPORT", "Personal analytics report exported by user ID: " + userId, userId, null, null);
        return pdf;
    }

    public byte[] exportAllAnalytics() {
        List<ClickEvent> events = clickEventRepository.findAll();
        byte[] pdf = generatePdf(events);
        auditLogService.log("ANALYTICS_REPORT_EXPORT", "All analytics report exported by administrator", null, null, null);
        return pdf;
    }

    public byte[] exportLinkAnalytics(Long linkId, Long userId, boolean isAdmin) {
        verifyOwnership(linkId, userId, isAdmin);
        List<ClickEvent> events = clickEventRepository.findByShortLinkId(linkId);
        byte[] pdf = generatePdf(events);
        auditLogService.log("ANALYTICS_REPORT_EXPORT", "Link analytics report exported for link ID: " + linkId, userId, null, null);
        return pdf;
    }

    public Map<String, Object> getDashboardStats(Long userId, boolean isAdmin) {
        List<ShortLink> links;
        if (isAdmin) {
            links = shortLinkRepository.findAll();
        } else {
            links = shortLinkRepository.findByUserId(userId);
        }

        List<Long> linkIds = links.stream().map(ShortLink::getId).collect(Collectors.toList());
        if (linkIds.isEmpty()) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLinks", 0);
            stats.put("totalClicks", 0);
            stats.put("uniqueClicks", 0);
            stats.put("clicksToday", 0);
            return stats;
        }

        List<ClickEvent> events = clickEventRepository.findByShortLinkIdIn(linkIds);

        long totalClicks = events.size();
        long uniqueClicks = events.stream().map(ClickEvent::getIpHash).distinct().count();
        java.time.LocalDateTime today = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                .withNano(0);
        long clicksToday = events.stream()
                .filter(e -> e.getClickedAt().isAfter(today))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLinks", links.size());
        stats.put("totalClicks", totalClicks);
        stats.put("uniqueClicks", uniqueClicks);
        stats.put("clicksToday", clicksToday);
        return stats;
    }

    public List<Map<String, Object>> getClickHistory(Long userId, boolean isAdmin) {
        List<ShortLink> links;
        if (isAdmin) {
            links = shortLinkRepository.findAll();
        } else {
            links = shortLinkRepository.findByUserId(userId);
        }

        if (links.isEmpty()) {
            return List.of();
        }

        List<Long> linkIds = links.stream().map(ShortLink::getId).collect(Collectors.toList());
        List<ClickEvent> events = clickEventRepository.findByShortLinkIdIn(linkIds);

        java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);

        return events.stream()
                .filter(e -> e.getClickedAt().toLocalDate().isAfter(thirtyDaysAgo)
                        || e.getClickedAt().toLocalDate().isEqual(thirtyDaysAgo))
                .collect(Collectors.groupingBy(e -> e.getClickedAt().toLocalDate(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", entry.getKey().toString());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Page<ClickEvent> getLatestClickEvents(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return clickEventRepository.findAll(pageable);
        }
        String cleanedQuery = query.trim();
        String searchPattern = "%" + cleanedQuery.toLowerCase() + "%";
        return clickEventRepository.searchEvents(searchPattern, cleanedQuery, pageable);
    }

    public Page<ClickEvent> getLinkEvents(Long linkId, Long userId, boolean isAdmin, String query, Pageable pageable) {
        verifyOwnership(linkId, userId, isAdmin);
        if (query != null && !query.trim().isEmpty()) {
            String cleanedQuery = query.trim();
            return clickEventRepository.searchLinkEvents(linkId, "%" + cleanedQuery.toLowerCase() + "%", cleanedQuery, pageable);
        }
        return clickEventRepository.findByShortLinkId(linkId, pageable);
    }

    private byte[] generatePdf(List<ClickEvent> events) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Analytics Export Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 1.5f, 2f, 2f, 2f, 2f, 2f, 3f });

            String[] headers = { "Click ID", "Link ID", "Country", "City", "Device", "Browser", "Referrer",
                    "Timestamp" };
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(new Color(0, 123, 255)); // Brand variant
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (ClickEvent e : events) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(e.getId()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(e.getShortLinkId()), cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getCountry() != null ? e.getCountry() : "-", cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getCity() != null ? e.getCity() : "-", cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getDevice() != null ? e.getDevice() : "-", cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getBrowser() != null ? e.getBrowser() : "-", cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getReferrer() != null ? e.getReferrer() : "-", cellFont)));
                table.addCell(new PdfPCell(new Phrase(e.getClickedAt().toString(), cellFont)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private String generateCsv(List<ClickEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("Click ID,Link ID,Country,City,Device,Browser,Referrer,Timestamp\n");
        for (ClickEvent e : events) {
            sb.append(e.getId()).append(",")
                    .append(e.getShortLinkId()).append(",")
                    .append(escapeCsv(e.getCountry())).append(",")
                    .append(escapeCsv(e.getCity())).append(",")
                    .append(escapeCsv(e.getDevice())).append(",")
                    .append(escapeCsv(e.getBrowser())).append(",")
                    .append(escapeCsv(e.getReferrer())).append(",")
                    .append(e.getClickedAt()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void verifyOwnership(Long linkId, Long userId, boolean isAdmin) {
        if (isAdmin)
            return;

        com.urlshortener.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPlan() != com.urlshortener.entity.Plan.PRO) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Detailed analytics are a Pro feature. Please upgrade to access.");
        }

        ShortLink link = shortLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!link.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link's analytics");
        }
    }
}
