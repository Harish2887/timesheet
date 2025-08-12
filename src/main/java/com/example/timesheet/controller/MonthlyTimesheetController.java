package com.example.timesheet.controller;

import com.example.timesheet.model.HolidayType;
import com.example.timesheet.model.MonthlyTimesheet;
import com.example.timesheet.model.MonthlyTimesheet.TimesheetStatus;
import com.example.timesheet.model.TimesheetEntry;
import com.example.timesheet.model.User;
import com.example.timesheet.repository.HolidayTypeRepository;
import com.example.timesheet.repository.MonthlyTimesheetRepository;
import com.example.timesheet.repository.TimesheetEntryRepository;
import com.example.timesheet.repository.UserRepository;
import com.example.timesheet.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.time.DayOfWeek;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/monthly-timesheet")
public class MonthlyTimesheetController {
    
    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;
    
    @Autowired
    private TimesheetEntryRepository timesheetEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private HolidayTypeRepository holidayTypeRepository;
    
    @GetMapping("/")
    @PreAuthorize("hasAuthority('ROLE_USER_EMP') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<MonthlyTimesheet>> getMyMonthlyTimesheets() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        List<MonthlyTimesheet> timesheets = monthlyTimesheetRepository.findByUser(user);
        return ResponseEntity.ok(timesheets);
    }
    
