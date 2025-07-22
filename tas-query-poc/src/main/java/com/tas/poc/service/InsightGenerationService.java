package com.tas.poc.service;

import com.tas.poc.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for generating intelligent insights from query results.
 * 
 * This service analyzes raw SQL query results and produces:
 * - Human-readable summaries of the data
 * - Key findings and trends
 * - Statistical analysis (averages, totals, distributions)
 * - Anomaly detection and warnings
 * - Context-aware follow-up suggestions
 * - Business intelligence insights
 * 
 * The service uses the Strategy pattern to provide specialized insight generation
 * for different types of queries:
 * - Colleague distribution insights
 * - Exception analysis insights
 * - Trend identification for time-series data
 * - Comparative analysis across entities
 * - Performance metrics and KPIs
 * 
 * This transforms the application from a simple query tool into an intelligent
 * data assistant that helps users understand their data without requiring
 * technical knowledge of SQL or data analysis.
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Service
public class InsightGenerationService {
    
    /**
     * Map of query patterns to their specialized insight generation strategies.
     * Each strategy understands the specific context and meaning of its query type
     * and generates appropriate insights, findings, and recommendations.
     */
    private static final Map<String, InsightStrategy> INSIGHT_STRATEGIES = new HashMap<>();
    
    static {
        INSIGHT_STRATEGIES.put("colleagues by location", new ColleagueLocationInsightStrategy());
        INSIGHT_STRATEGIES.put("exceptions by type", new ExceptionTypeInsightStrategy());
        INSIGHT_STRATEGIES.put("daily exceptions", new DailyExceptionInsightStrategy());
        INSIGHT_STRATEGIES.put("tenant overview", new TenantOverviewInsightStrategy());
        INSIGHT_STRATEGIES.put("shift patterns", new ShiftPatternInsightStrategy());
        INSIGHT_STRATEGIES.put("exception status distribution", new ExceptionStatusInsightStrategy());
    }
    
    public Insights generateInsights(String query, QueryResult result, ChartData chartData) {
        if (!result.isSuccess() || result.getResults().isEmpty()) {
            return Insights.builder()
                    .summary("No data found for your query.")
                    .keyFindings(List.of())
                    .followUpSuggestions(getDefaultFollowUpSuggestions())
                    .build();
        }
        
        String queryPattern = findQueryPattern(query.toLowerCase());
        InsightStrategy strategy = INSIGHT_STRATEGIES.get(queryPattern);
        
        if (strategy != null) {
            return strategy.generateInsights(result, chartData);
        }
        
        return generateGenericInsights(result);
    }
    
    private String findQueryPattern(String query) {
        for (String pattern : INSIGHT_STRATEGIES.keySet()) {
            if (query.contains(pattern)) {
                return pattern;
            }
        }
        return null;
    }
    
    private Insights generateGenericInsights(QueryResult result) {
        List<Finding> findings = new ArrayList<>();
        int rowCount = result.getRowCount();
        
        findings.add(Finding.builder()
                .type("HIGHLIGHT")
                .message(String.format("Found %d records", rowCount))
                .significance(0.5)
                .build());
        
        return Insights.builder()
                .summary(String.format("Your query returned %d results.", rowCount))
                .keyFindings(findings)
                .followUpSuggestions(getDefaultFollowUpSuggestions())
                .explanation("The data has been retrieved successfully.")
                .build();
    }
    
    private List<String> getDefaultFollowUpSuggestions() {
        return List.of(
            "Show tenant overview",
            "Show exceptions by type",
            "Show colleagues by location"
        );
    }
    
    // Base interface for insight strategies
    private interface InsightStrategy {
        Insights generateInsights(QueryResult result, ChartData chartData);
    }
    
    // Colleague Location Insights
    private static class ColleagueLocationInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Find location with most colleagues
            Map<String, Object> maxLocation = data.stream()
                    .max(Comparator.comparing(row -> ((Number) row.get("colleague_count")).longValue()))
                    .orElse(null);
            
            if (maxLocation != null) {
                findings.add(Finding.builder()
                        .type("HIGHLIGHT")
                        .message(String.format("%s has the most colleagues", maxLocation.get("location_name")))
                        .value(String.valueOf(maxLocation.get("colleague_count")))
                        .significance(0.9)
                        .build());
            }
            
            // Calculate total colleagues
            long totalColleagues = data.stream()
                    .mapToLong(row -> ((Number) row.get("colleague_count")).longValue())
                    .sum();
            
