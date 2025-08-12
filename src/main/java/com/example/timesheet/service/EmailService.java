package com.example.timesheet.service;

/**
 * Service interface for sending emails
 */
public interface EmailService {
    
    /**
     * Send a password reset email to the user
     * @param toEmail the recipient's email address
     * @param username the username
     * @param resetUrl the password reset URL
     */
    void sendPasswordResetEmail(String toEmail, String username, String resetUrl);
    
    /**
     * Send a simple text email
     * @param toEmail the recipient's email address
     * @param subject the email subject
     * @param content the email content
     */
    void sendSimpleEmail(String toEmail, String subject, String content);
    
    /**
     * Send an HTML email
     * @param toEmail the recipient's email address
     * @param subject the email subject
     * @param htmlContent the HTML email content
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);
} 