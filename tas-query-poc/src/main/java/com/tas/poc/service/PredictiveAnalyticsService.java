package com.tas.poc.service;

import com.tas.poc.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating predictive analytics from query results.
 * 
 * This service analyzes historical data patterns to provide:
 * - Trend predictions
 * - Anomaly detection
 * - Risk assessments
 * - Recommendations based on patterns
 * 
 * Current implementation uses statistical analysis and pattern recognition.
 * Future enhancements could integrate ML models for more sophisticated predictions.
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictiveAnalyticsService {
    
    /**
     * Generates predictive insights based on query results and data patterns.
     * 
     * @param queryType The type of query being analyzed
     * @param result The query results to analyze
     * @return Map containing predictive insights
     */
    public Map<String, Object> generatePredictiveInsights(String queryType, QueryResult result) {
        log.debug("Generating predictive insights for query type: {}", queryType);
        
        Map<String, Object> predictions = new HashMap<>();
        
        String lowerQuery = queryType.toLowerCase();
        
        if (lowerQuery.contains("exception")) {
            predictions = generateExceptionPredictions(result);
        } else if (lowerQuery.contains("shift") || lowerQuery.contains("attendance")) {
            predictions = generateAttendancePredictions(result);
        } else if (lowerQuery.contains("colleague") || lowerQuery.contains("employee")) {
            predictions = generateColleaguePredictions(result);
        } else if (lowerQuery.contains("trend") || lowerQuery.contains("pattern")) {
            predictions = generateTrendPredictions(result);
        }
        
        // Add general predictions if we have time-series data
        if (hasTimeSeriesData(result)) {
            predictions.putAll(generateTimeSeriesPredictions(result));
        }
        
        return predictions;
    }
    
    /**
     * Generates predictions for exception-related queries.
     * Analyzes patterns to predict future exceptions and risk areas.
     */
    private Map<String, Object> generateExceptionPredictions(QueryResult result) {
        Map<String, Object> predictions = new HashMap<>();
        
        // Analyze exception trends
        if (result.getRows().size() > 5) {
            List<Double> exceptionCounts = extractNumericColumn(result, "exception_count", "count", "total_exceptions");
            
            if (!exceptionCounts.isEmpty()) {
                double avgExceptions = calculateAverage(exceptionCounts);
                double stdDev = calculateStandardDeviation(exceptionCounts, avgExceptions);
                double trend = calculateTrend(exceptionCounts);
                
                predictions.put("trend", trend > 0 ? "increasing" : "decreasing");
                predictions.put("trendPercentage", Math.abs(trend * 100));
                predictions.put("predictedNextPeriod", avgExceptions + trend);
                predictions.put("riskLevel", determineRiskLevel(avgExceptions, stdDev, trend));
                
                // Identify high-risk periods
                List<String> highRiskPeriods = identifyHighRiskPeriods(result, avgExceptions + stdDev);
                predictions.put("highRiskPeriods", highRiskPeriods);
                
                // Generate recommendations
                List<String> recommendations = generateExceptionRecommendations(avgExceptions, trend, highRiskPeriods);
                predictions.put("recommendations", recommendations);
            }
        }
        
        return predictions;
    }
    
    /**
     * Generates predictions for attendance and shift patterns.
     */
    private Map<String, Object> generateAttendancePredictions(QueryResult result) {
        Map<String, Object> predictions = new HashMap<>();
        
        // Analyze shift patterns
        List<Double> shiftCounts = extractNumericColumn(result, "shift_count", "total_shifts", "count");
        
        if (!shiftCounts.isEmpty()) {
            double avgShifts = calculateAverage(shiftCounts);
            double peakLoad = Collections.max(shiftCounts);
            double minLoad = Collections.min(shiftCounts);
            
            predictions.put("averageShifts", avgShifts);
            predictions.put("peakLoad", peakLoad);
            predictions.put("minimumLoad", minLoad);
            predictions.put("loadVariance", (peakLoad - minLoad) / avgShifts * 100);
            
            // Predict staffing needs
            double predictedDemand = avgShifts * 1.1; // 10% buffer
            predictions.put("recommendedStaffing", Math.ceil(predictedDemand));
            
            // Identify patterns
            Map<String, Integer> patterns = identifyShiftPatterns(result);
            predictions.put("commonPatterns", patterns);
            
            // Coverage recommendations
            List<String> coverageRecommendations = generateCoverageRecommendations(avgShifts, peakLoad, patterns);
            predictions.put("coverageRecommendations", coverageRecommendations);
        }
        
        return predictions;
    }
    
    /**
     * Generates predictions for colleague/employee performance.
     */
    private Map<String, Object> generateColleaguePredictions(QueryResult result) {
        Map<String, Object> predictions = new HashMap<>();
        
        // Analyze colleague metrics
        List<Double> exceptionCounts = extractNumericColumn(result, "total_exceptions", "exception_count");
        List<Double> shiftCounts = extractNumericColumn(result, "total_shifts", "shift_count");
        
        if (!exceptionCounts.isEmpty() && !shiftCounts.isEmpty()) {
            // Calculate exception rate per shift
            List<Double> exceptionRates = new ArrayList<>();
            for (int i = 0; i < Math.min(exceptionCounts.size(), shiftCounts.size()); i++) {
                if (shiftCounts.get(i) > 0) {
                    exceptionRates.add(exceptionCounts.get(i) / shiftCounts.get(i));
                }
            }
            
            if (!exceptionRates.isEmpty()) {
                double avgRate = calculateAverage(exceptionRates);
                double threshold = avgRate * 1.5; // 50% above average
                
                predictions.put("averageExceptionRate", avgRate);
                predictions.put("riskThreshold", threshold);
                
                // Identify at-risk colleagues
                List<Map<String, Object>> atRiskColleagues = identifyAtRiskColleagues(result, threshold);
                predictions.put("atRiskColleagues", atRiskColleagues);
                predictions.put("atRiskCount", atRiskColleagues.size());
                
                // Performance recommendations
                List<String> recommendations = generatePerformanceRecommendations(atRiskColleagues, avgRate);
                predictions.put("performanceRecommendations", recommendations);
            }
        }
        
        return predictions;
    }
    
    /**
     * Generates general trend predictions.
     */
    private Map<String, Object> generateTrendPredictions(QueryResult result) {
        Map<String, Object> predictions = new HashMap<>();
        
        // Find numeric columns for trend analysis
        List<String> numericColumns = findNumericColumns(result);
        
        for (String column : numericColumns) {
            List<Double> values = extractNumericColumn(result, column);
            if (values.size() >= 3) {
                double trend = calculateTrend(values);
                String trendDirection = trend > 0.05 ? "increasing" : (trend < -0.05 ? "decreasing" : "stable");
                
                predictions.put(column + "_trend", trendDirection);
                predictions.put(column + "_change", Math.abs(trend * 100));
                
                // Simple linear projection
                double lastValue = values.get(values.size() - 1);
                double predictedNext = lastValue + (lastValue * trend);
                predictions.put(column + "_nextPredicted", predictedNext);
            }
        }
        
        return predictions;
    }
    
    /**
     * Generates predictions for time-series data.
     */
    private Map<String, Object> generateTimeSeriesPredictions(QueryResult result) {
        Map<String, Object> predictions = new HashMap<>();
        
        // Look for date/time columns
        List<String> dateColumns = findDateColumns(result);
        
        if (!dateColumns.isEmpty()) {
            // Analyze seasonality
            Map<String, Object> seasonality = analyzeSeasonality(result, dateColumns.get(0));
            predictions.put("seasonality", seasonality);
            
            // Predict next period
            LocalDate nextPeriod = LocalDate.now().plusDays(7);
            predictions.put("predictionDate", nextPeriod.format(DateTimeFormatter.ISO_DATE));
            predictions.put("confidenceLevel", "moderate"); // Simple confidence assessment
        }
        
        return predictions;
    }
    
    // Helper methods
    
    private List<Double> extractNumericColumn(QueryResult result, String... columnNames) {
        List<Double> values = new ArrayList<>();
        
        for (String columnName : columnNames) {
            int columnIndex = -1;
            for (int i = 0; i < result.getColumns().size(); i++) {
                if (result.getColumns().get(i).toLowerCase().contains(columnName.toLowerCase())) {
                    columnIndex = i;
                    break;
                }
            }
            
            if (columnIndex >= 0) {
                for (List<Object> row : result.getRows()) {
                    try {
                        Object value = row.get(columnIndex);
                        if (value instanceof Number) {
                            values.add(((Number) value).doubleValue());
                        } else if (value instanceof String) {
                            values.add(Double.parseDouble(value.toString()));
                        }
                    } catch (Exception e) {
                        // Skip non-numeric values
                    }
                }
                
                if (!values.isEmpty()) {
                    break; // Found values, no need to check other column names
                }
            }
        }
        
        return values;
    }
    
    private double calculateAverage(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private double calculateStandardDeviation(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double calculateTrend(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        // Simple linear regression
        double n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < values.size(); i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double avgY = sumY / n;
        
        return avgY > 0 ? slope / avgY : 0; // Return relative trend
    }
    
    private String determineRiskLevel(double avg, double stdDev, double trend) {
        double riskScore = 0;
        
        // High average indicates higher base risk
        if (avg > 10) riskScore += 2;
        else if (avg > 5) riskScore += 1;
        
        // High variance indicates unpredictability
        if (stdDev / avg > 0.5) riskScore += 2;
        else if (stdDev / avg > 0.3) riskScore += 1;
        
        // Increasing trend indicates growing risk
        if (trend > 0.1) riskScore += 2;
        else if (trend > 0.05) riskScore += 1;
        
        if (riskScore >= 4) return "HIGH";
        else if (riskScore >= 2) return "MEDIUM";
        else return "LOW";
    }
    
    private List<String> identifyHighRiskPeriods(QueryResult result, double threshold) {
        List<String> highRiskPeriods = new ArrayList<>();
        
        // Look for date/time columns and exception counts
        int dateIndex = -1;
        int countIndex = -1;
        
        for (int i = 0; i < result.getColumns().size(); i++) {
            String column = result.getColumns().get(i).toLowerCase();
            if (column.contains("date") || column.contains("time")) {
                dateIndex = i;
            } else if (column.contains("count") || column.contains("exception")) {
                countIndex = i;
            }
        }
        
        if (dateIndex >= 0 && countIndex >= 0) {
            for (List<Object> row : result.getRows()) {
                try {
                    double count = Double.parseDouble(row.get(countIndex).toString());
                    if (count > threshold) {
                        highRiskPeriods.add(row.get(dateIndex).toString());
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                }
            }
        }
        
        return highRiskPeriods;
    }
    
    private List<String> generateExceptionRecommendations(double avg, double trend, List<String> highRiskPeriods) {
        List<String> recommendations = new ArrayList<>();
        
        if (trend > 0.1) {
            recommendations.add("Exception rate is increasing significantly. Consider implementing preventive measures.");
        }
        
        if (avg > 10) {
            recommendations.add("High average exception rate detected. Review and optimize current processes.");
        }
        
        if (!highRiskPeriods.isEmpty()) {
            recommendations.add(String.format("Focus on high-risk periods: %s", 
                highRiskPeriods.stream().limit(3).collect(Collectors.joining(", "))));
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Exception rates are within normal range. Continue monitoring.");
        }
        
        return recommendations;
    }
    
    private Map<String, Integer> identifyShiftPatterns(QueryResult result) {
        Map<String, Integer> patterns = new HashMap<>();
        
        // Look for shift hour or time patterns
        for (int i = 0; i < result.getColumns().size(); i++) {
            if (result.getColumns().get(i).toLowerCase().contains("hour")) {
                for (List<Object> row : result.getRows()) {
                    try {
                        String hour = row.get(i).toString();
                        patterns.merge(hour + ":00", 1, Integer::sum);
                    } catch (Exception e) {
                        // Skip invalid rows
                    }
                }
                break;
            }
        }
        
        return patterns;
    }
    
    private List<String> generateCoverageRecommendations(double avg, double peak, Map<String, Integer> patterns) {
        List<String> recommendations = new ArrayList<>();
        
        double coverageGap = peak - avg;
        if (coverageGap > avg * 0.5) {
            recommendations.add(String.format("Large variance in shift coverage (%.1f%%). Consider load balancing.", 
                coverageGap / avg * 100));
        }
        
        // Find peak hours
        if (!patterns.isEmpty()) {
            String peakHour = patterns.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
            recommendations.add(String.format("Peak shift time is %s. Ensure adequate staffing.", peakHour));
        }
        
        recommendations.add(String.format("Maintain minimum staffing of %.0f per shift based on historical data.", 
            Math.ceil(avg * 1.1)));
        
        return recommendations;
    }
    
    private List<Map<String, Object>> identifyAtRiskColleagues(QueryResult result, double threshold) {
        List<Map<String, Object>> atRisk = new ArrayList<>();
        
        // Find relevant columns
        int idIndex = -1;
        int exceptionIndex = -1;
        int shiftIndex = -1;
        
        for (int i = 0; i < result.getColumns().size(); i++) {
            String column = result.getColumns().get(i).toLowerCase();
            if (column.contains("colleague") || column.contains("uuid")) {
                idIndex = i;
            } else if (column.contains("exception")) {
                exceptionIndex = i;
            } else if (column.contains("shift")) {
                shiftIndex = i;
            }
        }
        
        if (idIndex >= 0 && exceptionIndex >= 0 && shiftIndex >= 0) {
            for (List<Object> row : result.getRows()) {
                try {
                    double exceptions = Double.parseDouble(row.get(exceptionIndex).toString());
                    double shifts = Double.parseDouble(row.get(shiftIndex).toString());
                    
                    if (shifts > 0) {
                        double rate = exceptions / shifts;
                        if (rate > threshold) {
                            Map<String, Object> colleague = new HashMap<>();
                            colleague.put("id", row.get(idIndex));
                            colleague.put("exceptionRate", rate);
                            colleague.put("totalExceptions", exceptions);
                            atRisk.add(colleague);
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                }
            }
        }
        
        return atRisk;
    }
    
    private List<String> generatePerformanceRecommendations(List<Map<String, Object>> atRiskColleagues, double avgRate) {
        List<String> recommendations = new ArrayList<>();
        
        if (!atRiskColleagues.isEmpty()) {
            recommendations.add(String.format("%d colleagues identified with above-average exception rates.", 
                atRiskColleagues.size()));
            recommendations.add("Consider additional training or support for at-risk colleagues.");
        }
        
        if (avgRate > 0.2) {
            recommendations.add("Overall exception rate is high. Review shift scheduling and workload distribution.");
        }
        
        recommendations.add("Implement regular performance reviews to identify issues early.");
        
        return recommendations;
    }
    
    private boolean hasTimeSeriesData(QueryResult result) {
        return result.getColumns().stream()
            .anyMatch(col -> col.toLowerCase().contains("date") || 
                           col.toLowerCase().contains("time") ||
                           col.toLowerCase().contains("period"));
    }
    
    private List<String> findNumericColumns(QueryResult result) {
        List<String> numericColumns = new ArrayList<>();
        
        for (int i = 0; i < result.getColumns().size(); i++) {
            boolean isNumeric = true;
            for (List<Object> row : result.getRows()) {
                if (row.size() > i && row.get(i) != null) {
                    try {
                        Double.parseDouble(row.get(i).toString());
                    } catch (Exception e) {
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
    
    private List<String> findDateColumns(QueryResult result) {
        return result.getColumns().stream()
            .filter(col -> col.toLowerCase().contains("date") || 
                         col.toLowerCase().contains("time"))
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> analyzeSeasonality(QueryResult result, String dateColumn) {
        Map<String, Object> seasonality = new HashMap<>();
        
        // Simple day-of-week analysis
        Map<String, Integer> dayOfWeekCounts = new HashMap<>();
        
        int dateIndex = result.getColumns().indexOf(dateColumn);
        if (dateIndex >= 0) {
            for (List<Object> row : result.getRows()) {
                try {
                    String dateStr = row.get(dateIndex).toString();
                    // This is simplified - in production, parse the date properly
                    dayOfWeekCounts.merge(dateStr.substring(0, 3), 1, Integer::sum);
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        }
        
        seasonality.put("pattern", "weekly");
        seasonality.put("distribution", dayOfWeekCounts);
        
        return seasonality;
    }
}