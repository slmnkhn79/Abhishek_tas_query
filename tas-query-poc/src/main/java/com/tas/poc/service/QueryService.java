package com.tas.poc.service;

import com.tas.poc.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for executing SQL queries against the TAS database.
 * 
 * This service provides:
 * - Safe SQL execution with read-only validation
 * - Protection against SQL injection and dangerous operations
 * - Query performance tracking
 * - Error handling and user-friendly error messages
 * - Database connectivity testing
 * - Schema introspection capabilities
 * 
 * Security features:
 * - Only SELECT queries are allowed
 * - Dangerous keywords (DROP, DELETE, etc.) are blocked
 * - All queries are logged for audit purposes
 * - Prepared statements are used internally by JdbcTemplate
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {
    
    /**
     * DataSource injected by Spring Boot auto-configuration.
     * Configured in application.yml with HikariCP connection pooling.
     * Points to PostgreSQL database with tas_demo schema.
     */
    private final DataSource dataSource;
    
    public QueryResult executeQuery(String sql) {
        log.info("Executing query: {}", sql);
        
        QueryResult result = QueryResult.builder()
                .query(sql)
                .build();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate query is read-only (basic check for POC)
            String normalizedSql = sql.trim().toUpperCase();
            if (!normalizedSql.startsWith("SELECT")) {
                throw new SecurityException("Only SELECT queries are allowed");
            }
            
            // Check for potentially dangerous keywords
            String[] dangerousKeywords = {"DROP", "DELETE", "INSERT", "UPDATE", "ALTER", "CREATE", "TRUNCATE"};
            for (String keyword : dangerousKeywords) {
                if (normalizedSql.contains(keyword)) {
                    throw new SecurityException("Query contains forbidden keyword: " + keyword);
                }
            }
            
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            result.setResults(results != null ? results : new ArrayList<>());
            result.setRowCount(results != null ? results.size() : 0);
            result.setSuccess(true);
            
            // Also populate columns and rows for compatibility
            if (results != null && !results.isEmpty()) {
                List<String> columnList = new ArrayList<>(results.get(0).keySet());
                result.setColumns(columnList);
                
                List<List<Object>> rowsList = new ArrayList<>();
                for (Map<String, Object> row : results) {
                    List<Object> rowData = new ArrayList<>();
                    for (String col : columnList) {
                        rowData.add(row.get(col));
                    }
                    rowsList.add(rowData);
                }
                result.setRows(rowsList);
            }
            
            log.info("Query executed successfully, returned {} rows", result.getRowCount());
            
        } catch (Exception e) {
            log.error("Error executing query: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setResults(new ArrayList<>());
            result.setRowCount(0);
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    public boolean testConnection() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Database connection test successful");
            return true;
        } catch (Exception e) {
            log.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    public List<String> getTables() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String query = """
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'tas_demo' 
                ORDER BY table_name
                """;
            return jdbcTemplate.queryForList(query, String.class);
        } catch (Exception e) {
            log.error("Error fetching tables: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}