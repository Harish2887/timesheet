package com.example.timesheet.service.impl;

import com.example.timesheet.model.PasswordResetToken;
import com.example.timesheet.model.User;
import com.example.timesheet.repository.PasswordResetTokenRepository;
import com.example.timesheet.repository.UserRepository;
import com.example.timesheet.service.EmailService;
import com.example.timesheet.service.PasswordResetService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Create a password reset token for the given user and send email
     */
    @Override
    public String createPasswordResetTokenForUser(User user) {
        // First, check if user already has a token
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
        
        if (existingToken.isPresent()) {
            // Delete existing token
            tokenRepository.delete(existingToken.get());
        }
        
        // Create new token
        PasswordResetToken token = new PasswordResetToken(user);
        tokenRepository.save(token);
        
        // Generate reset URL
        String resetUrl = generatePasswordResetUrl(token.getToken());
        
        // Send password reset email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetUrl);
        
        return token.getToken();
    }
    
    /**
     * Verify the validity of the token
     */
    @Override
    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passwordResetToken = tokenRepository.findByToken(token);
        
        if (passwordResetToken.isEmpty()) {
            return "invalidToken";
        }
        
        if (passwordResetToken.get().isExpired()) {
            tokenRepository.delete(passwordResetToken.get());
            return "expired";
        }
        
        return "valid";
    }
    
    /**
     * Get user by password reset token
     */
    @Override
    public Optional<User> getUserByPasswordResetToken(String token) {
        Optional<PasswordResetToken> passwordResetToken = tokenRepository.findByToken(token);
        return passwordResetToken.map(PasswordResetToken::getUser);
    }
    
    /**
     * Reset password
     */
    @Override
    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Delete used token
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);
    }
    
    /**
     * Generate a reset password URL that can be sent to users
     */
    @Override
    public String generatePasswordResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }
    
    /**
     * Clean up expired tokens
     */
    @Override
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(new Date());
    }
    
    /**
     * Delete a specific token
     */
    @Override
    public void deletePasswordResetToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }
} 