package com.tas.poc.service;

import com.tas.poc.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationalContextService {
    
    private final ConversationService conversationService;
    
    public String enhanceQueryWithContext(String userQuery, String sessionId) {
        List<ChatMessage> history = conversationService.getConversationHistory(sessionId);
        
        if (history.isEmpty()) {
            return userQuery;
        }
        
        // Check for contextual references
        String enhancedQuery = resolveContextualReferences(userQuery, history);
        
        log.debug("Original query: {}, Enhanced query: {}", userQuery, enhancedQuery);
        return enhancedQuery;
    }
    
    private String resolveContextualReferences(String query, List<ChatMessage> history) {
        String lowerQuery = query.toLowerCase();
        
        // Handle "more about", "details on", etc.
        if (containsContextualPhrase(lowerQuery)) {
            return enhanceWithPreviousContext(query, history);
        }
        
        // Handle pronouns and references
        if (containsPronouns(lowerQuery)) {
            return resolvePronouns(query, history);
        }
        
        return query;
    }
    
    private boolean containsContextualPhrase(String query) {
        String[] contextualPhrases = {
            "more about", "details on", "tell me more", "show more",
            "what about", "how about", "and the", "for that"
        };
        
        for (String phrase : contextualPhrases) {
            if (query.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsPronouns(String query) {
        String[] pronouns = {
            " that ", " those ", " this ", " these ",
            " it ", " them ", " its ", " their "
        };
        
        for (String pronoun : pronouns) {
            if (query.contains(pronoun)) {
                return true;
            }
        }
        return false;
    }
    
    private String enhanceWithPreviousContext(String query, List<ChatMessage> history) {
        // Find the last successful query
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (msg.getType() == ChatMessage.MessageType.INSIGHT && msg.getResultCount() != null && msg.getResultCount() > 0) {
                // Extract entities from previous query
                String previousQuery = msg.getUserMessage();
                
                // Look for tenant names
                Pattern tenantPattern = Pattern.compile("(Tenant_\\w+|tenant\\s+\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = tenantPattern.matcher(previousQuery);
                if (matcher.find()) {
                    String tenant = matcher.group(1);
                    if (!query.contains(tenant)) {
                        return query + " for " + tenant;
                    }
                }
                
                // Look for location names  
                Pattern locationPattern = Pattern.compile("(Location_\\w+|location\\s+\\w+)", Pattern.CASE_INSENSITIVE);
                matcher = locationPattern.matcher(previousQuery);
                if (matcher.find()) {
                    String location = matcher.group(1);
                    if (!query.contains(location)) {
                        return query + " for " + location;
                    }
                }
                
                break;
            }
        }
        
        return query;
    }
    
    private String resolvePronouns(String query, List<ChatMessage> history) {
        // Simple pronoun resolution - replace with last mentioned entity
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (msg.getType() == ChatMessage.MessageType.INSIGHT && msg.getInsights() != null) {
                // Check if previous query mentioned specific entities
                String previousQuery = msg.getUserMessage().toLowerCase();
                
                // Replace "that tenant" with actual tenant name
                if (query.contains("that tenant") && previousQuery.contains("tenant")) {
                    Pattern pattern = Pattern.compile("(Tenant_\\w+)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(msg.getUserMessage());
                    if (matcher.find()) {
                        return query.replace("that tenant", matcher.group(1));
                    }
                }
                
                // Replace "those exceptions" with exception type
                if (query.contains("those exceptions") && previousQuery.contains("exception")) {
                    if (previousQuery.contains("type_a")) {
                        return query.replace("those exceptions", "TYPE_A exceptions");
                    }
                }
                
                break;
            }
        }
        
        return query;
    }
    
    public boolean isFollowUpQuery(String query, List<ChatMessage> history) {
        if (history.isEmpty()) {
            return false;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Check if it's asking for more details
        if (containsContextualPhrase(lowerQuery) || containsPronouns(lowerQuery)) {
            return true;
        }
        
        // Check if it matches suggested follow-ups
        ChatMessage lastBotMessage = history.stream()
                .filter(msg -> msg.getType() == ChatMessage.MessageType.INSIGHT)
                .reduce((first, second) -> second)
                .orElse(null);
        
        if (lastBotMessage != null && lastBotMessage.getFollowUpQuestions() != null) {
            for (String suggestion : lastBotMessage.getFollowUpQuestions()) {
                if (calculateSimilarity(query, suggestion) > 0.7) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private double calculateSimilarity(String str1, String str2) {
        String[] words1 = str1.toLowerCase().split("\\s+");
        String[] words2 = str2.toLowerCase().split("\\s+");
        
        int matches = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) || (word1.length() > 3 && word2.contains(word1))) {
                    matches++;
                    break;
                }
            }
        }
        
        return (double) matches / Math.max(words1.length, words2.length);
    }
}