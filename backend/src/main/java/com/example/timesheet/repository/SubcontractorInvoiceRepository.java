package com.example.timesheet.repository;

import com.example.timesheet.model.SubcontractorInvoice;
import com.example.timesheet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractorInvoiceRepository extends JpaRepository<SubcontractorInvoice, Long> {
    List<SubcontractorInvoice> findByUser(User user);
    List<SubcontractorInvoice> findByUserAndYearAndMonth(User user, Integer year, Integer month);
    List<SubcontractorInvoice> findByYearAndMonth(Integer year, Integer month);
    List<SubcontractorInvoice> findByStatus(SubcontractorInvoice.InvoiceStatus status);
} 