package com.urlshortener.controller;

import com.urlshortener.dto.MessageResponse;
import com.urlshortener.dto.UserDto;
import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@Slf4j
@Tag(name = "Users", description = "Endpoints for user profile management and administrative user control")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user profile", description = "Returns profile details for a specific user ID. Users can only view their own profile unless they are admins.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> getProfile(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} requesting profile for ID {}", userDetails.getUsername(), id);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !userDetails.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(userService.getUserDtoById(id));
    }

    @Operation(summary = "Soft delete user", description = "Deactivates a user account by setting its active status to 0.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} requesting soft delete for ID {}", userDetails.getUsername(), id);
        if (!userDetails.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user profile", description = "Updates the profile information (username, bio, etc.) for a specific user.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserDto userDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} updating profile data for ID {}", userDetails.getUsername(), id);
        if (!userDetails.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(userService.updateUserProfile(id, userDto));
    }

    @Operation(summary = "Export all users PDF (Admin)", description = "Generates a PDF report containing details of all registered users.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/export/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportUsersPdf() {
        byte[] pdfContent = userService.generateUsersPdf();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(pdfContent);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_report.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .contentLength(pdfContent.length)
                .body(resource);
    }

    @Operation(summary = "Get all users (Admin)", description = "Returns a list of all registered users in the system.")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllProfiles() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Update all users status (Admin)", description = "Bulk updates the active status for all users in the system.")
    @PutMapping("/admin/status-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateAllStatus(@Parameter(description = "New active status (1: Active, 0: Inactive)") @RequestParam("active") int active) {
        userService.updateStatusForAllUsers(active);
        return ResponseEntity
                .ok(new MessageResponse("All users status updated to " + (active == 1 ? "active" : "inactive")));
    }

    @Operation(summary = "Update single user status (Admin)", description = "Updates the active status for a specific user ID.")
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateSingleStatus(@PathVariable("id") Long id, @Parameter(description = "New active status (1: Active, 0: Inactive)") @RequestParam("active") int active) {
        return ResponseEntity.ok(userService.updateUserStatus(id, active));
    }
}
