package com.example.timesheet.payload.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MonthWorkdaysResponse {
    private int year;
    private int month;
    private int totalDays;
    private int workdaysCount;
    private List<Map<String, Object>> days;
} 