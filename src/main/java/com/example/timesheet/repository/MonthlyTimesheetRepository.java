package com.example.timesheet.repository;

import com.example.timesheet.model.MonthlyTimesheet;
import com.example.timesheet.model.MonthlyTimesheet.TimesheetStatus;
import com.example.timesheet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyTimesheetRepository extends JpaRepository<MonthlyTimesheet, Long> {
    List<MonthlyTimesheet> findByUser(User user);
    List<MonthlyTimesheet> findByUserOrderByYearDescMonthDesc(User user);
    Optional<MonthlyTimesheet> findByUserAndYearAndMonth(User user, Integer year, Integer month);
    List<MonthlyTimesheet> findByStatus(TimesheetStatus status);
    List<MonthlyTimesheet> findByYearAndMonth(Integer year, Integer month);
    
    @Query("SELECT mt FROM MonthlyTimesheet mt WHERE mt.year = :year AND mt.month = :month AND mt.status = :status")
    List<MonthlyTimesheet> findByYearAndMonthAndStatus(Integer year, Integer month, TimesheetStatus status);
    
    @Query("SELECT COUNT(mt) FROM MonthlyTimesheet mt WHERE mt.user = :user AND mt.year = :year AND mt.month = :month")
    Long countByUserAndYearAndMonth(User user, Integer year, Integer month);
} 