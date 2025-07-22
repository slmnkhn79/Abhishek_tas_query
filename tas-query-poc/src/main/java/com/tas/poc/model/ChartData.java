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
public class ChartData {
    private String chartType; // bar, line, pie, donut
    private String title;
    private List<String> labels;
    private List<Dataset> datasets;
    private Map<String, Object> options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dataset {
        private String label;
        private List<Number> data;
        private String backgroundColor;
        private String borderColor;
        private Integer borderWidth;
    }
}