package com.example.timesheet.controller;

import com.example.timesheet.model.HolidayType;
import com.example.timesheet.model.TimesheetEntry;
import com.example.timesheet.model.User;
import com.example.timesheet.payload.request.TimesheetRequest;
import com.example.timesheet.payload.response.MonthCompletionResponse;
import com.example.timesheet.payload.response.MonthWorkdaysResponse;
import com.example.timesheet.repository.HolidayTypeRepository;
import com.example.timesheet.repository.TimesheetEntryRepository;
import com.example.timesheet.repository.UserRepository;
import com.example.timesheet.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/timesheet")
public class TimesheetController {
    @Autowired
    private TimesheetEntryRepository timesheetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HolidayTypeRepository holidayTypeRepository;

    @GetMapping("/")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<List<TimesheetEntry>> getMyTimesheet() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TimesheetEntry> entries = timesheetRepository.findByUser(user);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<List<TimesheetEntry>> getTimesheetsByDateRange(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        
        List<TimesheetEntry> entries = timesheetRepository.findByUserAndDateBetween(user, start, end);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/month/workdays")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<MonthWorkdaysResponse> getMonthWorkdays(
            @RequestParam int year,
            @RequestParam int month) {
        
        // Get the first and last day of the month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        // Collect all days in the month
        List<Map<String, Object>> allDays = new ArrayList<>();
        LocalDate date = firstDay;
        
        // Get Swedish holidays
        Set<LocalDate> publicHolidays = getSwedishPublicHolidays(year, month);
        
        // Process each day in the month
        while (!date.isAfter(lastDay)) {
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", date.toString());
            dayInfo.put("dayOfWeek", date.getDayOfWeek().toString());
            
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || 
                               date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = publicHolidays.contains(date);
            
            dayInfo.put("isWorkday", !isWeekend && !isHoliday);
            dayInfo.put("isWeekend", isWeekend);
            dayInfo.put("isHoliday", isHoliday);
            
            if (isHoliday) {
                dayInfo.put("holidayName", getHolidayName(date));
            }
            
            allDays.add(dayInfo);
            date = date.plusDays(1);
        }
        
        // Calculate workdays count
        long workdaysCount = allDays.stream()
                .filter(day -> (boolean) day.get("isWorkday"))
                .count();
        
        // Create response
        MonthWorkdaysResponse response = new MonthWorkdaysResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTotalDays(allDays.size());
        response.setWorkdaysCount((int) workdaysCount);
        response.setDays(allDays);
        
        return ResponseEntity.ok(response);
    }
    
    private String getHolidayName(LocalDate date) {
        // Simplified implementation - in a real app, you'd look this up from a database
        Map<String, String> knownHolidays = new HashMap<>();
        knownHolidays.put(date.getYear() + "-01-01", "New Year's Day");
        knownHolidays.put(date.getYear() + "-01-06", "Epiphany");
        knownHolidays.put(date.getYear() + "-05-01", "Labor Day");
        knownHolidays.put(date.getYear() + "-06-06", "National Day of Sweden");
        knownHolidays.put(date.getYear() + "-12-24", "Christmas Eve");
        knownHolidays.put(date.getYear() + "-12-25", "Christmas Day");
        knownHolidays.put(date.getYear() + "-12-26", "Boxing Day");
        knownHolidays.put(date.getYear() + "-12-31", "New Year's Eve");
        
        return knownHolidays.getOrDefault(date.toString(), "Public Holiday");
    }
    
