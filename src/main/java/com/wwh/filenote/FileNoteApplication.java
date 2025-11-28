package com.wwh.filenote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.wwh.filenote.config")
public class FileNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileNoteApplication.class, args);
    }
}
