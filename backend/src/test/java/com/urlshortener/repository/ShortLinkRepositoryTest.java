package com.urlshortener.repository;

import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.ShortLink;
import com.urlshortener.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ShortLinkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private User user;
    private ShortLink link;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .role(Role.USER)
                .plan(Plan.FREE)
                .build();
        user = entityManager.persist(user);

        link = ShortLink.builder()
                .userId(user.getId())
                .shortCode("abc123")
                .originalUrl("https://example.com")
                .title("Example")
                .active(1)
                .createdAt(LocalDateTime.now())
                .build();
        link = entityManager.persist(link);
        entityManager.flush();
    }

    @Test
    void findByShortCode_ReturnsLink() {
        Optional<ShortLink> found = shortLinkRepository.findByShortCode("abc123");
        assertTrue(found.isPresent());
        assertEquals("https://example.com", found.get().getOriginalUrl());
    }

    @Test
    void searchUserLinks_WithQuery_ReturnsMatches() {
        Page<ShortLink> result = shortLinkRepository.searchUserLinks(
                user.getId(),
                "%example%",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void existsByShortCode_ReturnsTrue() {
        assertTrue(shortLinkRepository.existsByShortCode("abc123"));
        assertFalse(shortLinkRepository.existsByShortCode("nonexistent"));
    }

    @Test
    void countByUserId_ReturnsCorrectCount() {
        assertEquals(1, shortLinkRepository.countByUserId(user.getId()));
    }
}
