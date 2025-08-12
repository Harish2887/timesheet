package com.example.timesheet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class FileStorageConfig {

    @Value("${app.invoice.upload.dir}")
    private String uploadDir;

    @Bean
    public void initStorageDirectories() {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Could not create directory for file uploads: " + uploadDir);
            }
        }
    }
} 