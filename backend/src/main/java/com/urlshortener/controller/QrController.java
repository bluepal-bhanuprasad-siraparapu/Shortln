package com.urlshortener.controller;

import com.urlshortener.entity.ShortLink;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.QrService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "QR Codes", description = "Endpoints for generating and downloading QR codes for short links")
@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;
    private final ShortLinkRepository shortLinkRepository;

    @Operation(summary = "Get link QR code", description = "Generates a PNG QR code image for a specific short link ID. Users can only access QR codes for their own links.")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                                            HttpServletRequest request) {
        log.info("User {} requesting QR code for link ID {}", userDetails.getUsername(), id);
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !link.getUserId().equals(userDetails.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link");
        }

        // Construct full short URL based on incoming request
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "");
        String shortUrl = baseUrl + "/" + link.getShortCode();

        try {
            byte[] image = qrService.generateQrCode(shortUrl, 250, 250);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-" + link.getShortCode() + ".png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