    private Set<LocalDate> getSwedishPublicHolidays(int year, int month) {
        // Simplified implementation - just the major fixed date holidays
        Set<LocalDate> holidays = new HashSet<>();
        
        // Add the fixed holidays for the current month
        switch (month) {
            case 1: // January
                holidays.add(LocalDate.of(year, 1, 1)); // New Year's Day
                holidays.add(LocalDate.of(year, 1, 6)); // Epiphany
                break;
            case 5: // May
                holidays.add(LocalDate.of(year, 5, 1)); // Labor Day
                break;
            case 6: // June
                holidays.add(LocalDate.of(year, 6, 6)); // National Day of Sweden
                break;
            case 12: // December
                holidays.add(LocalDate.of(year, 12, 24)); // Christmas Eve
                holidays.add(LocalDate.of(year, 12, 25)); // Christmas Day
                holidays.add(LocalDate.of(year, 12, 26)); // Boxing Day
                holidays.add(LocalDate.of(year, 12, 31)); // New Year's Eve
                break;
        }
        
        // Note: Movable holidays like Easter, Midsummer, etc., would need more complex calculation
        
        return holidays;
    }
    
    @GetMapping("/month/completion")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<MonthCompletionResponse> getMonthCompletion(
            @RequestParam int year,
            @RequestParam int month) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get the first and last day of the month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        // Get all timesheet entries for this month
        List<TimesheetEntry> entries = timesheetRepository.findByUserAndDateBetween(user, firstDay, lastDay);
        
        // Create a set of all dates that have entries
        Set<LocalDate> datesWithEntries = entries.stream()
                .map(TimesheetEntry::getDate)
                .collect(Collectors.toSet());
        
        // Find all weekdays in this month (excluding weekends and holidays)
        Set<LocalDate> allWorkdays = new HashSet<>();
        Set<LocalDate> publicHolidays = getSwedishPublicHolidays(year, month);
        
        LocalDate date = firstDay;
        while (!date.isAfter(lastDay)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && 
                date.getDayOfWeek() != DayOfWeek.SUNDAY && 
                !publicHolidays.contains(date)) {
                allWorkdays.add(date);
            }
            date = date.plusDays(1);
        }
        
        // Determine if all workdays have entries
        boolean isComplete = allWorkdays.stream().allMatch(datesWithEntries::contains);
        
        // Calculate completion percentage
        int totalDays = allWorkdays.size();
        int filledDays = (int) allWorkdays.stream().filter(datesWithEntries::contains).count();
        double completionPercentage = totalDays > 0 ? ((double) filledDays / totalDays) * 100 : 0;
        
        // Create response
        MonthCompletionResponse response = new MonthCompletionResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setComplete(isComplete);
        response.setTotalWorkdays(totalDays);
        response.setFilledWorkdays(filledDays);
        response.setCompletionPercentage(completionPercentage);
        response.setEntries(entries);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<?> createTimesheet(@RequestBody TimesheetRequest timesheetRequest) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        TimesheetEntry entry = new TimesheetEntry();
        entry.setUser(user);
        entry.setDate(LocalDate.parse(timesheetRequest.getDate(), DateTimeFormatter.ISO_DATE));
        entry.setHoursWorked(timesheetRequest.getHoursWorked());
        
        if (timesheetRequest.getHolidayTypeId() != null) {
            entry.setHolidayType(holidayTypeRepository.findById(timesheetRequest.getHolidayTypeId())
                    .orElseThrow(() -> new RuntimeException("Holiday type not found")));
        }
        
        entry.setDescription(timesheetRequest.getDescription());
        entry.setStatus("PENDING");
        
        timesheetRepository.save(entry);
        
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<?> getTimesheetById(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        TimesheetEntry entry = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet entry not found"));
        
        // Check if user owns this entry or is admin
        if (!entry.getUser().getId().equals(user.getId()) && 
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.badRequest().body("You don't have permission to view this entry");
        }
        
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER_EMP') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteTimesheet(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        TimesheetEntry entry = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet entry not found"));
        
        // Check if user owns this entry or is admin
        if (!entry.getUser().getId().equals(user.getId()) && 
            !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.badRequest().body("You don't have permission to delete this entry");
        }
        
        timesheetRepository.delete(entry);
        
        return ResponseEntity.ok("Timesheet entry deleted successfully");
    }
} 