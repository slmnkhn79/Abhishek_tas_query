package com.tas.poc.service;

import com.tas.poc.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatResponseBuilder {
    
    private final InsightGenerationService insightGenerationService;
    
    public ChatMessage buildInsightfulResponse(String sessionId, String userQuery, String sql, 
                                             QueryResult queryResult, ChartData chartData) {
        
        // Generate insights
        Insights insights = insightGenerationService.generateInsights(userQuery, queryResult, chartData);
        
        // Build conversational response
        String botResponse = buildConversationalResponse(queryResult, insights, chartData);
        
        // Create result summary
        String resultSummary = createResultSummary(queryResult, insights);
        
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userMessage(userQuery)
                .botResponse(botResponse)
                .sqlQuery(sql)
                .timestamp(LocalDateTime.now())
                .type(queryResult.isSuccess() ? ChatMessage.MessageType.INSIGHT : ChatMessage.MessageType.ERROR)
                .insights(insights)
                .followUpQuestions(generateFollowUpQuestions(insights, userQuery))
                .resultSummary(resultSummary)
                .resultCount(queryResult.getRowCount())
                .build();
    }
    
    private String buildConversationalResponse(QueryResult result, Insights insights, ChartData chartData) {
        StringBuilder response = new StringBuilder();
        
        if (!result.isSuccess()) {
            return "I encountered an error while processing your query: " + result.getErrorMessage();
        }
        
        if (result.getRowCount() == 0) {
            return "I didn't find any data matching your query. " +
                   "Try one of these suggestions: " + String.join(", ", insights.getFollowUpSuggestions());
        }
        
        // Start with the summary
        response.append(insights.getSummary()).append("\n\n");
        
        // Add key findings
        if (!insights.getKeyFindings().isEmpty()) {
            response.append("📊 Key Findings:\n");
            for (Finding finding : insights.getKeyFindings()) {
                response.append("• ").append(finding.getMessage());
                if (finding.getValue() != null) {
                    response.append(" (").append(finding.getValue()).append(")");
                }
                response.append("\n");
            }
            response.append("\n");
        }
        
        // Add chart indication
        if (chartData != null) {
            response.append("📈 I've prepared a ").append(chartData.getChartType())
                   .append(" chart to visualize this data.\n\n");
        }
        
        // Add follow-up suggestions
        if (!insights.getFollowUpSuggestions().isEmpty()) {
            response.append("💡 You might also want to:\n");
            for (String suggestion : insights.getFollowUpSuggestions()) {
                response.append("• ").append(suggestion).append("\n");
            }
        }
        
        return response.toString();
    }
    
    private String createResultSummary(QueryResult result, Insights insights) {
        if (!result.isSuccess()) {
            return "Query failed: " + result.getErrorMessage();
        }
        
        if (result.getRowCount() == 0) {
            return "No data found";
        }
        
        // Use insights summary if available
        if (insights.getSummary() != null && !insights.getSummary().isEmpty()) {
            return insights.getSummary();
        }
        
        // Default summary
        return String.format("Found %d results in %dms", 
                result.getRowCount(), result.getExecutionTimeMs());
    }
    
    private List<String> generateFollowUpQuestions(Insights insights, String originalQuery) {
        List<String> questions = new ArrayList<>();
        
        // Add insight-based suggestions
        if (insights.getFollowUpSuggestions() != null) {
            questions.addAll(insights.getFollowUpSuggestions());
        }
        
        // Add contextual questions based on query type
        String lowerQuery = originalQuery.toLowerCase();
        
        if (lowerQuery.contains("tenant")) {
            questions.add("Show me more details about a specific tenant");
            questions.add("Which tenants have the most exceptions?");
        } else if (lowerQuery.contains("exception")) {
            questions.add("What's the average resolution time?");
            questions.add("Show me exception trends by week");
        } else if (lowerQuery.contains("colleague")) {
            questions.add("Which colleagues have the most shifts?");
            questions.add("Show colleague distribution by shift time");
        }
        
        // Limit to 5 suggestions
        return questions.stream().limit(5).toList();
    }
}