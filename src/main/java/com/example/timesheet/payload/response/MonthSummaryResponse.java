package com.example.timesheet.payload.response;

import lombok.Data;

@Data
public class MonthSummaryResponse {
    private Long userId;
    private String username;
    private int year;
    private int month;
    private String monthName;
    private boolean isComplete;
    private int totalWorkdays;
    private int filledWorkdays;
    private double completionPercentage;
    private String status; // PENDING, APPROVED, REJECTED
    private int entriesCount;
} 