package com.example.timesheet.config;

import com.example.timesheet.model.HolidayType;
import com.example.timesheet.model.Role;
import com.example.timesheet.repository.HolidayTypeRepository;
import com.example.timesheet.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private HolidayTypeRepository holidayTypeRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.count() == 0) {
            Role employeeRole = new Role();
            employeeRole.setName(Role.ERole.ROLE_USER_EMP);
            roleRepository.save(employeeRole);
            
            Role adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
            
            System.out.println("Roles initialized successfully!");
        }
        
        // Initialize Swedish holiday types if they don't exist
        if (holidayTypeRepository.count() == 0) {
            // Create Swedish government holiday types
            createHolidayType("Sjukledighet", "Sick leave", true);
            createHolidayType("Föräldraledighet", "Parental leave", true);
            createHolidayType("Semester", "Vacation", true);
            createHolidayType("VAB (Vård av barn)", "Care of child", true);
            createHolidayType("Tjänstledighet", "Leave of absence", true);
            createHolidayType("Studieledighet", "Study leave", true);
            
            // Create company-specific holiday types
            createHolidayType("Work from home", "Remote work day", false);
            createHolidayType("Conference", "Attending a conference", false);
            createHolidayType("Training", "Training/education day", false);
            
            System.out.println("Holiday types initialized successfully!");
        }
    }
    
    private void createHolidayType(String name, String description, boolean isGovernment) {
        HolidayType holidayType = new HolidayType();
        holidayType.setName(name);
        holidayType.setDescription(description);
        holidayType.setGovernmentHoliday(isGovernment);
        holidayTypeRepository.save(holidayType);
    }
} 