    @GetMapping("/{year}/{month}")
    @PreAuthorize("hasAuthority('ROLE_USER_EMP') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getMonthlyTimesheet(@PathVariable int year, @PathVariable int month) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<MonthlyTimesheet> timesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        
        if (timesheet.isPresent()) {
            return ResponseEntity.ok(timesheet.get());
        } else {
            // Check if there are any timesheet entries for this month
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            
            List<TimesheetEntry> entries = timesheetEntryRepository.findByUserAndDateBetween(user, startDate, endDate);
            
            // Return an empty response with the month's details
            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("exists", false);
            response.put("entriesCount", entries.size());
            
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/{year}/{month}/entries")
    @PreAuthorize("hasAuthority('ROLE_USER_EMP') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_USER_PAY') or hasAuthority('ROLE_USER_SUB')")
    public ResponseEntity<?> createOrUpdateMonthlyTimesheetWithEntries(
            @PathVariable int year, 
            @PathVariable int month,
            @RequestBody Map<String, Object> requestPayload) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get entries from request
        List<Map<String, Object>> entriesData = new ArrayList<>();
        boolean isSubmit = false;
        
        if (requestPayload.containsKey("entries")) {
            entriesData = (List<Map<String, Object>>) requestPayload.get("entries");
        }
        
        if (requestPayload.containsKey("submit")) {
            isSubmit = (boolean) requestPayload.get("submit");
        }
        
        // Check if a monthly timesheet already exists
        Optional<MonthlyTimesheet> existingTimesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        
        MonthlyTimesheet timesheet;
        if (existingTimesheet.isPresent()) {
            timesheet = existingTimesheet.get();
            // Don't clear the existing entries - fetch all existing entries first 
            // Get existing entries for this month
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            List<TimesheetEntry> existingEntries = timesheetEntryRepository.findByUserAndDateBetween(user, startDate, endDate);
            
            // Store existing entry ids for later comparison
            Map<LocalDate, TimesheetEntry> existingEntriesMap = new HashMap<>();
            for (TimesheetEntry entry : existingEntries) {
                existingEntriesMap.put(entry.getDate(), entry);
            }
            
            // Process new entries
            List<TimesheetEntry> updatedEntries = new ArrayList<>();
            for (Map<String, Object> entryData : entriesData) {
                String dateStr = (String) entryData.get("date");
                LocalDate date = LocalDate.parse(dateStr);
                
                // Look for an existing entry for this date
                TimesheetEntry entry = existingEntriesMap.get(date);
                if (entry == null) {
                    // Create new entry if none exists
                    entry = new TimesheetEntry();
                    entry.setUser(user);
                    entry.setDate(date);
                }
                
                // Update entry fields
                Double hoursWorked = entryData.get("hoursWorked") instanceof Integer ? 
                      ((Integer)entryData.get("hoursWorked")).doubleValue() : 
                      (Double) entryData.get("hoursWorked");
                Double supportHours = 0.0;
                if (entryData.containsKey("supportHours") && entryData.get("supportHours") != null) {
                    Object sh = entryData.get("supportHours");
                    if (sh instanceof Integer) supportHours = ((Integer) sh).doubleValue();
                    else if (sh instanceof Double) supportHours = (Double) sh;
                }
                Long holidayTypeId = entryData.get("holidayTypeId") != null ? 
                      Long.valueOf(entryData.get("holidayTypeId").toString()) : null;
                String notes = (String) entryData.get("notes");
                
                entry.setHoursWorked(hoursWorked);
                entry.setSupportHours(supportHours);
                entry.setDescription(notes);
                entry.setMonthlyTimesheet(timesheet);
                
                if (holidayTypeId != null) {
                    HolidayType holidayType = holidayTypeRepository.findById(holidayTypeId)
                            .orElseThrow(() -> new RuntimeException("Holiday type not found"));
                    entry.setHolidayType(holidayType);
                } else {
                    entry.setHolidayType(null);
                }
                
                updatedEntries.add(entry);
                existingEntriesMap.remove(date); // Remove from map to track what's been updated
            }
            
            // For entries in the existing map that weren't in the update request,
            // detach them from this timesheet
            for (TimesheetEntry orphanedEntry : existingEntriesMap.values()) {
                orphanedEntry.setMonthlyTimesheet(null);
                timesheetEntryRepository.save(orphanedEntry);
            }
            
            // Save all the updated entries
            List<TimesheetEntry> savedEntries = timesheetEntryRepository.saveAll(updatedEntries);
            
            // Calculate totals
            double totalHours = 0;
            double regularHours = 0;
            double holidayHours = 0;
            double totalSupportHours = 0;
            
            for (TimesheetEntry entry : savedEntries) {
                if (entry.getHoursWorked() != null) {
                    totalHours += entry.getHoursWorked();
                    if (entry.getSupportHours() != null) {
                        totalSupportHours += entry.getSupportHours();
                    }
                    
                    if (entry.getHolidayType() != null) {
                        holidayHours += entry.getHoursWorked();
                    } else {
                        regularHours += entry.getHoursWorked();
                    }
                }
            }
            
            // Update timesheet totals
            timesheet.setTotalHoursWorked(totalHours);
            timesheet.setRegularHours(regularHours);
            timesheet.setHolidayHours(holidayHours);
            // totalSupportHours can be reported separately if needed in future
            timesheet.setTotalHoursReportedByUploader(null);
            
            // If submit flag is true, submit the timesheet
            if (isSubmit && timesheet.getStatus() == TimesheetStatus.DRAFT) {
                timesheet.setStatus(TimesheetStatus.SUBMITTED);
                timesheet.setSubmissionDate(LocalDate.now());
            }
            
            // Save the timesheet
            timesheet = monthlyTimesheetRepository.save(timesheet);
            
            // Fetch the saved timesheet with all its entries
            timesheet = monthlyTimesheetRepository.findById(timesheet.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve saved timesheet"));
            
            return ResponseEntity.ok(timesheet);
        } else {
            // Creating a new timesheet
            timesheet = new MonthlyTimesheet();
            timesheet.setUser(user);
            timesheet.setYear(year);
            timesheet.setMonth(month);
            timesheet.setStatus(TimesheetStatus.DRAFT);
            
            // Save the timesheet first to get an ID
            timesheet = monthlyTimesheetRepository.save(timesheet);
            
            // Create and save entries
            List<TimesheetEntry> newEntries = new ArrayList<>();
            for (Map<String, Object> entryData : entriesData) {
                String dateStr = (String) entryData.get("date");
                Double hoursWorked = entryData.get("hoursWorked") instanceof Integer ? 
                      ((Integer)entryData.get("hoursWorked")).doubleValue() : 
                      (Double) entryData.get("hoursWorked");
                Double supportHours = 0.0;
                if (entryData.containsKey("supportHours") && entryData.get("supportHours") != null) {
                    Object sh = entryData.get("supportHours");
                    if (sh instanceof Integer) supportHours = ((Integer) sh).doubleValue();
                    else if (sh instanceof Double) supportHours = (Double) sh;
                }
                Long holidayTypeId = entryData.get("holidayTypeId") != null ? 
                      Long.valueOf(entryData.get("holidayTypeId").toString()) : null;
                String notes = (String) entryData.get("notes");
                
                // Parse date
                LocalDate date = LocalDate.parse(dateStr);
                
                // Create new entry
                TimesheetEntry entry = new TimesheetEntry();
                entry.setUser(user);
                entry.setDate(date);
                entry.setHoursWorked(hoursWorked);
                entry.setSupportHours(supportHours);
                entry.setDescription(notes);
                entry.setMonthlyTimesheet(timesheet);
                
                if (holidayTypeId != null) {
                    HolidayType holidayType = holidayTypeRepository.findById(holidayTypeId)
                            .orElseThrow(() -> new RuntimeException("Holiday type not found"));
                    entry.setHolidayType(holidayType);
                }
                
                newEntries.add(entry);
            }
            
            // Save all entries at once
            List<TimesheetEntry> savedEntries = timesheetEntryRepository.saveAll(newEntries);
            
            // Calculate totals
            double totalHours = 0;
            double regularHours = 0;
            double holidayHours = 0;
            double totalSupportHours = 0;
            
            for (TimesheetEntry entry : savedEntries) {
                if (entry.getHoursWorked() != null) {
                    totalHours += entry.getHoursWorked();
                    if (entry.getSupportHours() != null) {
                        totalSupportHours += entry.getSupportHours();
                    }
                    
                    if (entry.getHolidayType() != null) {
                        holidayHours += entry.getHoursWorked();
                    } else {
                        regularHours += entry.getHoursWorked();
                    }
                }
            }
            
            // Update timesheet totals
            timesheet.setTotalHoursWorked(totalHours);
            timesheet.setRegularHours(regularHours);
            timesheet.setHolidayHours(holidayHours);
            timesheet.setTotalHoursReportedByUploader(null);
            
            // If submit flag is true, submit the timesheet
            if (isSubmit) {
                timesheet.setStatus(TimesheetStatus.SUBMITTED);
                timesheet.setSubmissionDate(LocalDate.now());
            }
            
            // Save the timesheet
            timesheet = monthlyTimesheetRepository.save(timesheet);
            
            // Fetch the saved timesheet with all its entries
            timesheet = monthlyTimesheetRepository.findById(timesheet.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve saved timesheet"));
            
            return ResponseEntity.ok(timesheet);
        }
    }
    
    @PostMapping("/{year}/{month}/upload")
    @PreAuthorize("hasAuthority('ROLE_USER_PAY') or hasAuthority('ROLE_USER_SUB') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> uploadTimesheetPdf(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam("totalHoursReported") double totalHoursReported,
            @RequestParam("file") MultipartFile file) {

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

        // 1. File Validation: Type and Name
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty."));
        }
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid file type. Only PDF is allowed."));
        }

        String originalFilename = file.getOriginalFilename();
        String username = user.getUsername().replaceAll("[^a-zA-Z0-9_.-]", "_"); // Sanitize username
        String filename = username + "_" + year + "-" + String.format("%02d", month) + ".pdf";
        Path uploadDir = Paths.get("uploads");

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 2. Hours Validation
            YearMonth yearMonthObject = YearMonth.of(year, month);
            long workDaysInMonth = countWorkdays(yearMonthObject);
            double maxHoursForMonth = workDaysInMonth * 8; // Assuming 8 hours/day

            if (Math.abs(totalHoursReported - maxHoursForMonth) > 0.01) { // Using a tolerance for double comparison
                 // Delete the uploaded file if validation fails
                Files.deleteIfExists(filePath);
                return ResponseEntity.badRequest().body(Map.of("message", 
                    "Reported hours (" + totalHoursReported + ") do not match the expected work hours for the month (" + maxHoursForMonth + "). Expected workdays: " + workDaysInMonth));
            }

            // 3. Database Interaction
            MonthlyTimesheet timesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month)
                .orElseGet(() -> {
                    MonthlyTimesheet newMs = new MonthlyTimesheet();
                    newMs.setUser(user);
                    newMs.setYear(year);
                    newMs.setMonth(month);
                    return newMs;
                });

            timesheet.setUploadedFilePath(filePath.toString());
            
            // New logic for PAY/SUB PDF uploads
            timesheet.setTotalHoursReportedByUploader(totalHoursReported);
            timesheet.setTotalHoursWorked(totalHoursReported); // For PAY/SUB, this is the same as reported
            timesheet.setRegularHours(totalHoursReported);   // Assume all reported hours are regular
            timesheet.setHolidayHours(0.0);                   // No breakdown from PDF

            MonthlyTimesheet savedTimesheet = monthlyTimesheetRepository.save(timesheet);

            // Create or update a single summary TimesheetEntry
            // First, remove any existing entries associated with this monthly timesheet
            List<TimesheetEntry> entriesToDelete = new ArrayList<>(savedTimesheet.getEntries());
            if (!entriesToDelete.isEmpty()) {
                 // Detach from the monthly timesheet first
                 for (TimesheetEntry entry : entriesToDelete) {
                     entry.setMonthlyTimesheet(null);
                 }
                 // It's generally safer to save the detached entries if your cascade settings aren't ALL + orphanRemoval, 
                 // but with those, clearing the collection on the parent and saving the parent should suffice.
                 // However, to be explicit and handle various cascade types:
                 timesheetEntryRepository.saveAll(entriesToDelete); // Persist detachment if necessary
                 savedTimesheet.getEntries().clear(); // Clear the collection on the parent
                 timesheetEntryRepository.deleteAllInBatch(entriesToDelete); // Delete them
            }
            // Ensure the timesheet is saved after clearing entries, before adding the new one.
            // This is important if deleteAllInBatch doesn't flush immediately or if there are triggers.
            monthlyTimesheetRepository.save(savedTimesheet); 

            TimesheetEntry summaryEntry = new TimesheetEntry();
            summaryEntry.setUser(user);
            summaryEntry.setMonthlyTimesheet(savedTimesheet);
            summaryEntry.setDate(yearMonthObject.atDay(1)); // First day of the month
            summaryEntry.setHoursWorked(totalHoursReported);
            summaryEntry.setDescription("Uploaded PDF timesheet: " + filename);
            summaryEntry.setStatus(savedTimesheet.getStatus().name()); // Match status from MonthlyTimesheet
            
            timesheetEntryRepository.save(summaryEntry);
            
            return ResponseEntity.ok(Map.of("message", "Timesheet PDF uploaded and submitted successfully.", "filePath", filePath.toString()));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to upload file: " + e.getMessage()));
        }
    }

    private long countWorkdays(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        long workDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                workDays++;
            }
        }
        return workDays;
    }
    
    @PostMapping("/{year}/{month}")
    @PreAuthorize("hasAuthority('ROLE_USER_EMP') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createOrUpdateMonthlyTimesheet(@PathVariable int year, @PathVariable int month) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if a monthly timesheet already exists
        Optional<MonthlyTimesheet> existingTimesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        
        MonthlyTimesheet timesheet;
        if (existingTimesheet.isPresent()) {
            timesheet = existingTimesheet.get();
            // Clear the existing entries to avoid orphaned entities
            timesheet.getEntries().clear();
        } else {
            timesheet = new MonthlyTimesheet();
            timesheet.setUser(user);
            timesheet.setYear(year);
            timesheet.setMonth(month);
            timesheet.setStatus(TimesheetStatus.DRAFT);
        }
        
        // First save the timesheet to ensure it has an ID
        timesheet = monthlyTimesheetRepository.save(timesheet);
        
        // Get all timesheet entries for this month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<TimesheetEntry> entries = timesheetEntryRepository.findByUserAndDateBetween(user, startDate, endDate);
        
        // Calculate totals
        double totalHours = 0;
        double regularHours = 0;
        double holidayHours = 0;
        
        for (TimesheetEntry entry : entries) {
            if (entry.getHoursWorked() != null) {
                totalHours += entry.getHoursWorked();
                
                if (entry.getHolidayType() != null) {
                    holidayHours += entry.getHoursWorked();
                } else {
                    regularHours += entry.getHoursWorked();
                }
            }
            
            // Associate entry with this monthly timesheet
            entry.setMonthlyTimesheet(timesheet);
            // Add entry to timesheet collection
            timesheet.getEntries().add(entry);
            
            // Save the entry
            timesheetEntryRepository.save(entry);
        }
        
        // Update timesheet totals
        timesheet.setTotalHoursWorked(totalHours);
        timesheet.setRegularHours(regularHours);
        timesheet.setHolidayHours(holidayHours);
        timesheet.setTotalHoursReportedByUploader(null);
        
        // Save the monthly timesheet
        MonthlyTimesheet savedTimesheet = monthlyTimesheetRepository.save(timesheet);
        
        return ResponseEntity.ok(savedTimesheet);
    }
    
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_USER_EMP') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> submitMonthlyTimesheet(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        MonthlyTimesheet timesheet = monthlyTimesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Monthly timesheet not found"));
        
        // Verify the user owns this timesheet
        if (!timesheet.getUser().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body("You don't have permission to submit this timesheet");
        }
        
        // Update timesheet status
        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmissionDate(LocalDate.now());
        
        // Save the updated timesheet
        monthlyTimesheetRepository.save(timesheet);
        
        return ResponseEntity.ok(timesheet);
    }
    
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> approveMonthlyTimesheet(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        MonthlyTimesheet timesheet = monthlyTimesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Monthly timesheet not found"));
        
        // Only submitted timesheets can be approved
        if (!timesheet.getStatus().equals(TimesheetStatus.SUBMITTED)) {
            return ResponseEntity.badRequest().body("Only submitted timesheets can be approved");
        }
        
        // Update timesheet status
        timesheet.setStatus(TimesheetStatus.APPROVED);
        timesheet.setApprovalDate(LocalDate.now());
        
        // Add comments if provided
        if (body != null && body.containsKey("comments")) {
            timesheet.setComments(body.get("comments"));
        }
        
        // Save the updated timesheet
        monthlyTimesheetRepository.save(timesheet);
        
        return ResponseEntity.ok(timesheet);
    }
    
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> rejectMonthlyTimesheet(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MonthlyTimesheet timesheet = monthlyTimesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Monthly timesheet not found"));
        
        // Only submitted timesheets can be rejected
        if (!timesheet.getStatus().equals(TimesheetStatus.SUBMITTED)) {
            return ResponseEntity.badRequest().body("Only submitted timesheets can be rejected");
        }
        
        // Comments are required for rejection
        if (!body.containsKey("comments") || body.get("comments").trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Comments are required when rejecting a timesheet");
        }
        
        // Update timesheet status
        timesheet.setStatus(TimesheetStatus.REJECTED);
        timesheet.setComments(body.get("comments"));
        
        // Save the updated timesheet
        monthlyTimesheetRepository.save(timesheet);
        
        return ResponseEntity.ok(timesheet);
    }
    
    @GetMapping("/admin/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<MonthlyTimesheet>> getPendingTimesheets() {
        List<MonthlyTimesheet> timesheets = monthlyTimesheetRepository.findByStatus(TimesheetStatus.SUBMITTED);
        return ResponseEntity.ok(timesheets);
    }

    @GetMapping("/{userId}/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getMonthlyTimesheetForUser(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<TimesheetEntry> entries = timesheetEntryRepository.findByUserAndYearAndMonth(user, year, month);
        return ResponseEntity.ok(entries);
    }
} 