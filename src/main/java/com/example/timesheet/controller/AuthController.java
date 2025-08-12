package com.example.timesheet.controller;

import com.example.timesheet.model.Role;
import com.example.timesheet.model.Role.ERole;
import com.example.timesheet.model.User;
import com.example.timesheet.payload.request.ForgotPasswordRequest;
import com.example.timesheet.payload.request.LoginRequest;
import com.example.timesheet.payload.request.ResetPasswordRequest;
import com.example.timesheet.payload.request.SignupRequest;
import com.example.timesheet.payload.response.JwtResponse;
import com.example.timesheet.payload.response.MessageResponse;
import com.example.timesheet.repository.RoleRepository;
import com.example.timesheet.repository.UserRepository;
import com.example.timesheet.security.jwt.JwtUtils;
import com.example.timesheet.security.services.UserDetailsImpl;
import com.example.timesheet.service.PasswordResetService;
import com.example.timesheet.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;
    
    @Autowired
    PasswordResetService passwordResetService;
    
    @Autowired
    RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // Set the authentication in the context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        // Get user details from authentication
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Get user roles as strings
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Return the JWT in a response
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Clear the security context
        SecurityContextHolder.clearContext();
        
        // In a stateless JWT implementation, the server doesn't actually store tokens
        // The client is responsible for removing the token from storage
        // This endpoint is mainly for consistency and future expansion
        
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
    
    @GetMapping("/debug-auth")
    public ResponseEntity<?> debugAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (auth != null) {
            response.put("authenticated", true);
            response.put("principal", auth.getPrincipal().toString());
            response.put("name", auth.getName());
            
            List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            
            response.put("authorities", authorities);
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();
        String strRole = signUpRequest.getRole();

        // If no role is specified, assign ROLE_USER_EMP by default
        if (strRole == null || strRole.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER_EMP)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            // Assign role as specified
            switch (strRole) {
                case "admin":
                    Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(adminRole);
                    break;
                case "pay":
                    Role payRole = roleRepository.findByName(ERole.ROLE_USER_PAY)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(payRole);
                    break;
                case "sub":
                    Role subRole = roleRepository.findByName(ERole.ROLE_USER_SUB)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(subRole);
                    break;
                default:
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER_EMP)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole);
            }
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail();
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (!userOptional.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email not found!"));
        }
        
        User user = userOptional.get();
        passwordResetService.createPasswordResetTokenForUser(user);
        System.out.println();
        
        return ResponseEntity.ok(new MessageResponse("Password reset link sent to your email"));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        String token = resetPasswordRequest.getToken();
        String password = resetPasswordRequest.getPassword();
        
        String result = passwordResetService.validatePasswordResetToken(token);
        
        if (!"valid".equals(result)) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Invalid or expired token"));
        }
        
        Optional<User> userOptional = passwordResetService.getUserByPasswordResetToken(token);
        
        if (!userOptional.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: User not found"));
        }
        
        User user = userOptional.get();
        passwordResetService.changeUserPassword(user, password);
        passwordResetService.deletePasswordResetToken(token);
        
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
} 