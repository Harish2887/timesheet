package com.example.timesheet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subcontractor_invoices")
public class SubcontractorInvoice {
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
    
    @Column(nullable = false)
    private BigDecimal invoiceAmount;
    
    @Column(nullable = false)
    private Double hoursWorked;
    
    @Column(nullable = false)
    private String invoiceNumber;
    
    @Column(nullable = false)
    private LocalDate submissionDate;
    
    @Column
    private LocalDate approvalDate;
    
    @Column
    private LocalDate paymentDate;
    
    @Column(nullable = false, length = 255)
    private String invoiceFilePath;
    
    @Column(nullable = false, length = 50)
    private String invoiceFileName;
    
    @Column(nullable = false, length = 255)
    private String invoiceFileContentType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.SUBMITTED;
    
    @Column
    private String comments;
    
    public enum InvoiceStatus {
        SUBMITTED,  // When invoice is submitted
        APPROVED,   // When invoice is approved
        REJECTED,   // When invoice is rejected
        PAID        // When invoice is paid
    }
} 