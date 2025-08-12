package com.example.timesheet.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monthly_timesheets")
public class MonthlyTimesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(nullable = false)
    private Integer month;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimesheetStatus status = TimesheetStatus.DRAFT;
    
    @Column
    private Double totalHoursWorked;
    
    @Column
    private Double regularHours;
    
    @Column
    private Double holidayHours;
    
    @Column // For PAY/SUB users uploading a PDF
    private Double totalHoursReportedByUploader;
    
    @Column
    private LocalDate submissionDate;
    
    @Column
    private LocalDate approvalDate;
    
    @Column
    private LocalDate paymentDate;
    
    @Column
    private String comments;
    
    @Column
    private String uploadedFilePath;
    
    @OneToMany(mappedBy = "monthlyTimesheet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @ToString.Exclude
    private List<TimesheetEntry> entries = new ArrayList<>();
    
    public enum TimesheetStatus {
        DRAFT,      // Initial state when timesheet is being created
        SUBMITTED,  // When user has submitted the timesheet for approval
        APPROVED,   // When admin has approved the timesheet
        REJECTED,   // When admin has rejected the timesheet
        PAID        // When payment has been processed
    }
} 