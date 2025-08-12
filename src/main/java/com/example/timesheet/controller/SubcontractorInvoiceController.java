package com.example.timesheet.controller;

import com.example.timesheet.model.SubcontractorInvoice;
import com.example.timesheet.model.User;
import com.example.timesheet.repository.SubcontractorInvoiceRepository;
import com.example.timesheet.repository.UserRepository;
import com.example.timesheet.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/invoices")
public class SubcontractorInvoiceController {

    @Autowired
    private SubcontractorInvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.invoice.upload.dir}")
    private String uploadDir;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId()).orElseThrow(() -> 
            new RuntimeException("User not found with id: " + userDetails.getId()));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_USER_SUB')")
    public ResponseEntity<?> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam("invoiceAmount") BigDecimal invoiceAmount,
            @RequestParam("hoursWorked") Double hoursWorked,
            @RequestParam("invoiceNumber") String invoiceNumber) {
        
        try {
            // Get current user
            User currentUser = getCurrentUser();
            
            // Check if invoice for this month already exists
            Optional<SubcontractorInvoice> existingInvoice = invoiceRepository
                .findByUserAndYearAndMonth(currentUser, year, month)
                .stream()
                .findFirst();
                
            if (existingInvoice.isPresent()) {
                return ResponseEntity
                    .badRequest()
                    .body("Invoice for this month already exists. Please update the existing invoice.");
            }
            
            // Create upload directory if it doesn't exist
            String yearMonthPath = year + "/" + month;
            File uploadPath = new File(uploadDir + "/" + yearMonthPath);
            if (!uploadPath.exists()) {
                uploadPath.mkdirs();
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = currentUser.getUsername() + "_" + year + "_" + month + "_" + System.currentTimeMillis() + fileExtension;
            
            // Save file
            Path filePath = Paths.get(uploadDir + "/" + yearMonthPath + "/" + uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create invoice record
            SubcontractorInvoice invoice = new SubcontractorInvoice();
            invoice.setUser(currentUser);
            invoice.setYear(year);
            invoice.setMonth(month);
            invoice.setInvoiceAmount(invoiceAmount);
            invoice.setHoursWorked(hoursWorked);
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setSubmissionDate(LocalDate.now());
            invoice.setInvoiceFilePath(yearMonthPath + "/" + uniqueFilename);
            invoice.setInvoiceFileName(originalFilename);
            invoice.setInvoiceFileContentType(file.getContentType());
            invoice.setStatus(SubcontractorInvoice.InvoiceStatus.SUBMITTED);
            
            // Save to database
            SubcontractorInvoice savedInvoice = invoiceRepository.save(invoice);
            
            return ResponseEntity.ok(savedInvoice);
        } catch (IOException e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to upload invoice: " + e.getMessage());
        }
    }
    
    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('ROLE_USER_SUB')")
    public ResponseEntity<List<SubcontractorInvoice>> getMyInvoices() {
        User currentUser = getCurrentUser();
        List<SubcontractorInvoice> invoices = invoiceRepository.findByUser(currentUser);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/my-invoices/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_USER_SUB')")
    public ResponseEntity<?> getMyInvoiceByMonth(
            @PathVariable("year") Integer year,
            @PathVariable("month") Integer month) {
        
        User currentUser = getCurrentUser();
        List<SubcontractorInvoice> invoices = invoiceRepository.findByUserAndYearAndMonth(
            currentUser, year, month);
            
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(invoices.get(0));
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<SubcontractorInvoice>> getAllInvoices() {
        List<SubcontractorInvoice> invoices = invoiceRepository.findAll();
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<SubcontractorInvoice>> getInvoicesByMonth(
            @PathVariable("year") Integer year,
            @PathVariable("month") Integer month) {
        
        List<SubcontractorInvoice> invoices = invoiceRepository.findByYearAndMonth(year, month);
        return ResponseEntity.ok(invoices);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateInvoiceStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "comments", required = false) String comments) {
        
        Optional<SubcontractorInvoice> optionalInvoice = invoiceRepository.findById(id);
        if (!optionalInvoice.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        SubcontractorInvoice invoice = optionalInvoice.get();
        try {
            SubcontractorInvoice.InvoiceStatus newStatus = SubcontractorInvoice.InvoiceStatus.valueOf(status);
            invoice.setStatus(newStatus);
            
            if (comments != null && !comments.trim().isEmpty()) {
                invoice.setComments(comments);
            }
            
            if (newStatus == SubcontractorInvoice.InvoiceStatus.APPROVED) {
                invoice.setApprovalDate(LocalDate.now());
            } else if (newStatus == SubcontractorInvoice.InvoiceStatus.PAID) {
                invoice.setPaymentDate(LocalDate.now());
            }
            
            invoiceRepository.save(invoice);
            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        }
    }
    
    @GetMapping("/summary/{year}/{month}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getMonthSummary(
            @PathVariable("year") Integer year,
            @PathVariable("month") Integer month) {
        
        List<SubcontractorInvoice> invoices = invoiceRepository.findByYearAndMonth(year, month);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        double totalHours = 0;
        int submittedCount = 0;
        int approvedCount = 0;
        int paidCount = 0;
        
        for (SubcontractorInvoice invoice : invoices) {
            totalAmount = totalAmount.add(invoice.getInvoiceAmount());
            totalHours += invoice.getHoursWorked();
            
            if (invoice.getStatus() == SubcontractorInvoice.InvoiceStatus.SUBMITTED) {
                submittedCount++;
            } else if (invoice.getStatus() == SubcontractorInvoice.InvoiceStatus.APPROVED) {
                approvedCount++;
            } else if (invoice.getStatus() == SubcontractorInvoice.InvoiceStatus.PAID) {
                paidCount++;
            }
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("year", year);
        summary.put("month", month);
        summary.put("totalInvoices", invoices.size());
        summary.put("totalAmount", totalAmount);
        summary.put("totalHours", totalHours);
        summary.put("submittedCount", submittedCount);
        summary.put("approvedCount", approvedCount);
        summary.put("paidCount", paidCount);
        
        return ResponseEntity.ok(summary);
    }
} 