package com.tas.poc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insights {
    private String summary;
    private List<Finding> keyFindings;
    private List<String> followUpSuggestions;
    private DataHighlights highlights;
    private String explanation;
    
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