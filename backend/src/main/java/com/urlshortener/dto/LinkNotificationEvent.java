package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkNotificationEvent {
    private Long userId;
    private String userEmail;
    private String userName;
    private String linkTitle;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime expiresAt;
    private String message;
}
