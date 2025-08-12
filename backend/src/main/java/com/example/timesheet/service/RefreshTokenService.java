package com.example.timesheet.service;

import com.example.timesheet.model.RefreshToken;
import java.util.Optional;

/**
 * Service interface for managing refresh tokens
 */
public interface RefreshTokenService {
    
    /**
     * Create a new refresh token for a user
     * @param userId the user ID
     * @return the created refresh token
     */
    RefreshToken createRefreshToken(Long userId);
    
    /**
     * Find a refresh token by token string
     * @param token the refresh token string
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Verify that a refresh token is valid and not expired
     * @param token the refresh token
     * @return the verified refresh token
     */
    RefreshToken verifyExpiration(RefreshToken token);
    
    /**
     * Delete a refresh token by user ID
     * @param userId the user ID
     * @return the number of tokens deleted
     */
    int deleteByUserId(Long userId);
} 