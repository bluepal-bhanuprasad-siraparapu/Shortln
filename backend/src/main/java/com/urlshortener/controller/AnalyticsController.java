package com.urlshortener.controller;

import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Analytics", description = "Endpoints for link performance tracking, geo-location data, and dashboard statistics")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

        private final AnalyticsService analyticsService;

        private boolean checkIsAdmin(UserDetailsImpl userDetails) {
                return userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        @Operation(summary = "Get overall link analytics", description = "Returns click counts and basic stats for a specific link")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/link/{id}/overall")
        public ResponseEntity<Map<String, Object>> getOverallAnalytics(@PathVariable("id") Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                log.info("User {} requesting overall analytics for link ID {}", userDetails.getUsername(), id);
                return ResponseEntity
                                .ok(analyticsService.getOverallAnalytics(id, userDetails.getId(),
                                                checkIsAdmin(userDetails)));
        }

        @Operation(summary = "Get geo-location analytics", description = "Returns click distribution by country for a specific link")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/link/{id}/geo")
        public ResponseEntity<Map<String, Long>> getGeoAnalytics(@PathVariable("id") Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                return ResponseEntity.ok(
                                analyticsService.getGeoAnalytics(id, userDetails.getId(), checkIsAdmin(userDetails)));
        }

        @Operation(summary = "Get device analytics", description = "Returns click distribution by device type and browser for a specific link")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/link/{id}/devices")
        public ResponseEntity<Map<String, Long>> getDeviceAnalytics(@PathVariable("id") Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                return ResponseEntity
                                .ok(analyticsService.getDeviceAnalytics(id, userDetails.getId(),
                                                checkIsAdmin(userDetails)));
        }

        @Operation(summary = "Get link click events", description = "Returns a paginated list of click events for a specific link with optional search filtering.")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/link/{id}/events")
        public ResponseEntity<Page<com.urlshortener.entity.ClickEvent>> getLinkEvents(@PathVariable("id") Long id,
                        @Parameter(description = "Search query for country, city, device, or browser") @RequestParam(required = false) String query,
                        @AuthenticationPrincipal UserDetailsImpl userDetails, Pageable pageable) {
                return ResponseEntity.ok(analyticsService.getLinkEvents(id, userDetails.getId(),
                                checkIsAdmin(userDetails), query, pageable));
        }

        @Operation(summary = "Get dashboard stats", description = "Returns aggregated statistics for the user's dashboard (total links, clicks, etc.)")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/stats")
        public ResponseEntity<Map<String, Object>> getDashboardStats(
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                return ResponseEntity.ok(analyticsService.getDashboardStats(userDetails.getId(), isAdmin));
        }

        @Operation(summary = "Get click history", description = "Returns a list of daily click counts for the user's links over the last 30 days.")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/history")
        public ResponseEntity<List<Map<String, Object>>> getClickHistory(
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                return ResponseEntity.ok(analyticsService.getClickHistory(userDetails.getId(), isAdmin));
        }

        @Operation(summary = "Export link analytics PDF", description = "Generates a detailed PDF report for a specific link. Pro feature.")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/link/{id}/export")
        public ResponseEntity<org.springframework.core.io.Resource> exportLinkAnalytics(@PathVariable("id") Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                if (!checkIsAdmin(userDetails)
                                && !com.urlshortener.entity.Plan.PRO.name().equals(userDetails.getPlan())) {
                        throw new org.springframework.security.access.AccessDeniedException(
                                        "Exporting analytics is a Pro feature. Please upgrade your plan.");
                }

                byte[] pdfContent = analyticsService.exportLinkAnalytics(id, userDetails.getId(),
                                checkIsAdmin(userDetails));
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                                pdfContent);

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=link_analytics_" + id + ".pdf")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .contentLength(pdfContent.length)
                                .body(resource);
        }

        @Operation(summary = "Export user analytics PDF", description = "Generates a comprehensive PDF report for all links owned by the user. Pro feature.")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
        @GetMapping("/export")
        public ResponseEntity<org.springframework.core.io.Resource> exportUserAnalytics(
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                log.info("User {} exporting overall user analytics PDF", userDetails.getUsername());
                if (!checkIsAdmin(userDetails)
                                && !com.urlshortener.entity.Plan.PRO.name().equals(userDetails.getPlan())) {
                        throw new org.springframework.security.access.AccessDeniedException(
                                        "Exporting analytics is a Pro feature. Please upgrade your plan.");
                }

                byte[] pdfContent = analyticsService.exportUserAnalytics(userDetails.getId());
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                                pdfContent);

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=analytics.pdf")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .contentLength(pdfContent.length)
                                .body(resource);
        }

        @Operation(summary = "Export all analytics (Admin)", description = "Generates a PDF report containing analytics for all users and links.")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin/export/all")
        public ResponseEntity<org.springframework.core.io.Resource> exportAllAnalytics() {
                byte[] pdfContent = analyticsService.exportAllAnalytics();
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                                pdfContent);

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=all_analytics.pdf")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .contentLength(pdfContent.length)
                                .body(resource);
        }

        @Operation(summary = "Get latest platform events (Admin)", description = "Returns a paginated list of the most recent click events across the entire platform.")
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin/latest")
        public ResponseEntity<Page<com.urlshortener.entity.ClickEvent>> getLatestEvents(
                        @Parameter(description = "Search query for country, city, etc.") @RequestParam(required = false) String query,
                        Pageable pageable) {
                return ResponseEntity.ok(analyticsService.getLatestClickEvents(query, pageable));
        }
}
