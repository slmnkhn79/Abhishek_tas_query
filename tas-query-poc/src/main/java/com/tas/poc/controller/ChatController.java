package com.tas.poc.controller;

import com.tas.poc.model.*;
import com.tas.poc.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for chat-based natural language database queries.
 * 
 * This controller provides the main chat interface for the application,
 * handling natural language messages and returning intelligent responses
 * with data insights and visualizations.
 * 
 * Key features:
 * - Natural language message processing
 * - Session-based conversation management
 * - Intelligent response generation with insights
 * - Data visualization support
 * - Conversation history tracking
 * - Query suggestions
 * 
 * All responses include:
 * - Natural language explanation of results
 * - Key findings and insights
 * - Chart data for visualization
 * - Follow-up query suggestions
 * - Raw query results for advanced users
 * 
 * Security note: @CrossOrigin is enabled for POC only.
 * In production, configure proper CORS settings.
 * 
 * @author TAS Query POC Team
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For POC only - configure properly in production
public class ChatController {
    
    private final QueryService queryService;
    private final ConversationService conversationService;
    private final SqlGenerationService sqlGenerationService;
    private final ChartDataService chartDataService;
    private final ChatResponseBuilder chatResponseBuilder;
    private final ConversationalContextService contextService;
    
    @PostMapping("/message")
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        
        // Add user message
        ChatMessage userMessage = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userMessage(request.getMessage())
                .timestamp(LocalDateTime.now())
                .type(ChatMessage.MessageType.USER)
                .build();
        conversationService.addMessage(sessionId, userMessage);
        
        // Process with context
        String enhancedQuery = contextService.enhanceQueryWithContext(request.getMessage(), sessionId);
        String sql = sqlGenerationService.generateSql(enhancedQuery, sessionId);
        QueryResult result = queryService.executeQuery(sql);
        ChartData chartData = chartDataService.generateChartData(enhancedQuery, result);
        
        // Build response with insights
        ChatMessage botMessage = chatResponseBuilder.buildInsightfulResponse(
                sessionId, request.getMessage(), sql, result, chartData);
        conversationService.addMessage(sessionId, botMessage);
        
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(botMessage.getBotResponse())
                .insights(botMessage.getInsights())
                .chartData(chartData)
                .queryResult(result)
                .followUpSuggestions(botMessage.getFollowUpQuestions())
                .timestamp(botMessage.getTimestamp())
                .build();
    }
    
    @GetMapping("/history/{sessionId}")
    public ConversationHistory getHistory(@PathVariable String sessionId) {
        List<ChatMessage> messages = conversationService.getConversationHistory(sessionId);
        
        return ConversationHistory.builder()
                .sessionId(sessionId)
                .messages(messages)
                .messageCount(messages.size())
                .build();
    }
    
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        conversationService.clearSession(sessionId);
    }
    
    @GetMapping("/suggestions")
    public List<String> getQuerySuggestions() {
        return List.of(
            "Show tenant overview",
            "Show colleagues by location",
            "Show daily exceptions",
            "Show exceptions by type",
            "Show shift patterns",
            "Show exception status distribution",
            "Show active tenants",
            "Show colleague activity"
        );
    }
    
    @Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
    }
    
    @Data
    @lombok.Builder
    public static class ChatResponse {
        private String sessionId;
        private String message;
        private Insights insights;
        private ChartData chartData;
        private QueryResult queryResult;
        private List<String> followUpSuggestions;
        private LocalDateTime timestamp;
    }
    
    @Data
    @lombok.Builder
    public static class ConversationHistory {
        private String sessionId;
        private List<ChatMessage> messages;
        private int messageCount;
    }
}