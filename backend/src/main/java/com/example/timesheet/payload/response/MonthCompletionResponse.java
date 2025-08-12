package com.example.timesheet.payload.response;

import com.example.timesheet.model.TimesheetEntry;
import lombok.Data;

import java.util.List;

@Data
public class MonthCompletionResponse {
    private int year;
    private int month;
    private boolean isComplete;
    private int totalWorkdays;
    private int filledWorkdays;
    private double completionPercentage;
    private List<TimesheetEntry> entries;
} 