package com.example.timesheet.service.impl;

import com.example.timesheet.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Timesheet Application}")
    private String appName;
    
    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetUrl) {
        try {
            String subject = appName + " - Password Reset Request";
            
            String content = String.format(
                "Hello %s,\n\n" +
                "We received a request to reset your password for your %s account.\n\n" +
                "Click the link below to reset your password:\n%s\n\n" +
                "This link will expire in 24 hours for security reasons.\n\n" +
                "If you didn't request a password reset, please ignore this email. Your password will remain unchanged.\n\n" +
                "Best regards,\n%s Team",
                username, appName, resetUrl, appName
            );
            
            sendSimpleEmail(toEmail, subject, content);
            
            logger.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    @Override
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            
            logger.info("Simple email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send simple email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        // For now, just send as simple text email
        sendSimpleEmail(toEmail, subject, htmlContent);
    }
} 