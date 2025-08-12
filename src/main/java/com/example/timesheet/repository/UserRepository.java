package com.example.timesheet.repository;

import com.example.timesheet.model.Role;
import com.example.timesheet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(Role.ERole roleName);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName OR r.name = :alternativeRoleName")
    List<User> findByRoleNameOrAlternativeRoleName(Role.ERole roleName, Role.ERole alternativeRoleName);
} 