package com.example.timesheet.repository;

import com.example.timesheet.model.HolidayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HolidayTypeRepository extends JpaRepository<HolidayType, Long> {
    List<HolidayType> findByIsGovernmentHolidayTrue();
} 