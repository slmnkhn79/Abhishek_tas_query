package com.tas.poc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finding {
    private String type; // TREND, ANOMALY, HIGHLIGHT, WARNING
    private String message;
    private String value;
    private Double significance; // 0-1 scale
}