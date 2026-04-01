package com.urlshortener.dto;

import com.urlshortener.entity.ShortLink;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LinkResponse {
    private Long id;
    private String title;
    private String originalUrl;
    private String shortCode;
    private String customAlias;
    private String shortUrl;
    private int active;
    private LocalDateTime expiresAt;
    private Long clickCount;
    private LocalDateTime createdAt;
    private String username;

    public static LinkResponse fromEntity(ShortLink link) {
        LinkResponse response = new LinkResponse();
        response.setId(link.getId());
        response.setTitle(link.getTitle());
        response.setOriginalUrl(link.getOriginalUrl());
        response.setShortCode(link.getShortCode());
        response.setCustomAlias(link.getCustomAlias());
        response.setActive(link.getActive());
        response.setExpiresAt(link.getExpiresAt());
        response.setClickCount(link.getClickCount());
        response.setCreatedAt(link.getCreatedAt());
        
        try {
            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            response.setShortUrl(baseUrl + "/" + (link.getCustomAlias() != null ? link.getCustomAlias() : link.getShortCode()));
        } catch (Exception e) {
            response.setShortUrl("http://localhost:8080/" + (link.getCustomAlias() != null ? link.getCustomAlias() : link.getShortCode()));
        }

        return response;
    }
}