            // Find understaffed locations
            long understaffedCount = data.stream()
                    .filter(row -> ((Number) row.get("colleague_count")).longValue() < 3)
                    .count();
            
            if (understaffedCount > 0) {
                findings.add(Finding.builder()
                        .type("WARNING")
                        .message(String.format("%d locations have fewer than 3 colleagues", understaffedCount))
                        .significance(0.7)
                        .build());
            }
            
            String summary = String.format("Found %d colleagues distributed across %d locations.", 
                    totalColleagues, data.size());
            
            return Insights.builder()
                    .summary(summary)
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show colleague activity",
                        "Show exceptions by location",
                        "Show shift coverage by location"
                    ))
                    .highlights(Insights.DataHighlights.builder()
                            .total(totalColleagues)
                            .topValue(maxLocation != null ? (String) maxLocation.get("location_name") : null)
                            .build())
                    .build();
        }
    }
    
    // Exception Type Insights
    private static class ExceptionTypeInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Most common exception type
            Map<String, Object> mostCommon = data.stream()
                    .max(Comparator.comparing(row -> ((Number) row.get("exception_count")).longValue()))
                    .orElse(null);
            
            if (mostCommon != null) {
                findings.add(Finding.builder()
                        .type("TREND")
                        .message(String.format("%s is the most common exception type", mostCommon.get("exception_type")))
                        .value(String.valueOf(mostCommon.get("exception_count")))
                        .significance(0.8)
                        .build());
                
                // Average duration for most common type
                Number avgDuration = (Number) mostCommon.get("avg_duration_mins");
                if (avgDuration != null) {
                    findings.add(Finding.builder()
                            .type("HIGHLIGHT")
                            .message(String.format("Average duration: %.1f minutes", avgDuration.doubleValue()))
                            .significance(0.6)
                            .build());
                }
            }
            
            // Total exceptions
            long totalExceptions = data.stream()
                    .mapToLong(row -> ((Number) row.get("exception_count")).longValue())
                    .sum();
            
            String summary = String.format("Analyzed %d total exceptions across %d different types.", 
                    totalExceptions, data.size());
            
            return Insights.builder()
                    .summary(summary)
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show daily exceptions",
                        "Show exception status distribution",
                        "Show exceptions for specific colleague"
                    ))
                    .build();
        }
    }
    
    // Daily Exception Insights
    private static class DailyExceptionInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Calculate resolution rate
            long totalExceptions = data.stream()
                    .mapToLong(row -> ((Number) row.get("total_exceptions")).longValue())
                    .sum();
            
            long totalResolved = data.stream()
                    .mapToLong(row -> ((Number) row.get("resolved_count")).longValue())
                    .sum();
            
            double resolutionRate = totalExceptions > 0 ? 
                    (totalResolved * 100.0 / totalExceptions) : 0;
            
            findings.add(Finding.builder()
                    .type("HIGHLIGHT")
                    .message(String.format("Overall resolution rate: %.1f%%", resolutionRate))
                    .significance(0.8)
                    .build());
            
            // Find peak day
            Map<String, Object> peakDay = data.stream()
                    .max(Comparator.comparing(row -> ((Number) row.get("total_exceptions")).longValue()))
                    .orElse(null);
            
            if (peakDay != null) {
                findings.add(Finding.builder()
                        .type("ANOMALY")
                        .message(String.format("Peak exception day: %s", peakDay.get("exception_date")))
                        .value(String.valueOf(peakDay.get("total_exceptions")))
                        .significance(0.7)
                        .build());
            }
            
            // Trend analysis
            if (data.size() > 7) {
                List<Long> recentCounts = data.stream()
                        .limit(7)
                        .map(row -> ((Number) row.get("total_exceptions")).longValue())
                        .collect(Collectors.toList());
                
                double avgRecent = recentCounts.stream().mapToLong(Long::longValue).average().orElse(0);
                double avgOverall = data.stream()
                        .mapToLong(row -> ((Number) row.get("total_exceptions")).longValue())
                        .average().orElse(0);
                
                String trend = avgRecent > avgOverall * 1.1 ? "increasing" : 
                              avgRecent < avgOverall * 0.9 ? "decreasing" : "stable";
                
                findings.add(Finding.builder()
                        .type("TREND")
                        .message(String.format("Exception trend is %s", trend))
                        .significance(0.6)
                        .build());
            }
            
            String summary = String.format("Analyzed exception trends over %d days with %d total exceptions.", 
                    data.size(), totalExceptions);
            
            return Insights.builder()
                    .summary(summary)
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show exceptions by type",
                        "Show unresolved exceptions",
                        "Show exception details for peak day"
                    ))
                    .highlights(Insights.DataHighlights.builder()
                            .total(totalExceptions)
                            .trend(totalExceptions > 0 ? "active" : "none")
                            .build())
                    .build();
        }
    }
    
    // Tenant Overview Insights
    private static class TenantOverviewInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Active vs Inactive tenants
            long activeTenants = data.stream()
                    .filter(row -> Boolean.TRUE.equals(row.get("is_active")))
                    .count();
            
            findings.add(Finding.builder()
                    .type("HIGHLIGHT")
                    .message(String.format("%d of %d tenants are active", activeTenants, data.size()))
                    .significance(0.8)
                    .build());
            
            // Tenant with most colleagues
            Map<String, Object> largestTenant = data.stream()
                    .max(Comparator.comparing(row -> ((Number) row.get("colleague_count")).longValue()))
                    .orElse(null);
            
            if (largestTenant != null) {
                findings.add(Finding.builder()
                        .type("HIGHLIGHT")
                        .message(String.format("%s has the most colleagues", largestTenant.get("tenant_name")))
                        .value(String.valueOf(largestTenant.get("colleague_count")))
                        .significance(0.7)
                        .build());
            }
            
            // Tenants without shifts
            long tenantsWithoutShifts = data.stream()
                    .filter(row -> ((Number) row.get("shift_count")).longValue() == 0)
                    .count();
            
            if (tenantsWithoutShifts > 0) {
                findings.add(Finding.builder()
                        .type("WARNING")
                        .message(String.format("%d tenants have no planned shifts", tenantsWithoutShifts))
                        .significance(0.6)
                        .build());
            }
            
            return Insights.builder()
                    .summary(String.format("Overview of %d tenants in the system.", data.size()))
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show inactive tenant details",
                        "Show shifts for specific tenant",
                        "Show colleague distribution by tenant"
                    ))
                    .build();
        }
    }
    
    // Shift Pattern Insights
    private static class ShiftPatternInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Peak shift hour
            Map<String, Object> peakHour = data.stream()
                    .max(Comparator.comparing(row -> ((Number) row.get("shift_count")).longValue()))
                    .orElse(null);
            
            if (peakHour != null) {
                int hour = ((Number) peakHour.get("shift_hour")).intValue();
                findings.add(Finding.builder()
                        .type("HIGHLIGHT")
                        .message(String.format("Most shifts start at %d:00", hour))
                        .value(String.valueOf(peakHour.get("shift_count")))
                        .significance(0.8)
                        .build());
            }
            
            // Total shifts analyzed
            long totalShifts = data.stream()
                    .mapToLong(row -> ((Number) row.get("shift_count")).longValue())
                    .sum();
            
            return Insights.builder()
                    .summary(String.format("Analyzed %d shifts across different start times.", totalShifts))
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show shift coverage by day",
                        "Show colleague shift assignments",
                        "Show shift duration analysis"
                    ))
                    .build();
        }
    }
    
    // Exception Status Distribution Insights
    private static class ExceptionStatusInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(QueryResult result, ChartData chartData) {
            List<Map<String, Object>> data = result.getResults();
            List<Finding> findings = new ArrayList<>();
            
            // Group by status
            Map<String, Long> statusCounts = data.stream()
                    .collect(Collectors.groupingBy(
                        row -> (String) row.get("status"),
                        Collectors.summingLong(row -> ((Number) row.get("count")).longValue())
                    ));
            
            // Calculate percentages
            long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            
            statusCounts.forEach((status, count) -> {
                double percentage = (count * 100.0) / total;
                findings.add(Finding.builder()
                        .type("HIGHLIGHT")
                        .message(String.format("%s: %.1f%% of exceptions", status, percentage))
                        .value(String.valueOf(count))
                        .significance(0.6)
                        .build());
            });
            
            return Insights.builder()
                    .summary(String.format("Exception status breakdown across %d tenants.", 
                            data.stream().map(row -> row.get("tenant_name")).distinct().count()))
                    .keyFindings(findings)
                    .followUpSuggestions(List.of(
                        "Show open exceptions details",
                        "Show exception resolution time",
                        "Show exceptions by tenant"
                    ))
                    .build();
        }
    }
}