package com.tas.poc.service;

import com.tas.poc.model.Insights;
import com.tas.poc.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
 * - Context-aware recommendations
 * - Business intelligence insights
 * - Predictive analytics integration
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
@RequiredArgsConstructor
public class InsightGenerationService {
    
    private final PredictiveAnalyticsService predictiveAnalyticsService;
    
    /**
     * Generates comprehensive insights from query results.
     * Includes basic analysis enhanced with predictive analytics.
     * 
     * @param query The natural language query
     * @param result The query execution results
     * @return Insights object with findings and recommendations
     */
    public Insights generateInsights(String query, QueryResult result) {
        log.debug("Generating insights for query: {}", query);
        
        if (result.getRows().isEmpty()) {
            return Insights.builder()
                    .summary("No data found for your query.")
                    .keyFindings(List.of("No results returned from the database"))
                    .build();
        }
        
        // Analyze query type and generate appropriate insights
        String lowerQuery = query.toLowerCase();
        InsightStrategy strategy = determineStrategy(lowerQuery);
        
        Insights basicInsights = strategy.generateInsights(query, result);
        
        // Enhance with predictive analytics
        Map<String, Object> predictions = predictiveAnalyticsService.generatePredictiveInsights(query, result);
        
        return enhanceWithPredictions(basicInsights, predictions);
    }
    
    /**
     * Determines the appropriate insight generation strategy based on query type.
     * 
     * @param query The lowercase query string
     * @return Appropriate InsightStrategy implementation
     */
    private InsightStrategy determineStrategy(String query) {
        if (query.contains("exception")) {
            return new ExceptionInsightStrategy();
        } else if (query.contains("colleague") || query.contains("employee")) {
            return new ColleagueInsightStrategy();
        } else if (query.contains("tenant")) {
            return new TenantInsightStrategy();
        } else if (query.contains("shift") || query.contains("schedule")) {
            return new ShiftInsightStrategy();
        } else if (query.contains("location")) {
            return new LocationInsightStrategy();
        } else {
            return new GenericInsightStrategy();
        }
    }
    
    /**
     * Base interface for insight generation strategies.
     */
    private interface InsightStrategy {
        Insights generateInsights(String query, QueryResult result);
    }
    
    /**
     * Generic insight strategy for unspecified query types.
     */
    private class GenericInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            findings.add(String.format("Retrieved %d records", result.getRows().size()));
            
            // Analyze numeric columns
            List<String> numericColumns = findNumericColumns(result);
            for (String column : numericColumns) {
                List<Double> values = extractNumericValues(result, column);
                if (!values.isEmpty()) {
                    double avg = calculateAverage(values);
                    findings.add(String.format("Average %s: %.2f", column, avg));
                    trends.put(column + "_average", avg);
                }
            }
            
