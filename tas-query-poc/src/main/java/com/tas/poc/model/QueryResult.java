package com.tas.poc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a database query execution.
 * 
 * This model supports two representations of query results:
 * 1. As a list of maps (results) - for flexible JSON serialization
 * 2. As rows and columns - for table-like data processing
 * 
 * @author TAS Query POC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    /**
     * The original SQL query that was executed
     */
    private String query;
    
    /**
     * Results as a list of maps, where each map represents a row
     * with column names as keys and cell values as values
     */
    private List<Map<String, Object>> results;
    
    /**
     * Column names from the result set
     */
    private List<String> columns;
    
    /**
     * Raw result data as a list of rows, where each row is a list of values
     * in the same order as the columns list
     */
    private List<List<Object>> rows;
    
    /**
     * Total number of rows returned by the query
     */
    private int rowCount;
    
    /**
     * Time taken to execute the query in milliseconds
     */
    private long executionTimeMs;
    
    /**
     * Whether the query executed successfully
     */
    private boolean success;
    
    /**
     * Error message if the query failed
     */
    private String errorMessage;
    
    /**
     * Convenience method to get columns from results if not explicitly set
     */
    public List<String> getColumns() {
        if (columns != null) {
            return columns;
        }
        if (results != null && !results.isEmpty()) {
            return new ArrayList<>(results.get(0).keySet());
        }
        return new ArrayList<>();
    }
    
    /**
     * Convenience method to get rows from results if not explicitly set
     */
    public List<List<Object>> getRows() {
        if (rows != null) {
            return rows;
        }
        if (results != null) {
            List<List<Object>> convertedRows = new ArrayList<>();
            List<String> cols = getColumns();
            for (Map<String, Object> result : results) {
                List<Object> row = new ArrayList<>();
                for (String col : cols) {
                    row.add(result.get(col));
                }
                convertedRows.add(row);
            }
            return convertedRows;
        }
        return new ArrayList<>();
    }
}