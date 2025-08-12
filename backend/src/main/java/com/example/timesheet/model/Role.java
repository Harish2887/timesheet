package com.example.timesheet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;
    
    public enum ERole {
        ROLE_ADMIN,      // Administrator role
        ROLE_USER_EMP,   // Regular employee user
        ROLE_USER_PAY,   // Payment-handling employee
        ROLE_USER_SUB    // Subcontractor
    }
} 