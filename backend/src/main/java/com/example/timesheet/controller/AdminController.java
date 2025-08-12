package com.example.timesheet.controller;

import com.example.timesheet.model.MonthlyTimesheet;
import com.example.timesheet.model.Role;
import com.example.timesheet.model.SubcontractorInvoice;
import com.example.timesheet.model.TimesheetEntry;
import com.example.timesheet.model.User;
import com.example.timesheet.payload.request.UserCreateRequest;
import com.example.timesheet.payload.response.MonthSummaryResponse;
import com.example.timesheet.repository.MonthlyTimesheetRepository;
import com.example.timesheet.repository.RoleRepository;
import com.example.timesheet.repository.SubcontractorInvoiceRepository;
import com.example.timesheet.repository.TimesheetEntryRepository;
import com.example.timesheet.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private TimesheetEntryRepository timesheetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private MonthlyTimesheetRepository monthlyTimesheetRepository;
    
    @Autowired
    private SubcontractorInvoiceRepository invoiceRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/timesheet")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<TimesheetEntry>> getAllTimesheets() {
        List<TimesheetEntry> entries = timesheetRepository.findAll();
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/timesheet/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getUserTimesheets(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TimesheetEntry> entries = timesheetRepository.findByUser(user);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/timesheet/monthly")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<MonthSummaryResponse>> getMonthlyTimesheets() {
        List<User> users = userRepository.findAll();
        List<TimesheetEntry> allEntries = timesheetRepository.findAll();
        
        // Group by user and month
        Map<Long, Map<String, List<TimesheetEntry>>> userMonthlyEntries = new HashMap<>();
        
        for (TimesheetEntry entry : allEntries) {
            Long userId = entry.getUser().getId();
            String monthKey = entry.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            userMonthlyEntries.computeIfAbsent(userId, k -> new HashMap<>());
            userMonthlyEntries.get(userId).computeIfAbsent(monthKey, k -> new ArrayList<>());
            userMonthlyEntries.get(userId).get(monthKey).add(entry);
        }
        
        // Create MonthSummaryResponse objects
        List<MonthSummaryResponse> response = new ArrayList<>();
        
        for (Long userId : userMonthlyEntries.keySet()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;
            
            Map<String, List<TimesheetEntry>> monthlyEntries = userMonthlyEntries.get(userId);
            
            for (String monthKey : monthlyEntries.keySet()) {
                List<TimesheetEntry> entries = monthlyEntries.get(monthKey);
                
                // Get year and month from monthKey (yyyy-MM)
                int year = Integer.parseInt(monthKey.substring(0, 4));
                int month = Integer.parseInt(monthKey.substring(5, 7));
                
                // Calculate month completion
                YearMonth yearMonth = YearMonth.of(year, month);
                LocalDate firstDay = yearMonth.atDay(1);
                LocalDate lastDay = yearMonth.atEndOfMonth();
                
                // Create a set of all dates that have entries
                Set<LocalDate> datesWithEntries = entries.stream()
                        .map(TimesheetEntry::getDate)
                        .collect(Collectors.toSet());
                
                // Find all weekdays in this month
                Set<LocalDate> allWeekdays = new HashSet<>();
                LocalDate date = firstDay;
                while (!date.isAfter(lastDay)) {
                    if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                        allWeekdays.add(date);
                    }
                    date = date.plusDays(1);
                }
                
                // Determine if all workdays have entries
                boolean isComplete = allWeekdays.stream().allMatch(datesWithEntries::contains);
                
                // Calculate completion percentage
                int totalDays = allWeekdays.size();
                int filledDays = (int) allWeekdays.stream().filter(datesWithEntries::contains).count();
                double completionPercentage = totalDays > 0 ? ((double) filledDays / totalDays) * 100 : 0;
                
                // Determine overall status
                String status = "PENDING";
                if (entries.stream().anyMatch(e -> "APPROVED".equals(e.getStatus()))) {
                    status = "APPROVED";
                } else if (entries.stream().anyMatch(e -> "REJECTED".equals(e.getStatus()))) {
                    status = "REJECTED";
                }
                
                // Create the response object
                MonthSummaryResponse summary = new MonthSummaryResponse();
                summary.setUserId(userId);
                summary.setUsername(user.getUsername());
                summary.setYear(year);
                summary.setMonth(month);
                summary.setMonthName(yearMonth.getMonth().toString());
                summary.setComplete(isComplete);
                summary.setTotalWorkdays(totalDays);
                summary.setFilledWorkdays(filledDays);
                summary.setCompletionPercentage(completionPercentage);
                summary.setStatus(status);
                summary.setEntriesCount(entries.size());
                
                response.add(summary);
            }
        }
        
        // Sort by year and month, then by username
        response.sort((a, b) -> {
            int yearCompare = Integer.compare(b.getYear(), a.getYear()); // Newest first
            if (yearCompare != 0) return yearCompare;
            
            int monthCompare = Integer.compare(b.getMonth(), a.getMonth()); // Newest first
            if (monthCompare != 0) return monthCompare;
            
            return a.getUsername().compareTo(b.getUsername());
        });
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }
    
    @PostMapping("/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest userRequest) {
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        // Create new user
        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setCompanyName(userRequest.getCompanyName());

        Set<String> strRoles = userRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default to USER_EMP if no role is specified
            Role employeeRole = roleRepository.findByName(Role.ERole.ROLE_USER_EMP)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(employeeRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                case "admin":
                    Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(adminRole);
                    break;
                case "user_emp":
                    Role empRole = roleRepository.findByName(Role.ERole.ROLE_USER_EMP)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(empRole);
                    break;
                case "user_pay":
                    Role payRole = roleRepository.findByName(Role.ERole.ROLE_USER_PAY)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(payRole);
                    break;
                case "user_sub":
                    Role subRole = roleRepository.findByName(Role.ERole.ROLE_USER_SUB)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(subRole);
                    break;
                default:
                    Role defaultRole = roleRepository.findByName(Role.ERole.ROLE_USER_EMP)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(defaultRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
    
    @GetMapping("/users/by-role/{role}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        Role.ERole roleEnum;
        try {
            roleEnum = Role.ERole.valueOf("ROLE_" + role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        List<User> users = userRepository.findByRoleName(roleEnum);
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/timesheet/{id}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateTimesheetStatus(@PathVariable Long id, @RequestParam String status) {
        if (!status.equals("APPROVED") && !status.equals("REJECTED") && !status.equals("PENDING")) {
            return ResponseEntity.badRequest().body("Invalid status. Must be APPROVED, REJECTED, or PENDING");
        }
        
        TimesheetEntry entry = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet entry not found"));
        
        entry.setStatus(status);
        timesheetRepository.save(entry);
        
        return ResponseEntity.ok(entry);
    }
    
    @PutMapping("/timesheet/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> approveTimesheet(@PathVariable Long id) {
        TimesheetEntry entry = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet entry not found"));
        
        entry.setStatus("APPROVED");
        timesheetRepository.save(entry);
        
        return ResponseEntity.ok(entry);
    }
    
    @PutMapping("/timesheet/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> rejectTimesheet(@PathVariable Long id) {
        TimesheetEntry entry = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet entry not found"));
        
        entry.setStatus("REJECTED");
        timesheetRepository.save(entry);
        
        return ResponseEntity.ok(entry);
    }
    
    @PutMapping("/timesheet/monthly/{userId}/{year}/{month}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> approveMonthlyTimesheet(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TimesheetEntry> entries = timesheetRepository.findByUserAndYearAndMonth(user, year, month);
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body("No timesheet entries found for this month");
        }
        
        for (TimesheetEntry entry : entries) {
            entry.setStatus("APPROVED");
            timesheetRepository.save(entry);
        }
        
        // If there's a monthly timesheet record, update its status too
        Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        if (monthlyTimesheet.isPresent()) {
            MonthlyTimesheet mt = monthlyTimesheet.get();
            mt.setStatus(MonthlyTimesheet.TimesheetStatus.APPROVED);
            mt.setApprovalDate(LocalDate.now());
            monthlyTimesheetRepository.save(mt);
        }
        
        return ResponseEntity.ok("Monthly timesheet approved successfully");
    }
    
    @PutMapping("/timesheet/monthly/{userId}/{year}/{month}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> rejectMonthlyTimesheet(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<TimesheetEntry> entries = timesheetRepository.findByUserAndYearAndMonth(user, year, month);
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body("No timesheet entries found for this month");
        }
        
        for (TimesheetEntry entry : entries) {
            entry.setStatus("REJECTED");
            timesheetRepository.save(entry);
        }
        
        // If there's a monthly timesheet record, update its status too
        Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        if (monthlyTimesheet.isPresent()) {
            MonthlyTimesheet mt = monthlyTimesheet.get();
            mt.setStatus(MonthlyTimesheet.TimesheetStatus.REJECTED);
            monthlyTimesheetRepository.save(mt);
        }
        
        return ResponseEntity.ok("Monthly timesheet rejected successfully");
    }
    
    @PutMapping("/timesheet/monthly/{userId}/{year}/{month}/pay")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> markTimesheetAsPaid(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // If there's a monthly timesheet record, update its status to PAID
        Optional<MonthlyTimesheet> monthlyTimesheet = monthlyTimesheetRepository.findByUserAndYearAndMonth(user, year, month);
        if (monthlyTimesheet.isPresent()) {
            MonthlyTimesheet mt = monthlyTimesheet.get();
            if (mt.getStatus() != MonthlyTimesheet.TimesheetStatus.APPROVED) {
                return ResponseEntity.badRequest().body("Cannot mark as paid: timesheet is not approved");
            }
            mt.setStatus(MonthlyTimesheet.TimesheetStatus.PAID);
            mt.setPaymentDate(LocalDate.now());
            monthlyTimesheetRepository.save(mt);
            return ResponseEntity.ok("Monthly timesheet marked as paid");
        } else {
            return ResponseEntity.badRequest().body("Monthly timesheet not found");
        }
    }
    
    @GetMapping("/export/timesheets/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportTimesheets(
            @PathVariable int year,
            @PathVariable int month) throws IOException {
        
        // Get all users with their timesheets for the specified month
        List<User> users = userRepository.findAll();
        Map<Long, List<TimesheetEntry>> userTimesheets = new HashMap<>();
        
        for (User user : users) {
            List<TimesheetEntry> entries = timesheetRepository.findByUserAndYearAndMonth(user, year, month);
            if (!entries.isEmpty()) {
                userTimesheets.put(user.getId(), entries);
            }
        }
        
        // Create Excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            
            // Create sheet for overview
            Sheet overviewSheet = workbook.createSheet("Overview");
            
            // Create header row
            Row headerRow = overviewSheet.createRow(0);
            String[] headers = {"User ID", "Username", "Email", "Total Days", "Total Hours", "Total Support Hours", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data
            int rowNum = 1;
            for (Long userId : userTimesheets.keySet()) {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;
                
                List<TimesheetEntry> entries = userTimesheets.get(userId);
                
                Row row = overviewSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(entries.size());
                
                double totalHours = entries.stream()
                        .mapToDouble(TimesheetEntry::getHoursWorked)
                        .sum();
                row.createCell(4).setCellValue(totalHours);

                double totalSupportHours = entries.stream()
                        .mapToDouble(e -> e.getSupportHours() != null ? e.getSupportHours() : 0.0)
                        .sum();
                row.createCell(5).setCellValue(totalSupportHours);
                
                String status = "MIXED";
                if (entries.stream().allMatch(e -> "APPROVED".equals(e.getStatus()))) {
                    status = "APPROVED";
                } else if (entries.stream().allMatch(e -> "REJECTED".equals(e.getStatus()))) {
                    status = "REJECTED";
                } else if (entries.stream().allMatch(e -> "PENDING".equals(e.getStatus()))) {
                    status = "PENDING";
                }
                row.createCell(6).setCellValue(status);
                
                // Create user detail sheet
                Sheet userSheet = workbook.createSheet(user.getUsername());
                
                // Create header row
                Row userHeaderRow = userSheet.createRow(0);
                String[] userHeaders = {"Date", "Hours Worked", "Support Hours", "Holiday Type", "Description", "Status"};
                for (int i = 0; i < userHeaders.length; i++) {
                    Cell cell = userHeaderRow.createCell(i);
                    cell.setCellValue(userHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Fill user data
                int userRowNum = 1;
                for (TimesheetEntry entry : entries) {
                    Row userRow = userSheet.createRow(userRowNum++);
                    
                    Cell dateCell = userRow.createCell(0);
                    dateCell.setCellValue(entry.getDate().toString());
                    
                    userRow.createCell(1).setCellValue(entry.getHoursWorked());
                    userRow.createCell(2).setCellValue(entry.getSupportHours() != null ? entry.getSupportHours() : 0.0);
                    
                    String holidayType = entry.getHolidayType() != null ? 
                            entry.getHolidayType().getName() : "Regular Workday";
                    userRow.createCell(3).setCellValue(holidayType);
                    
                    userRow.createCell(4).setCellValue(entry.getDescription() != null ? 
                            entry.getDescription() : "");
                    
                    userRow.createCell(5).setCellValue(entry.getStatus());
                }
                
                // Auto-size columns
                for (int i = 0; i < userHeaders.length; i++) {
                    userSheet.autoSizeColumn(i);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                overviewSheet.autoSizeColumn(i);
            }
            
            // Write to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            // Set response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "timesheet_export_" + year + "_" + month + ".xlsx";
            responseHeaders.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());
        }
    }

    @GetMapping("/export/timesheets/{userId}/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportTimesheetsForUser(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TimesheetEntry> entries = timesheetRepository.findByUserAndYearAndMonth(user, year, month);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create user detail sheet
            String sheetName = user.getUsername() + "-" + year + "-" + String.format("%02d", month);
            Sheet userSheet = workbook.createSheet(sheetName);

            // Header
            Row userHeaderRow = userSheet.createRow(0);
            String[] userHeaders = {"Date", "Hours Worked", "Holiday Type", "Description", "Status"};
            for (int i = 0; i < userHeaders.length; i++) {
                Cell cell = userHeaderRow.createCell(i);
                cell.setCellValue(userHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Rows
            int userRowNum = 1;
            for (TimesheetEntry entry : entries) {
                Row userRow = userSheet.createRow(userRowNum++);
                userRow.createCell(0).setCellValue(entry.getDate().toString());
                userRow.createCell(1).setCellValue(entry.getHoursWorked());
                String holidayType = entry.getHolidayType() != null ? entry.getHolidayType().getName() : "Regular Workday";
                userRow.createCell(2).setCellValue(holidayType);
                userRow.createCell(3).setCellValue(entry.getDescription() != null ? entry.getDescription() : "");
                userRow.createCell(4).setCellValue(entry.getStatus());
            }

            // Auto-size columns
            for (int i = 0; i < userHeaders.length; i++) {
                userSheet.autoSizeColumn(i);
            }

            // Write to output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "timesheet_" + user.getUsername() + "_" + year + "_" + String.format("%02d", month) + ".xlsx";
            responseHeaders.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());
        }
    }
    
    @GetMapping("/export/invoices/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportInvoices(
            @PathVariable int year,
            @PathVariable int month) throws IOException {
        
        // Get all invoices for the specified month
        List<SubcontractorInvoice> invoices = invoiceRepository.findByYearAndMonth(year, month);
        
        // Create Excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create sheet
            Sheet sheet = workbook.createSheet("Invoices");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Invoice ID", "Subcontractor", "Invoice Number", "Amount", "Hours", 
                    "Submission Date", "Status", "Invoice Filename"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data
            int rowNum = 1;
            for (SubcontractorInvoice invoice : invoices) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(invoice.getId());
                row.createCell(1).setCellValue(invoice.getUser().getUsername());
                row.createCell(2).setCellValue(invoice.getInvoiceNumber());
                row.createCell(3).setCellValue(invoice.getInvoiceAmount().doubleValue());
                row.createCell(4).setCellValue(invoice.getHoursWorked());
                row.createCell(5).setCellValue(invoice.getSubmissionDate().toString());
                row.createCell(6).setCellValue(invoice.getStatus().toString());
                row.createCell(7).setCellValue(invoice.getInvoiceFileName());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            // Set response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "invoice_export_" + year + "_" + month + ".xlsx";
            responseHeaders.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(outputStream.toByteArray());
        }
    }
} 