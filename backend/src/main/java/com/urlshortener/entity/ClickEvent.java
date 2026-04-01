package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "click_event_seq")
    @SequenceGenerator(name = "click_event_seq", sequenceName = "click_events_id_seq", initialValue = 5001, allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long shortLinkId;

    private String country;
    
    private String city;

    private String device;

    private String browser;

    @Column(length = 1000)
    private String referrer;

    @Column(nullable = false)
    private String ipHash;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime clickedAt;
}
