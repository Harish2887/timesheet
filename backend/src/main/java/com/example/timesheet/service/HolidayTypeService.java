package com.example.timesheet.service;

import com.example.timesheet.model.HolidayType;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing holiday types
 */
public interface HolidayTypeService {
    
    /**
     * Find all holiday types
     * @return list of all holiday types
     */
    List<HolidayType> findAll();
    
    /**
     * Find holiday type by ID
     * @param id the holiday type ID
     * @return the holiday type if found
     */
    Optional<HolidayType> findById(Long id);
    
    /**
     * Find government holiday types
     * @return list of government holiday types
     */
    List<HolidayType> findGovernmentHolidays();
    
    /**
     * Save a holiday type
     * @param holidayType the holiday type to save
     * @return the saved holiday type
     */
    HolidayType save(HolidayType holidayType);
    
    /**
     * Delete a holiday type
     * @param id the holiday type ID to delete
     */
    void deleteById(Long id);
} 