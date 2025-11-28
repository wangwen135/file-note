package com.example.pasteboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.pasteboard.config")
public class PasteboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(PasteboardApplication.class, args);
    }
}
