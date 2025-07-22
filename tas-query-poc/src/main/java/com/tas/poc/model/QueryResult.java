package com.tas.poc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private String query;
    private List<Map<String, Object>> results;
    private int rowCount;
    private long executionTimeMs;
    private boolean success;
    private String errorMessage;
}