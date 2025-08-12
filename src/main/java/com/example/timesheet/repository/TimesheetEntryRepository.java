package com.example.timesheet.repository;

import com.example.timesheet.model.TimesheetEntry;
import com.example.timesheet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {
    List<TimesheetEntry> findByUser(User user);
    List<TimesheetEntry> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<TimesheetEntry> findByUserAndDateBetweenOrderByDateAsc(User user, LocalDate startDate, LocalDate endDate);
    List<TimesheetEntry> findByDateBetween(LocalDate startDate, LocalDate endDate);
    long countByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    Optional<TimesheetEntry> findByUserAndDate(User user, LocalDate date);
    
    @Query("SELECT e FROM TimesheetEntry e WHERE e.user = :user AND YEAR(e.date) = :year AND MONTH(e.date) = :month")
    List<TimesheetEntry> findByUserAndYearAndMonth(User user, Integer year, Integer month);
    
    @Query("SELECT e FROM TimesheetEntry e WHERE YEAR(e.date) = :year AND MONTH(e.date) = :month")
    List<TimesheetEntry> findByYearAndMonth(Integer year, Integer month);
} 