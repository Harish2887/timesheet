package com.example.timesheet.service.impl;

import com.example.timesheet.model.HolidayType;
import com.example.timesheet.repository.HolidayTypeRepository;
import com.example.timesheet.service.HolidayTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of HolidayTypeService
 */
@Service
public class HolidayTypeServiceImpl implements HolidayTypeService {
    
    private static final Logger logger = LoggerFactory.getLogger(HolidayTypeServiceImpl.class);
    
    @Autowired
    private HolidayTypeRepository holidayTypeRepository;
    
    @Override
    public List<HolidayType> findAll() {
        logger.info("Fetching all holiday types");
        return holidayTypeRepository.findAll();
    }
    
    @Override
    public Optional<HolidayType> findById(Long id) {
        logger.info("Fetching holiday type with id: {}", id);
        return holidayTypeRepository.findById(id);
    }
    
    @Override
    public List<HolidayType> findGovernmentHolidays() {
        logger.info("Fetching government holiday types");
        return holidayTypeRepository.findByIsGovernmentHolidayTrue();
    }
    
    @Override
    public HolidayType save(HolidayType holidayType) {
        logger.info("Saving holiday type: {}", holidayType.getName());
        return holidayTypeRepository.save(holidayType);
    }
    
    @Override
    public void deleteById(Long id) {
        logger.info("Deleting holiday type with id: {}", id);
        holidayTypeRepository.deleteById(id);
    }
} 