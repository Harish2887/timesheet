package com.example.timesheet.service;

import com.example.timesheet.model.User;
import java.util.Optional;

/**
 * Service interface for managing password reset operations
 */
public interface PasswordResetService {
    
    /**
     * Create a password reset token for a user
     * @param user the user who requested a password reset
     * @return the generated token string
     */
    String createPasswordResetTokenForUser(User user);
    
    /**
     * Validate a password reset token
     * @param token the token to validate
     * @return a string indicating the validity status ("valid", "invalidToken", or "expired")
     */
    String validatePasswordResetToken(String token);
    
    /**
     * Find a user by their password reset token
     * @param token the password reset token
     * @return the user associated with the token, if found
     */
    Optional<User> getUserByPasswordResetToken(String token);
    
    /**
     * Change a user's password
     * @param user the user whose password is being changed
     * @param newPassword the new password (unencoded)
     */
    void changePassword(User user, String newPassword);
    
    /**
     * Generate a reset password URL
     * @param token the reset token
     * @return the complete URL for password reset
     */
    String generatePasswordResetUrl(String token);
    
    /**
     * Clean up expired tokens
     */
    void cleanExpiredTokens();
    
    /**
     * Alias for changePassword for backward compatibility
     */
    default void changeUserPassword(User user, String newPassword) {
        changePassword(user, newPassword);
    }
    
    /**
     * Delete a specific password reset token
     * @param token the token to delete
     */
    void deletePasswordResetToken(String token);
} 