package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClickEventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClickEventRepository clickEventRepository;

    private ClickEvent event;

    @BeforeEach
    void setUp() {
        event = ClickEvent.builder()
                .shortLinkId(1L)
                .country("India")
                .city("Hyderabad")
                .device("Desktop")
                .browser("Chrome")
                .ipHash("hash123")
                .clickedAt(LocalDateTime.now())
                .build();
        event = entityManager.persist(event);
        entityManager.flush();
    }

    @Test
    void searchLinkEvents_WithQuery_ReturnsMatches() {
        Page<ClickEvent> result = clickEventRepository.searchLinkEvents(
                1L,
                "%india%",
                null,
                PageRequest.of(0, 10)
        );

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchEvents_ReturnsMatches() {
        Page<ClickEvent> result = clickEventRepository.searchEvents(
                "%india%",
                null,
                PageRequest.of(0, 10)
        );

        assertFalse(result.isEmpty());
    }
}
