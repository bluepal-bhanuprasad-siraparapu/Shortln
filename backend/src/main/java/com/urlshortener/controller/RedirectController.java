package com.urlshortener.controller;

import com.urlshortener.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

@Slf4j
@Tag(name = "Redirection", description = "The core engine for link redirection and click tracking")
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    @Operation(summary = "Redirect to original URL", description = "Accepts a short code and redirects the user to the associated original URL while tracking click analytics asynchronously.")
    @GetMapping("/{shortCode:(?!swagger-ui|v3|api-docs)[a-zA-Z0-9_-]+}")
    public void redirect(@Parameter(description = "The unique short code of the link") @PathVariable("shortCode") String shortCode, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("Incoming redirect request for shortCode: {}", shortCode);
        String originalUrl = redirectService.getOriginalUrl(shortCode);

        // Async tracking
        String ip = getClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String referrer = request.getHeader(HttpHeaders.REFERER);
        
        redirectService.trackClick(shortCode, ip, userAgent, referrer);

        response.sendRedirect(originalUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
            // taking the first IP if multiple exist
            if (remoteAddr != null && remoteAddr.contains(",")) {
                remoteAddr = remoteAddr.split(",")[0];
            }
        }
        return remoteAddr;
    }
}
