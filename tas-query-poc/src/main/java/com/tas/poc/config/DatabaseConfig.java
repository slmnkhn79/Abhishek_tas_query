package com.tas.poc.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration class for the TAS Query POC application.
 * 
 * This configuration class sets up:
 * - JPA repository scanning for database access objects
 * - Entity scanning for domain models
 * - Transaction management for database operations
 * 
 * The actual database connection properties are configured in application.yml
 * including connection pooling via HikariCP.
 * 
 * Current configuration targets PostgreSQL with the tas_demo schema containing:
 * - tenant: Multi-tenant organization data
 * - location: Physical locations for each tenant
 * - colleague_details: Employee information
 * - planned_shift: Work schedule data
 * - exception: Attendance exceptions/anomalies
 * - exception_detail: Detailed exception information
 * - exception_audit: Audit trail for exceptions
 */
@Configuration
@EnableTransactionManagement  // Enables Spring's annotation-driven transaction management
@EnableJpaRepositories(basePackages = "com.tas.poc.repository")  // Scans for JPA repositories
@EntityScan(basePackages = "com.tas.poc.model")  // Scans for JPA entities
public class DatabaseConfig {
    
    // Note: Additional database configuration can be added here such as:
    // - Custom DataSource beans for multiple databases
    // - JPA property customization
    // - Custom transaction managers
    // - Database migration tools (Flyway/Liquibase)
    
    // For this POC, we rely on Spring Boot's auto-configuration
    // with properties defined in application.yml
}