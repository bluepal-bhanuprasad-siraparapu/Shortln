package com.urlshortener.service;

import com.maxmind.geoip2.model.CityResponse;
import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final GeoIpService geoIpService;
    private final AuditLogService auditLogService;

    private final Parser uaParser = new Parser();
    private static final String CACHE_PREFIX = "shortlink:";
    private static final String SALT = "MyScretSalt123!@#";

    public String getOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Check Redis
        try {
            String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUrl != null) {
                return cachedUrl;
            }
        } catch (Exception e) {
            log.warn("Redis is unavailable, falling back to database for short code: {}", shortCode);
        }

        // 2. Fallback to DB
        ShortLink link = shortLinkRepository.findByShortCodeOrCustomAlias(shortCode, shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + shortCode));

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Invalidate cache if it somehow got in
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.warn("Failed to delete expired link from Redis for short code: {}", shortCode);
            }
            throw new ResourceNotFoundException("Link has expired");
        }

        if (link.getActive() != 1) {
            throw new ResourceNotFoundException("Link is inactive");
        }

        // 3. Populate Cache with appropriate TTL
        try {
            long ttlSeconds = 24 * 3600; // Default 24h
            if (link.getExpiresAt() != null) {
                long secondsUntilExpiry = java.time.Duration.between(LocalDateTime.now(), link.getExpiresAt()).getSeconds();
                if (secondsUntilExpiry > 0) {
                    ttlSeconds = Math.min(ttlSeconds, secondsUntilExpiry);
                } else {
                    // This should have been caught by the check above, but for safety:
                    throw new ResourceNotFoundException("Link has expired");
                }
            }
            redisTemplate.opsForValue().set(cacheKey, link.getOriginalUrl(), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to populate Redis cache for short code: {}", shortCode);
        }

        auditLogService.log("LINK_REDIRECT", "Visitor redirected via short code: " + shortCode + " to " + link.getOriginalUrl(), link.getUserId(), null, null);

        return link.getOriginalUrl();
    }

    @Async
    @Transactional
    public void trackClick(String shortCode, String ip, String userAgentString, String referrer) {
        try {
            log.info("Tracking click for short code: {} from IP: {}", shortCode, ip);
            ShortLink link = shortLinkRepository.findByShortCodeOrCustomAlias(shortCode, shortCode).orElse(null);
            if (link == null) {
                log.warn("Link not found for short code: {}", shortCode);
                return;
            }

            if (link.getActive() != 1) {
                log.info("Skipping tracking for inactive link: {}", shortCode);
                return;
            }

            log.info("Incrementing count for link ID: {}", link.getId());
            shortLinkRepository.incrementClickCount(link.getId());

            String hashIp = hashIp(ip);
            String country = null;
            String city = null;

            if (ip != null) {
                CityResponse cityResponse = geoIpService.getCityResponse(ip);
                if (cityResponse != null) {
                    if (cityResponse.getCountry() != null) country = cityResponse.getCountry().getName();
                    if (cityResponse.getCity() != null) city = cityResponse.getCity().getName();
                }
            }

            String device = "Unknown";
            String browser = "Unknown";

            if (userAgentString != null && !userAgentString.isEmpty()) {
                Client client = uaParser.parse(userAgentString);
                browser = client.userAgent.family;
                device = client.device.family;
            }

            ClickEvent event = ClickEvent.builder()
                    .shortLinkId(link.getId())
                    .ipHash(hashIp)
                    .country(country)
                    .city(city)
                    .device(device)
                    .browser(browser)
                    .referrer(referrer)
                    .build();

            clickEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to track click for short code: {}", shortCode, e);
        }
    }

    private String hashIp(String ip) {
        if (ip == null) return "unknown";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(SALT.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(ip.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return "error-hash";
        }
    }
}
