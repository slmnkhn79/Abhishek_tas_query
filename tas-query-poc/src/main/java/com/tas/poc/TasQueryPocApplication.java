package com.tas.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the TAS Query POC (Proof of Concept) application.
 * 
 * This Spring Boot application provides a natural language interface for querying
 * a Time and Attendance System (TAS) database. It demonstrates:
 * - Natural language to SQL conversion
 * - Intelligent chat functionality with insights
 * - Data visualization capabilities
 * - Conversational context management
 * 
 * The application is designed to be enhanced with AI capabilities using Spring AI
 * and Ollama in future phases.
 * 
 * @author TAS Query POC Team
 * @version 1.0.0
 */
@SpringBootApplication
public class TasQueryPocApplication {

    /**
     * Main method that launches the Spring Boot application.
     * 
     * This method:
     * 1. Initializes the Spring application context
     * 2. Starts the embedded web server (Tomcat by default)
     * 3. Performs component scanning from this package and sub-packages
     * 4. Loads configuration from application.yml
     * 5. Establishes database connections
     * 
     * @param args Command line arguments (currently not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(TasQueryPocApplication.class, args);
    }
}