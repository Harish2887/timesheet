package com.example.timesheet.repository;

import com.example.timesheet.model.RefreshToken;
import com.example.timesheet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUser(User user);
    
    @Transactional
    @Modifying
    int deleteByUser(User user);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate <= ?1")
    void deleteAllExpiredTokens(Date date);
} 