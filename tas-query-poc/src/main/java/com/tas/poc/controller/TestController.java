package com.tas.poc.controller;

import com.tas.poc.model.ChatMessage;
import com.tas.poc.model.ChartData;
import com.tas.poc.model.QueryResult;
import com.tas.poc.service.ChartDataService;
import com.tas.poc.service.ConversationService;
import com.tas.poc.service.QueryService;
import com.tas.poc.service.SqlGenerationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final QueryService queryService;
    private final ConversationService conversationService;
    private final SqlGenerationService sqlGenerationService;
    private final ChartDataService chartDataService;
    private final ChatResponseBuilder chatResponseBuilder;
    private final ConversationalContextService contextService;
    
    @GetMapping("/health")
    public String health() {
        boolean dbConnected = queryService.testConnection();
        return "Application is running. Database connected: " + dbConnected;
    }
    
    @GetMapping("/tables")
    public List<String> getTables() {
        return queryService.getTables();
    }
    
    @PostMapping("/query")
    public QueryResponse testQuery(@RequestParam String query, 
                                  @RequestParam(defaultValue = "test-session") String sessionId) {
        // Add user message to conversation
        ChatMessage userMessage = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userMessage(query)
                .timestamp(LocalDateTime.now())
                .type(ChatMessage.MessageType.USER)
                .build();
        conversationService.addMessage(sessionId, userMessage);
        
        // Enhance query with conversational context
        String enhancedQuery = contextService.enhanceQueryWithContext(query, sessionId);
        
        // Generate SQL
        String sql = sqlGenerationService.generateSql(enhancedQuery, sessionId);
        
        // Execute query
        QueryResult result = queryService.executeQuery(sql);
        
        // Generate chart data if applicable
        ChartData chartData = chartDataService.generateChartData(enhancedQuery, result);
        
        // Build insightful chat response
        ChatMessage botMessage = chatResponseBuilder.buildInsightfulResponse(
                sessionId, query, sql, result, chartData);
        
        // Add bot response to conversation
        conversationService.addMessage(sessionId, botMessage);
        
        return new QueryResponse(result, chartData, botMessage);
    }
    
    @GetMapping("/conversation/{sessionId}")
    public List<ChatMessage> getConversation(@PathVariable String sessionId) {
        return conversationService.getConversationHistory(sessionId);
    }
    
    @Data
    @AllArgsConstructor
    public static class QueryResponse {
        private QueryResult queryResult;
        private ChartData chartData;
        private ChatMessage chatMessage;
    }
}