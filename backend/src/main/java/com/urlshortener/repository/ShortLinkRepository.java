package com.urlshortener.repository;

import com.urlshortener.entity.ShortLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByShortCodeOrCustomAlias(String shortCode, String customAlias);
    
    Optional<ShortLink> findByShortCode(String shortCode);

    List<ShortLink> findByUserId(Long userId);

    Page<ShortLink> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT s FROM ShortLink s WHERE s.userId = :userId " +
           "AND (CAST(:query AS text) IS NULL OR LOWER(s.title) LIKE :query OR LOWER(s.shortCode) LIKE :query OR LOWER(s.originalUrl) LIKE :query) " +
           "AND (:active IS NULL OR s.active = :active) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR s.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR s.createdAt <= :endDate)")
    Page<ShortLink> searchUserLinks(
        @Param("userId") Long userId,
        @Param("query") String query,
        @Param("active") Integer active,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT s FROM ShortLink s WHERE " +
           "(CAST(:query AS text) IS NULL OR LOWER(s.title) LIKE :query OR LOWER(s.shortCode) LIKE :query OR LOWER(s.originalUrl) LIKE :query) " +
           "AND (:active IS NULL OR s.active = :active) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR s.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR s.createdAt <= :endDate)")
    Page<ShortLink> searchLinks(
        @Param("query") String query,
        @Param("active") Integer active,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    long countByUserId(Long userId);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    @Modifying
    @Query("UPDATE ShortLink s SET s.clickCount = s.clickCount + 1 WHERE s.id = :id")
    void incrementClickCount(@Param("id") Long id);

    List<ShortLink> findByExpiresAtBefore(LocalDateTime threshold);
}
