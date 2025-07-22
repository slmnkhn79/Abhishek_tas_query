package com.tas.poc.service;

import com.tas.poc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for managing conversation sessions and message history.
 * 
 * This service provides:
 * - Session-based conversation storage with configurable history size
 * - Automatic session cleanup after inactivity timeout
 * - Thread-safe in-memory storage for POC (production should use Redis/Database)
 * - Context extraction for AI/NLP processing
 * 
 * The conversation history is crucial for:
 * - Maintaining context in follow-up queries
 * - Providing conversational flow
 * - Enabling pronoun resolution and entity tracking
 * - Supporting intelligent query suggestions
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Service
public class ConversationService {
    
    /**
     * Maximum number of messages to retain per session.
     * Configured via: tas.conversation.history-size in application.yml
     * Default: 5 messages
     */
    @Value("${tas.conversation.history-size:5}")
    private int historySize;
    
    /**
     * Session timeout in minutes after last activity.
     * Configured via: tas.conversation.session-timeout-minutes in application.yml
     * Default: 30 minutes
     */
    @Value("${tas.conversation.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;
    
    /**
     * Thread-safe storage for conversation messages indexed by session ID.
     * Uses ConcurrentHashMap to handle concurrent access from multiple users.
     * In production, this should be replaced with Redis or database storage.
     */
    private final Map<String, LinkedList<ChatMessage>> conversationStore = new ConcurrentHashMap<>();
    
    /**
     * Tracks last activity time for each session to enable timeout cleanup.
     * Used to automatically remove inactive sessions and free memory.
     */
    private final Map<String, LocalDateTime> lastActivityMap = new ConcurrentHashMap<>();
    
    /**
     * Adds a new message to the conversation history for a specific session.
     * 
     * This method:
     * 1. Creates a new conversation if sessionId doesn't exist
     * 2. Adds the message to the end of the conversation
     * 3. Maintains sliding window of last N messages (removes oldest if exceeding limit)
     * 4. Updates last activity timestamp
     * 5. Triggers cleanup of expired sessions
     * 
     * @param sessionId Unique identifier for the conversation session
     * @param message The ChatMessage to add (can be USER, BOT, ERROR, or INSIGHT type)
     */
    public void addMessage(String sessionId, ChatMessage message) {
        log.debug("Adding message to session: {}", sessionId);
        
        // Get or create conversation list for this session
        LinkedList<ChatMessage> messages = conversationStore.computeIfAbsent(sessionId, k -> new LinkedList<>());
        messages.addLast(message);
        
        // Implement sliding window - keep only the last N messages
        while (messages.size() > historySize) {
            ChatMessage removed = messages.removeFirst();
            log.trace("Removed old message from session {}: {}", sessionId, removed.getId());
        }
        
        // Update activity timestamp and trigger cleanup
        lastActivityMap.put(sessionId, LocalDateTime.now());
        cleanupExpiredSessions();
    }
    
    public List<ChatMessage> getConversationHistory(String sessionId) {
        cleanupExpiredSessions();
        LinkedList<ChatMessage> messages = conversationStore.get(sessionId);
        return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }
    
    public String getConversationContext(String sessionId) {
        List<ChatMessage> history = getConversationHistory(sessionId);
        
        if (history.isEmpty()) {
            return "";
        }
        
        return history.stream()
                .map(msg -> {
                    if (msg.getType() == ChatMessage.MessageType.USER) {
                        return "User: " + msg.getUserMessage();
                    } else if (msg.getType() == ChatMessage.MessageType.BOT && msg.getSqlQuery() != null) {
                        return "Assistant generated SQL: " + msg.getSqlQuery();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
    
    public void clearSession(String sessionId) {
        log.info("Clearing session: {}", sessionId);
        conversationStore.remove(sessionId);
        lastActivityMap.remove(sessionId);
    }
    
    private void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        
        Set<String> expiredSessions = lastActivityMap.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(cutoffTime))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        expiredSessions.forEach(sessionId -> {
            log.debug("Removing expired session: {}", sessionId);
            conversationStore.remove(sessionId);
            lastActivityMap.remove(sessionId);
        });
    }
}