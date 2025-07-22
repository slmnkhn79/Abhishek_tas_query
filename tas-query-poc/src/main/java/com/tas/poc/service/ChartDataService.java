package com.tas.poc.service;

import com.tas.poc.model.ChartData;
import com.tas.poc.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChartDataService {
    
    private static final Map<String, ChartConfig> CHART_CONFIGS = new HashMap<>();
    
    static {
        // Configuration for known query patterns
        CHART_CONFIGS.put("colleagues by location", new ChartConfig("bar", "colleague_count", "location_name"));
        CHART_CONFIGS.put("exceptions by type", new ChartConfig("donut", "exception_count", "exception_type"));
        CHART_CONFIGS.put("daily exceptions", new ChartConfig("line", Arrays.asList("total_exceptions", "resolved_count", "open_count"), "exception_date"));
        CHART_CONFIGS.put("tenant overview", new ChartConfig("bar", Arrays.asList("location_count", "colleague_count", "shift_count"), "tenant_name"));
        CHART_CONFIGS.put("shift patterns", new ChartConfig("line", "shift_count", "shift_hour"));
        CHART_CONFIGS.put("exception status distribution", new ChartConfig("stacked-bar", "count", Arrays.asList("tenant_name", "status")));
    }
    
    public ChartData generateChartData(String query, QueryResult queryResult) {
        if (!queryResult.isSuccess() || queryResult.getResults().isEmpty()) {
            return null;
        }
        
        // Find matching chart configuration
        String matchedPattern = findMatchingPattern(query);
        if (matchedPattern == null || !CHART_CONFIGS.containsKey(matchedPattern)) {
            return autoDetectChartData(queryResult);
        }
        
        ChartConfig config = CHART_CONFIGS.get(matchedPattern);
        return buildChartData(matchedPattern, config, queryResult);
    }
    
    private String findMatchingPattern(String query) {
        String lowerQuery = query.toLowerCase();
        for (String pattern : CHART_CONFIGS.keySet()) {
            if (lowerQuery.contains(pattern)) {
                return pattern;
            }
        }
        return null;
    }
    
    private ChartData buildChartData(String title, ChartConfig config, QueryResult queryResult) {
        List<Map<String, Object>> results = queryResult.getResults();
        
        ChartData.ChartDataBuilder chartBuilder = ChartData.builder()
                .chartType(config.chartType)
                .title(formatTitle(title));
        
        if (config.chartType.equals("stacked-bar") && config.groupByFields != null && config.groupByFields.size() == 2) {
            return buildStackedBarChart(title, config, results);
        }
        
        // Extract labels
        List<String> labels = results.stream()
                .map(row -> String.valueOf(row.get(config.labelField)))
                .collect(Collectors.toList());
        chartBuilder.labels(labels);
        
        // Build datasets
        List<ChartData.Dataset> datasets = new ArrayList<>();
        if (config.valueFields != null) {
            // Multiple datasets
            for (String valueField : config.valueFields) {
                datasets.add(buildDataset(valueField, results, valueField));
            }
        } else if (config.valueField != null) {
            // Single dataset
            datasets.add(buildDataset(title, results, config.valueField));
        }
        
        chartBuilder.datasets(datasets);
        
        // Add chart options
        Map<String, Object> options = new HashMap<>();
        options.put("responsive", true);
        options.put("maintainAspectRatio", false);
        chartBuilder.options(options);
        
        return chartBuilder.build();
    }
    
    private ChartData buildStackedBarChart(String title, ChartConfig config, List<Map<String, Object>> results) {
        // Group data for stacked bar chart
        Map<String, Map<String, Number>> groupedData = new LinkedHashMap<>();
        Set<String> allStatuses = new LinkedHashSet<>();
        
        for (Map<String, Object> row : results) {
            String tenant = String.valueOf(row.get(config.groupByFields.get(0)));
            String status = String.valueOf(row.get(config.groupByFields.get(1)));
            Number count = (Number) row.get(config.valueField);
            
            groupedData.computeIfAbsent(tenant, k -> new LinkedHashMap<>()).put(status, count);
            allStatuses.add(status);
        }
        
        List<String> labels = new ArrayList<>(groupedData.keySet());
        List<ChartData.Dataset> datasets = new ArrayList<>();
        
        for (String status : allStatuses) {
            List<Number> data = new ArrayList<>();
            for (String tenant : labels) {
                data.add(groupedData.get(tenant).getOrDefault(status, 0));
            }
            datasets.add(buildDataset(status, data));
        }
        
        return ChartData.builder()
                .chartType("bar")
                .title(formatTitle(title))
                .labels(labels)
                .datasets(datasets)
                .options(Map.of(
                    "responsive", true,
                    "maintainAspectRatio", false,
                    "scales", Map.of(
                        "x", Map.of("stacked", true),
                        "y", Map.of("stacked", true)
                    )
                ))
                .build();
    }
    
    private ChartData.Dataset buildDataset(String label, List<Map<String, Object>> results, String valueField) {
        List<Number> data = results.stream()
                .map(row -> {
                    Object value = row.get(valueField);
                    if (value instanceof Number) {
                        return (Number) value;
                    }
                    try {
                        return Double.parseDouble(String.valueOf(value));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
        
        return buildDataset(label, data);
    }
    
    private ChartData.Dataset buildDataset(String label, List<Number> data) {
        String color = generateColor(label.hashCode());
        return ChartData.Dataset.builder()
                .label(formatLabel(label))
                .data(data)
                .backgroundColor(color + "33") // Add transparency
                .borderColor(color)
                .borderWidth(2)
                .build();
    }
    
    private ChartData autoDetectChartData(QueryResult queryResult) {
        // Simple auto-detection logic
        List<Map<String, Object>> results = queryResult.getResults();
        if (results.isEmpty()) return null;
        
        Map<String, Object> firstRow = results.get(0);
        List<String> numericColumns = new ArrayList<>();
        String potentialLabelColumn = null;
        
        // Find numeric and non-numeric columns
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (entry.getValue() instanceof Number) {
                numericColumns.add(entry.getKey());
            } else if (potentialLabelColumn == null) {
                potentialLabelColumn = entry.getKey();
            }
        }
        
        if (numericColumns.isEmpty() || potentialLabelColumn == null) {
            return null;
        }
        
        // Build simple bar chart
        List<String> labels = results.stream()
                .map(row -> String.valueOf(row.get(potentialLabelColumn)))
                .collect(Collectors.toList());
        
        List<ChartData.Dataset> datasets = new ArrayList<>();
        for (String numericColumn : numericColumns) {
            datasets.add(buildDataset(numericColumn, results, numericColumn));
        }
        
        return ChartData.builder()
                .chartType("bar")
                .title("Query Results")
                .labels(labels)
                .datasets(datasets)
                .options(Map.of("responsive", true, "maintainAspectRatio", false))
                .build();
    }
    
    private String generateColor(int seed) {
        Random rand = new Random(seed);
        int r = rand.nextInt(156) + 100; // 100-255
        int g = rand.nextInt(156) + 100;
        int b = rand.nextInt(156) + 100;
        return String.format("#%02x%02x%02x", r, g, b);
    }
    
    private String formatTitle(String title) {
        return title.substring(0, 1).toUpperCase() + 
               title.substring(1).replace("_", " ");
    }
    
    private String formatLabel(String label) {
        return label.replace("_", " ").substring(0, 1).toUpperCase() + 
               label.replace("_", " ").substring(1);
    }
    
    private static class ChartConfig {
        String chartType;
        String valueField;
        List<String> valueFields;
        String labelField;
        List<String> groupByFields;
        
        ChartConfig(String chartType, String valueField, String labelField) {
            this.chartType = chartType;
            this.valueField = valueField;
            this.labelField = labelField;
        }
        
        ChartConfig(String chartType, List<String> valueFields, String labelField) {
            this.chartType = chartType;
            this.valueFields = valueFields;
            this.labelField = labelField;
        }
        
        ChartConfig(String chartType, String valueField, List<String> groupByFields) {
            this.chartType = chartType;
            this.valueField = valueField;
            this.groupByFields = groupByFields;
        }
    }
}