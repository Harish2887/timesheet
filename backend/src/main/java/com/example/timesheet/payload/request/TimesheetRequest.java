package com.example.timesheet.payload.request;

import lombok.Data;

@Data
public class TimesheetRequest {
    private String date;
    private Double hoursWorked;
    private Long holidayTypeId;
    private String description;
} 