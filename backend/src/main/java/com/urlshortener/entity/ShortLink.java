package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "short_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "short_link_seq")
    @SequenceGenerator(name = "short_link_seq", sequenceName = "short_links_id_seq", initialValue = 5001, allocationSize = 1)
    private Long id;
    
    private String title;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 50)
    private String shortCode;

    @Column(unique = true, length = 50)
    private String customAlias;

    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private int active = 1;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    private Integer lastExpiryMilestone = -1; // -1: none, 7: 7 days, 3: 3 days, 0: expiring today
}
