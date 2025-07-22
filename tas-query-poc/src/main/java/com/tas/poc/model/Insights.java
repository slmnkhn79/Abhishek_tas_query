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
public class Insights {
    private String summary;
    private List<String> keyFindings;
    private List<String> recommendations;
    private Map<String, Object> trends;
    private Map<String, Object> additionalData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataHighlights {
        private String topValue;
        private String bottomValue;
        private Double average;
        private Long total;
        private Double percentageChange;
        private String trend; // increasing, decreasing, stable
    }
}