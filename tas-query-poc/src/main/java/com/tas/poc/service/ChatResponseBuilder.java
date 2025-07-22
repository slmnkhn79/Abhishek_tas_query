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
        Insights insights = insightGenerationService.generateInsights(userQuery, queryResult);
        
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
                   "Try one of these suggestions: " + String.join(", ", 
                       insights.getRecommendations() != null ? insights.getRecommendations() : List.of());
        }
        
        // Start with the summary
        response.append(insights.getSummary()).append("\n\n");
        
        // Add key findings
        if (insights.getKeyFindings() != null && !insights.getKeyFindings().isEmpty()) {
            response.append("📊 Key Findings:\n");
            for (String finding : insights.getKeyFindings()) {
                response.append("• ").append(finding).append("\n");
            }
            response.append("\n");
        }
        
        // Add chart indication
        if (chartData != null) {
            response.append("📈 I've prepared a ").append(chartData.getChartType())
                   .append(" chart to visualize this data.\n\n");
        }
        
        // Add recommendations
        if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
            response.append("💡 Recommendations:\n");
            for (String recommendation : insights.getRecommendations()) {
                response.append("• ").append(recommendation).append("\n");
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
        
        // Add insight-based recommendations as follow-up questions
        if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
            // Convert recommendations to questions format
            for (String rec : insights.getRecommendations()) {
                if (rec.toLowerCase().contains("monitor") || rec.toLowerCase().contains("review")) {
                    // Skip process recommendations
                    continue;
                }
                questions.add(rec);
            }
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