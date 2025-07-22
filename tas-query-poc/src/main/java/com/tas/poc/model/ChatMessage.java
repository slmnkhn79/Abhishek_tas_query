package com.tas.poc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String sessionId;
    private String userMessage;
    private String botResponse;
    private String sqlQuery;
    private LocalDateTime timestamp;
    private MessageType type;
    
    // Enhanced fields for insights
    private Insights insights;
    private List<String> followUpQuestions;
    private String resultSummary;
    private Integer resultCount;
    
    public enum MessageType {
        USER, BOT, ERROR, INSIGHT
    }
}