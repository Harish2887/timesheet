package com.example.timesheet.controller;

import com.example.timesheet.model.HolidayType;
import com.example.timesheet.repository.HolidayTypeRepository;
import com.example.timesheet.service.HolidayTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/holidays")
public class HolidayTypeController {
    private static final Logger logger = LoggerFactory.getLogger(HolidayTypeController.class);
    
    @Autowired
    private HolidayTypeRepository holidayTypeRepository;

    @Autowired
    private HolidayTypeService holidayTypeService;
    
    @GetMapping("/")
    public ResponseEntity<List<HolidayType>> getAllHolidayTypes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("GET /holidays - Authentication: {}", auth != null ? auth.getName() : "null");
        
        try {
            List<HolidayType> holidayTypes = holidayTypeService.findAll();
            logger.info("Successfully retrieved {} holiday types", holidayTypes.size());
            return new ResponseEntity<>(holidayTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving holiday types: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("")
    public ResponseEntity<List<HolidayType>> getRootHolidayTypes() {
        // Add an empty path endpoint to handle direct "/api/holidays" requests
        List<HolidayType> holidayTypes = holidayTypeRepository.findAll();
        return ResponseEntity.ok(holidayTypes);
    }
    
    @GetMapping("/government")
    public ResponseEntity<List<HolidayType>> getGovernmentHolidayTypes() {
        List<HolidayType> holidayTypes = holidayTypeRepository.findByIsGovernmentHolidayTrue();
        return ResponseEntity.ok(holidayTypes);
    }
    
    @GetMapping("/range")
    public ResponseEntity<List<Map<String, Object>>> getHolidaysInRange(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        
        // Mock implementation to return some sample holidays
        // In a real implementation, you would fetch this from a database
        List<Map<String, Object>> holidays = new ArrayList<>();
        
        // Parse the start and end dates
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        
        // Add some sample Swedish holidays if they fall in the range
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 1, 1), "New Year's Day");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 1, 6), "Epiphany");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 5, 1), "Labor Day");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 6, 6), "National Day of Sweden");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 12, 24), "Christmas Eve");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 12, 25), "Christmas Day");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 12, 26), "Boxing Day");
        addHolidayIfInRange(holidays, start, end, LocalDate.of(start.getYear(), 12, 31), "New Year's Eve");
        
        // If the range spans multiple years, check next year's holidays too
        if (start.getYear() != end.getYear()) {
            addHolidayIfInRange(holidays, start, end, LocalDate.of(end.getYear(), 1, 1), "New Year's Day");
            addHolidayIfInRange(holidays, start, end, LocalDate.of(end.getYear(), 1, 6), "Epiphany");
            addHolidayIfInRange(holidays, start, end, LocalDate.of(end.getYear(), 5, 1), "Labor Day");
            addHolidayIfInRange(holidays, start, end, LocalDate.of(end.getYear(), 6, 6), "National Day of Sweden");
            // Add more as needed
        }
        
        return ResponseEntity.ok(holidays);
    }
    
    private void addHolidayIfInRange(List<Map<String, Object>> holidays, LocalDate start, LocalDate end, LocalDate holiday, String name) {
        if ((holiday.isEqual(start) || holiday.isAfter(start)) && (holiday.isEqual(end) || holiday.isBefore(end))) {
            Map<String, Object> holidayMap = new HashMap<>();
            holidayMap.put("date", holiday.toString());
            holidayMap.put("name", name);
            holidays.add(holidayMap);
        }
    }
    
    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createHolidayType(@RequestBody HolidayType holidayType) {
        HolidayType savedType = holidayTypeRepository.save(holidayType);
        return ResponseEntity.ok(savedType);
    }
} 