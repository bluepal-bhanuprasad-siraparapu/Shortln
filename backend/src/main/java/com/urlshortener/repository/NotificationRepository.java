package com.urlshortener.repository;

import com.urlshortener.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Notification> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT n FROM Notification n, User u WHERE n.userId = u.id " +
           "AND (CAST(:query AS text) IS NULL OR LOWER(u.email) LIKE :query OR LOWER(u.name) LIKE :query) " +
           "AND (CAST(:type AS text) IS NULL OR n.type = :type) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR n.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR n.createdAt <= :endDate) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchByAdmin(
        @Param("query") String query,
        @Param("type") String type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT n FROM Notification n, User u WHERE n.userId = u.id " +
           "AND (CAST(:query AS text) IS NULL OR LOWER(u.email) LIKE :query OR LOWER(u.name) LIKE :query) " +
           "AND (CAST(:type AS text) IS NULL OR n.type = :type) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR n.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR n.createdAt <= :endDate) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> searchByAdminList(
        @Param("query") String query,
        @Param("type") String type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    long countByUserIdAndIsReadFalse(Long userId);

    void deleteByUserId(Long userId);
}
