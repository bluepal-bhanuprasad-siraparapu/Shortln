package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class LinkRequest {
    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "^(https?|ftp|file|chrome-extension)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", message = "Must be a valid URL")
    private String originalUrl;

    private String title;

    private String customAlias;

    private LocalDateTime expiresAt;
}