            return Insights.builder()
                    .summary(String.format("Query returned %d results with %d columns.", 
                            result.getRows().size(), result.getColumns().size()))
                    .keyFindings(findings)
                    .recommendations(List.of(
                            "Try more specific queries for better insights",
                            "Use filters to narrow down results"
                    ))
                    .trends(trends)
                    .build();
        }
    }
    
    /**
     * Exception-specific insight strategy.
     */
    private class ExceptionInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            // Find exception counts and types
            Map<String, Integer> exceptionTypes = new HashMap<>();
            int totalExceptions = 0;
            
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < result.getColumns().size(); i++) {
                    String column = result.getColumns().get(i).toLowerCase();
                    if (column.contains("exception_type") && row.get(i) != null) {
                        String type = row.get(i).toString();
                        exceptionTypes.merge(type, 1, Integer::sum);
                    } else if (column.contains("count") && row.get(i) instanceof Number) {
                        totalExceptions += ((Number) row.get(i)).intValue();
                    }
                }
            }
            
            if (!exceptionTypes.isEmpty()) {
                String mostCommon = exceptionTypes.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                findings.add(String.format("Most common exception type: %s", mostCommon));
                trends.put("dominant_exception_type", mostCommon);
            }
            
            if (totalExceptions > 0) {
                findings.add(String.format("Total exceptions found: %d", totalExceptions));
                trends.put("total_exceptions", totalExceptions);
                
                if (totalExceptions > 100) {
                    recommendations.add("High exception count detected. Review processes and training.");
                }
            }
            
            // Analyze resolution status if available
            long resolvedCount = 0;
            long openCount = 0;
            
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < result.getColumns().size(); i++) {
                    if (result.getColumns().get(i).toLowerCase().contains("status")) {
                        String status = row.get(i) != null ? row.get(i).toString().toUpperCase() : "";
                        if (status.contains("RESOLVED")) resolvedCount++;
                        else if (status.contains("OPEN")) openCount++;
                    }
                }
            }
            
            if (resolvedCount > 0 || openCount > 0) {
                double resolutionRate = (resolvedCount * 100.0) / (resolvedCount + openCount);
                findings.add(String.format("Resolution rate: %.1f%%", resolutionRate));
                trends.put("resolution_rate", resolutionRate);
                
                if (resolutionRate < 80) {
                    recommendations.add("Resolution rate below 80%. Focus on clearing open exceptions.");
                }
            }
            
            recommendations.add("Monitor exception trends regularly");
            recommendations.add("Implement preventive measures for common exceptions");
            
            return Insights.builder()
                    .summary(String.format("Exception analysis: %d records analyzed", result.getRows().size()))
                    .keyFindings(findings)
                    .recommendations(recommendations)
                    .trends(trends)
                    .build();
        }
    }
    
    /**
     * Colleague-specific insight strategy.
     */
    private class ColleagueInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            // Count total colleagues
            Set<String> uniqueColleagues = new HashSet<>();
            Map<String, Integer> locationDistribution = new HashMap<>();
            
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < result.getColumns().size(); i++) {
                    String column = result.getColumns().get(i).toLowerCase();
                    
                    if (column.contains("colleague") && column.contains("uuid") && row.get(i) != null) {
                        uniqueColleagues.add(row.get(i).toString());
                    } else if (column.contains("location") && !column.contains("uuid") && row.get(i) != null) {
                        String location = row.get(i).toString();
                        locationDistribution.merge(location, 1, Integer::sum);
                    }
                }
            }
            
            if (!uniqueColleagues.isEmpty()) {
                findings.add(String.format("Total unique colleagues: %d", uniqueColleagues.size()));
                trends.put("total_colleagues", uniqueColleagues.size());
            }
            
            if (!locationDistribution.isEmpty()) {
                String topLocation = locationDistribution.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                findings.add(String.format("Location with most colleagues: %s", topLocation));
                trends.put("top_location", topLocation);
                
                // Check for understaffed locations
                long understaffed = locationDistribution.values().stream()
                        .filter(count -> count < 5)
                        .count();
                if (understaffed > 0) {
                    findings.add(String.format("%d locations may be understaffed", understaffed));
                    recommendations.add("Review staffing levels in smaller locations");
                }
            }
            
            recommendations.add("Ensure balanced colleague distribution across locations");
            recommendations.add("Monitor colleague activity and engagement");
            
            return Insights.builder()
                    .summary(String.format("Colleague analysis across %d records", result.getRows().size()))
                    .keyFindings(findings)
                    .recommendations(recommendations)
                    .trends(trends)
                    .build();
        }
    }
    
    /**
     * Tenant-specific insight strategy.
     */
    private class TenantInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            int activeTenants = 0;
            int inactiveTenants = 0;
            Map<String, Integer> tenantSizes = new HashMap<>();
            
            for (List<Object> row : result.getRows()) {
                String tenantName = null;
                int colleagueCount = 0;
                
                for (int i = 0; i < result.getColumns().size(); i++) {
                    String column = result.getColumns().get(i).toLowerCase();
                    
                    if (column.contains("is_active") && row.get(i) != null) {
                        boolean isActive = Boolean.parseBoolean(row.get(i).toString());
                        if (isActive) activeTenants++;
                        else inactiveTenants++;
                    } else if (column.contains("tenant_name") && row.get(i) != null) {
                        tenantName = row.get(i).toString();
                    } else if (column.contains("colleague_count") && row.get(i) instanceof Number) {
                        colleagueCount = ((Number) row.get(i)).intValue();
                    }
                }
                
                if (tenantName != null && colleagueCount > 0) {
                    tenantSizes.put(tenantName, colleagueCount);
                }
            }
            
            findings.add(String.format("Active tenants: %d", activeTenants));
            findings.add(String.format("Inactive tenants: %d", inactiveTenants));
            trends.put("active_tenants", activeTenants);
            trends.put("inactive_tenants", inactiveTenants);
            
            if (!tenantSizes.isEmpty()) {
                String largestTenant = tenantSizes.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                findings.add(String.format("Largest tenant: %s", largestTenant));
                trends.put("largest_tenant", largestTenant);
            }
            
            if (inactiveTenants > 0) {
                recommendations.add("Review and clean up inactive tenants");
            }
            
            recommendations.add("Monitor tenant growth and activity");
            recommendations.add("Ensure proper onboarding for new tenants");
            
            return Insights.builder()
                    .summary(String.format("Tenant overview: %d total tenants analyzed", 
                            activeTenants + inactiveTenants))
                    .keyFindings(findings)
                    .recommendations(recommendations)
                    .trends(trends)
                    .build();
        }
    }
    
    /**
     * Shift-specific insight strategy.
     */
    private class ShiftInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            Map<Integer, Integer> shiftHours = new HashMap<>();
            int totalShifts = 0;
            
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < result.getColumns().size(); i++) {
                    String column = result.getColumns().get(i).toLowerCase();
                    
                    if (column.contains("shift_hour") && row.get(i) instanceof Number) {
                        int hour = ((Number) row.get(i)).intValue();
                        shiftHours.merge(hour, 1, Integer::sum);
                    } else if (column.contains("shift_count") && row.get(i) instanceof Number) {
                        totalShifts += ((Number) row.get(i)).intValue();
                    }
                }
            }
            
            if (!shiftHours.isEmpty()) {
                Integer peakHour = shiftHours.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(0);
                findings.add(String.format("Peak shift hour: %d:00", peakHour));
                trends.put("peak_shift_hour", peakHour);
            }
            
            findings.add(String.format("Total shifts analyzed: %d", totalShifts));
            trends.put("total_shifts", totalShifts);
            
            recommendations.add("Ensure adequate coverage during peak hours");
            recommendations.add("Review shift patterns for optimization opportunities");
            
            return Insights.builder()
                    .summary(String.format("Shift pattern analysis: %d shift records", result.getRows().size()))
                    .keyFindings(findings)
                    .recommendations(recommendations)
                    .trends(trends)
                    .build();
        }
    }
    
    /**
     * Location-specific insight strategy.
     */
    private class LocationInsightStrategy implements InsightStrategy {
        @Override
        public Insights generateInsights(String query, QueryResult result) {
            List<String> findings = new ArrayList<>();
            List<String> recommendations = new ArrayList<>();
            Map<String, Object> trends = new HashMap<>();
            
            Set<String> locations = new HashSet<>();
            Map<String, Integer> locationActivity = new HashMap<>();
            
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < result.getColumns().size(); i++) {
                    String column = result.getColumns().get(i).toLowerCase();
                    
                    if (column.contains("location_name") && row.get(i) != null) {
                        String location = row.get(i).toString();
                        locations.add(location);
                        locationActivity.merge(location, 1, Integer::sum);
                    }
                }
            }
            
            findings.add(String.format("Total locations: %d", locations.size()));
            trends.put("total_locations", locations.size());
            
            if (!locationActivity.isEmpty()) {
                String busiestLocation = locationActivity.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                findings.add(String.format("Most active location: %s", busiestLocation));
                trends.put("busiest_location", busiestLocation);
            }
            
            recommendations.add("Monitor location-specific metrics");
            recommendations.add("Ensure consistent service across all locations");
            
            return Insights.builder()
                    .summary(String.format("Location analysis: %d locations found", locations.size()))
                    .keyFindings(findings)
                    .recommendations(recommendations)
                    .trends(trends)
                    .build();
        }
    }
    
    // Helper methods
    
    private List<String> findNumericColumns(QueryResult result) {
        List<String> numericColumns = new ArrayList<>();
        
        if (result.getRows().isEmpty()) return numericColumns;
        
        for (int i = 0; i < result.getColumns().size(); i++) {
            boolean isNumeric = true;
            for (List<Object> row : result.getRows()) {
                if (row.size() > i && row.get(i) != null && !(row.get(i) instanceof Number)) {
                    try {
                        Double.parseDouble(row.get(i).toString());
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            if (isNumeric) {
                numericColumns.add(result.getColumns().get(i));
            }
        }
        
        return numericColumns;
    }
    
    private List<Double> extractNumericValues(QueryResult result, String columnName) {
        List<Double> values = new ArrayList<>();
        int columnIndex = result.getColumns().indexOf(columnName);
        
        if (columnIndex >= 0) {
            for (List<Object> row : result.getRows()) {
                if (row.size() > columnIndex && row.get(columnIndex) != null) {
                    try {
                        if (row.get(columnIndex) instanceof Number) {
                            values.add(((Number) row.get(columnIndex)).doubleValue());
                        } else {
                            values.add(Double.parseDouble(row.get(columnIndex).toString()));
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            }
        }
        
        return values;
    }
    
    private double calculateAverage(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double mean = calculateAverage(values);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Enhances basic insights with predictive analytics.
     * 
     * @param insights Basic insights
     * @param predictions Predictive analytics data
     * @return Enhanced insights
     */
    private Insights enhanceWithPredictions(Insights insights, Map<String, Object> predictions) {
        if (predictions.isEmpty()) {
            return insights;
        }
        
        List<String> keyFindings = new ArrayList<>(insights.getKeyFindings());
        List<String> recommendations = new ArrayList<>(insights.getRecommendations());
        
        // Add prediction-based findings
        if (predictions.containsKey("trend")) {
            String trend = predictions.get("trend").toString();
            Object trendPercentage = predictions.get("trendPercentage");
            keyFindings.add(String.format("Trend Analysis: Data is %s by %.1f%%", 
                trend, Double.parseDouble(trendPercentage.toString())));
        }
        
        if (predictions.containsKey("riskLevel")) {
            String riskLevel = predictions.get("riskLevel").toString();
            keyFindings.add(String.format("Risk Assessment: %s risk level detected", riskLevel));
        }
        
        if (predictions.containsKey("predictedNextPeriod")) {
            Object predicted = predictions.get("predictedNextPeriod");
            keyFindings.add(String.format("Prediction: Next period expected value: %.2f", 
                Double.parseDouble(predicted.toString())));
        }
        
        // Add prediction-based recommendations
        if (predictions.containsKey("recommendations")) {
            @SuppressWarnings("unchecked")
            List<String> predictionRecs = (List<String>) predictions.get("recommendations");
            recommendations.addAll(predictionRecs);
        }
        
        if (predictions.containsKey("atRiskCount")) {
            int atRiskCount = Integer.parseInt(predictions.get("atRiskCount").toString());
            if (atRiskCount > 0) {
                recommendations.add(String.format("Action Required: %d items identified as at-risk", atRiskCount));
            }
        }
        
        // Add predictive data to additional data
        Map<String, Object> additionalData = new HashMap<>();
        if (insights.getAdditionalData() != null) {
            additionalData.putAll(insights.getAdditionalData());
        }
        additionalData.put("predictiveAnalytics", predictions);
        
        return Insights.builder()
                .summary(insights.getSummary() + " [Enhanced with predictive analytics]")
                .keyFindings(keyFindings)
                .recommendations(recommendations)
                .trends(insights.getTrends())
                .additionalData(additionalData)
                .build();
    }
}