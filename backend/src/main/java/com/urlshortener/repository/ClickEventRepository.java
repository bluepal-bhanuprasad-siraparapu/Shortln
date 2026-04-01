package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    Page<ClickEvent> findByShortLinkId(Long shortLinkId, Pageable pageable);
    List<ClickEvent> findByShortLinkId(Long shortLinkId);
    List<ClickEvent> findByShortLinkIdIn(List<Long> shortLinkIds);
    long countByShortLinkId(Long shortLinkId);
    List<ClickEvent> findFirst100ByOrderByClickedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ClickEvent c WHERE " +
           "(CAST(:query AS text) IS NULL OR LOWER(c.country) LIKE :query OR LOWER(c.city) LIKE :query OR LOWER(c.browser) LIKE :query OR LOWER(c.device) LIKE :query " +
           "OR CAST(c.id AS text) = :rawQuery OR CAST(c.shortLinkId AS text) = :rawQuery)")
    Page<ClickEvent> searchEvents(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("rawQuery") String rawQuery, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ClickEvent c WHERE c.shortLinkId = :linkId " +
           "AND (CAST(:query AS text) IS NULL OR LOWER(c.country) LIKE :query OR LOWER(c.city) LIKE :query OR LOWER(c.browser) LIKE :query OR LOWER(c.device) LIKE :query " +
           "OR CAST(c.id AS text) = :rawQuery)")
    Page<ClickEvent> searchLinkEvents(@org.springframework.data.repository.query.Param("linkId") Long linkId, @org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("rawQuery") String rawQuery, Pageable pageable);
}
