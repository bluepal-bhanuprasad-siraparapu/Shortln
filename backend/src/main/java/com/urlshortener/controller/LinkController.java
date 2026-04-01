package com.urlshortener.controller;

import com.urlshortener.dto.LinkRequest;
import com.urlshortener.dto.LinkResponse;
import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Links", description = "Endpoints for creating, managing, and tracking short links")
@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @Operation(summary = "Create short link", description = "Generates a new short URL for a given target URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Link created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid URL or alias already taken")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody LinkRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} is creating a new short link for target URL: {}", userDetails.getUsername(), request.getOriginalUrl());
        LinkResponse response = linkService.createLink(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    private boolean checkIsAdmin(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Operation(summary = "Get user links", description = "Returns a paginated list of links owned by the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LinkResponse>> getUserLinks(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "Search query for title or URL") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by status (1: Active, 0: Inactive)") @RequestParam(required = false) Integer active,
            @Parameter(description = "Filter by creation start date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "Filter by creation end date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            Pageable pageable) {
        log.info("Fetching links for user {}", userDetails.getUsername());
        Page<LinkResponse> links = linkService.getUserLinks(userDetails.getId(), query, active, startDate, endDate, pageable);
        return ResponseEntity.ok(links);
    }

    @Operation(summary = "Get all links (Admin)", description = "Returns a paginated list of all short links across the entire platform. Admin only.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Page<LinkResponse>> getAllLinksForAdmin(
            @Parameter(description = "Search query for title or URL") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by status (1: Active, 0: Inactive)") @RequestParam(required = false) Integer active,
            @Parameter(description = "Filter by creation start date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @Parameter(description = "Filter by creation end date") @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            Pageable pageable) {
        log.info("Admin fetching all links across the platform");
        Page<LinkResponse> links = linkService.getAllLinks(query, active, startDate, endDate, pageable);
        return ResponseEntity.ok(links);
    }

    @Operation(summary = "Update short link", description = "Updates the target URL, title, or status of an existing short link.")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<LinkResponse> updateLink(@PathVariable("id") Long id,
            @Valid @RequestBody LinkRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} is updating link ID {}", userDetails.getUsername(), id);
        LinkResponse response = linkService.updateLink(id, request, userDetails.getId(), checkIsAdmin(userDetails));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get link details", description = "Returns the details of a specific short link by its ID.")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<LinkResponse> getLinkById(@PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("Fetching link ID: {} for user: {}", id, userDetails.getUsername());
        LinkResponse response = linkService.getLinkById(id, userDetails.getId(), checkIsAdmin(userDetails));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete short link", description = "Permanently removes a short link from the system.")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLink(@PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} is deleting link ID {}", userDetails.getUsername(), id);
        linkService.deleteLink(id, userDetails.getId(), checkIsAdmin(userDetails));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Toggle link status", description = "Activates or deactivates a short link.")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleLinkStatus(@PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} is toggling status for link ID {}", userDetails.getUsername(), id);
        linkService.toggleLinkStatus(id, userDetails.getId(), checkIsAdmin(userDetails));
        return ResponseEntity.ok().build();
    }
}
