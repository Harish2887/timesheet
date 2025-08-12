package com.example.timesheet.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "timesheet_entries")
public class TimesheetEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "monthly_timesheet_id")
    @JsonBackReference
    private MonthlyTimesheet monthlyTimesheet;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private Double hoursWorked;
    
    // Extra support time worked after normal hours (overtime/support)
    @Column
    private Double supportHours;
    
    @ManyToOne
    @JoinColumn(name = "holiday_type_id")
    private HolidayType holidayType;
    
    private String description;
    
    private String status; // "PENDING", "APPROVED", "REJECTED"
} 