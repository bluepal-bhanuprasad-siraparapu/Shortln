package com.urlshortener.repository;

import com.urlshortener.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(CAST(:query AS text) IS NULL OR a.email ILIKE CAST(:query AS text) OR a.userName ILIKE CAST(:query AS text) OR a.details ILIKE CAST(:query AS text)) " +
           "AND (CAST(:action AS text) IS NULL OR a.action LIKE CAST(:action AS text)) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
        @Param("query") String query,
        @Param("action") String action,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(CAST(:query AS text) IS NULL OR a.email ILIKE CAST(:query AS text) OR a.userName ILIKE CAST(:query AS text) OR a.details ILIKE CAST(:query AS text)) " +
           "AND (CAST(:action AS text) IS NULL OR a.action LIKE CAST(:action AS text)) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> searchAuditLogsList(
        @Param("query") String query,
        @Param("action") String action,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
