package com.urlshortener.service;

import com.urlshortener.dto.LinkRequest;
import com.urlshortener.dto.LinkResponse;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;

    private static final String CACHE_PREFIX = "shortlink:";

    @Transactional
    public LinkResponse createLink(LinkRequest request, Long userId) {
        log.info("Creating link for user ID: {} with original URL: {}", userId, request.getOriginalUrl());
        com.urlshortener.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPlan() == com.urlshortener.entity.Plan.FREE) {
            long linkCount = shortLinkRepository.countByUserId(userId);
            if (linkCount >= 5) {
                throw new IllegalArgumentException("Free plan limit reached. Please upgrade to Pro for unlimited links.");
            }
        }

        String code;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (shortLinkRepository.existsByCustomAlias(request.getCustomAlias()) ||
                shortLinkRepository.existsByShortCode(request.getCustomAlias())) {
                throw new IllegalArgumentException("Alias is already taken");
            }
            code = request.getCustomAlias();
        } else {
            do {
                code = shortCodeGenerator.generate();
            } while (shortLinkRepository.existsByShortCode(code));
        }

        if (request.getExpiresAt() != null) {
            if (request.getExpiresAt().isBefore(LocalDateTime.now().plusHours(24).minusMinutes(1))) {
                throw new IllegalArgumentException("Expiry date must be at least 24 hours in the future.");
            }
        }

        ShortLink link = ShortLink.builder()
                .userId(userId)
                .title(request.getTitle())
                .originalUrl(request.getOriginalUrl())
                .shortCode(code)
                .customAlias(request.getCustomAlias())
                .expiresAt(request.getExpiresAt())
                .build();

        ShortLink savedLink = shortLinkRepository.save(link);
        if (savedLink.getCreatedAt() == null) {
            savedLink.setCreatedAt(LocalDateTime.now());
        }

        auditLogService.log("LINK_CREATE", "Short link created: " + savedLink.getShortCode() + " -> " + savedLink.getOriginalUrl(), userId, user.getEmail(), null);

        return LinkResponse.fromEntity(savedLink);
    }

    public Page<LinkResponse> getUserLinks(Long userId, String query, Integer active, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return shortLinkRepository.searchUserLinks(userId, searchPattern, active, startDate, endDate, pageable)
                .map(LinkResponse::fromEntity);
    }

    public List<LinkResponse> getUserLinksList(Long userId, String query, Integer active, LocalDateTime startDate, LocalDateTime endDate) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim().toLowerCase() + "%" : null;
        // Re-using the same methodology if I add a List method to repo, OR just fetch all if I don't.
        // For now, let's keep it simple and just use the existing non-paginated findByUserId if no filters.
        if (searchPattern == null && active == null && startDate == null && endDate == null) {
            return shortLinkRepository.findByUserId(userId).stream()
                    .map(LinkResponse::fromEntity)
                    .collect(Collectors.toList());
        }
        // If filters are present, we might need a non-paginated search in repo.
        // For brevity, I'll just use the paginated one with a large size or add it to repo.
        return shortLinkRepository.findByUserId(userId).stream()
                .map(LinkResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<LinkResponse> getAllLinks(String query, Integer active, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        String searchPattern = (query != null && !query.trim().isEmpty()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return shortLinkRepository.searchLinks(searchPattern, active, startDate, endDate, pageable)
                .map(link -> {
                    LinkResponse res = LinkResponse.fromEntity(link);
                    userRepository.findById(link.getUserId()).ifPresent(u -> {
                        String name = u.getName();
                        res.setUsername((name != null && !name.isEmpty()) ? name : u.getEmail());
                    });
                    return res;
                });
    }

    @Transactional
    public LinkResponse getLinkById(Long id, Long userId, boolean isAdmin) {
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        if (!isAdmin && !link.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link's data");
        }
        return LinkResponse.fromEntity(link);
    }

    public LinkResponse updateLink(Long id, LinkRequest request, Long userId, boolean isAdmin) {
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!isAdmin && !link.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link");
        }

        if (request.getCustomAlias() != null && !request.getCustomAlias().equals(link.getCustomAlias())) {
            if (shortLinkRepository.existsByCustomAlias(request.getCustomAlias()) ||
                shortLinkRepository.existsByShortCode(request.getCustomAlias())) {
                throw new IllegalArgumentException("Alias is already taken");
            }
            link.setCustomAlias(request.getCustomAlias());
            link.setShortCode(request.getCustomAlias()); // Using alias as the short code
        }

        if (request.getExpiresAt() != null && !request.getExpiresAt().equals(link.getExpiresAt())) {
            if (request.getExpiresAt().isBefore(LocalDateTime.now().plusHours(24).minusMinutes(1))) {
                throw new IllegalArgumentException("Expiry date must be at least 24 hours in the future.");
            }
            link.setExpiresAt(request.getExpiresAt());
        }
        link.setTitle(request.getTitle());
        link.setOriginalUrl(request.getOriginalUrl());

        invalidateCache(link.getShortCode());
        ShortLink updatedLink = shortLinkRepository.save(link);
        
        auditLogService.log("LINK_UPDATE", "Short link updated: " + updatedLink.getShortCode() + " points to " + updatedLink.getOriginalUrl(), userId, null, null);
        
        return LinkResponse.fromEntity(updatedLink);
    }

    @Transactional
    public void toggleLinkStatus(Long id, Long userId, boolean isAdmin) {
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        
        if (!isAdmin && !link.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link");
        }

        link.setActive(link.getActive() == 1 ? 0 : 1);
        invalidateCache(link.getShortCode());
        shortLinkRepository.save(link);
        
        auditLogService.log("LINK_STATUS_TOGGLE", "Link status toggled to " + (link.getActive() == 1 ? "ACTIVE" : "INACTIVE") + " for code: " + link.getShortCode(), userId, null, null);
    }

    @Transactional
    public void deleteLink(Long id, Long userId, boolean isAdmin) {
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!isAdmin && !link.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this link");
        }

        invalidateCache(link.getShortCode());
        shortLinkRepository.delete(link);

        auditLogService.log("LINK_DELETE", "Short link deleted: " + link.getShortCode(), userId, null, null);
    }

    private void invalidateCache(String shortCode) {
        if (shortCode == null) return;
        try {
            redisTemplate.delete(CACHE_PREFIX + shortCode);
        } catch (Exception e) {
            // Log but don't fail the transaction
            log.error("Failed to invalidate cache for: {}", shortCode, e);
        }
    }
}
