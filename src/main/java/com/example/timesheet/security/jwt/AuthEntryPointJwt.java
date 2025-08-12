package com.example.timesheet.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        logger.error("Unauthorized error: {}", authException.getMessage());
        logger.error("Request path: {}", request.getRequestURI());
        
        // Check if authentication is in the context
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            logger.error("CRITICAL ERROR: SecurityContext has authentication but access was denied: {}", 
                SecurityContextHolder.getContext().getAuthentication().getName());
            logger.error("Authentication authorities: {}", 
                SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        } else {
            logger.error("No authentication in security context when unauthorized error occurred");
        }
        
        // Check request headers
        logger.error("Authorization header: {}", 
            request.getHeader("Authorization") != null ? 
            request.getHeader("Authorization").substring(0, Math.min(20, request.getHeader("Authorization").length())) + "..." : "null");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
} 