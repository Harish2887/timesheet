package com.example.timesheet.security.jwt;

import com.example.timesheet.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private SecurityContextRepository securityContextRepository;
    
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Print the requested URI for debugging
            logger.info("Processing request to: {}", request.getRequestURI());
            
            // Parse JWT token from the request
            String jwt = parseJwt(request);
            
            // If token exists and is valid, set the authentication
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                logger.info("JWT token is valid for user: {}", username);
                
                // Create UserDetails from the username in the token
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Create authentication token with user details
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                // Set details from the request
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Create and populate a Security Context
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                context.setAuthentication(authentication);
                
                // Set the context in the holder
                SecurityContextHolder.setContext(context);
                
                // Store the context in the request for this thread
                securityContextRepository.saveContext(context, request, response);
                
                logger.info("Authentication set in SecurityContext for user: {}, with authorities: {}", 
                    userDetails.getUsername(), userDetails.getAuthorities());
                
                // Debug the authentication state after setting it
                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuth != null) {
                    logger.info("Pre-filter chain auth check - User: {}, Authorities: {}", 
                            currentAuth.getName(), currentAuth.getAuthorities());
                } else {
                    logger.warn("Pre-filter chain auth check - No authentication in context after setting it!");
                }
            } else if (jwt != null) {
                logger.warn("JWT token validation failed");
            } else {
                logger.info("No JWT token found in request");
            }
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
            // Log authentication state after filter chain execution
            Authentication postAuth = SecurityContextHolder.getContext().getAuthentication();
            if (postAuth != null) {
                logger.info("Post-filter authentication state - user: {}, authorities: {}", 
                    postAuth.getName(), postAuth.getAuthorities());
            } else {
                logger.warn("Post-filter authentication state - Authentication is null!");
            }
            
        } catch (Exception e) {
            logger.error("Error processing authentication: {}", e.getMessage(), e);
            
            // Continue with filter chain even after error
            filterChain.doFilter(request, response);
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            logger.info("Found JWT token in Authorization header (length: {})", token.length());
            return token;
        }
        
        return null;
    }
} 