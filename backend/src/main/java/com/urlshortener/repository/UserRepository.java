package com.urlshortener.repository;

import com.urlshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<String> findEmailById(Long id); // Though usually we just findById
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByAutoLoginToken(String token);
    
    java.util.List<User> findByRole(com.urlshortener.entity.Role role);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.active = :active")
    void updateAllUsersStatus(@org.springframework.data.repository.query.Param("active") int active);
}
