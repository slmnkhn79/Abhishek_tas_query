package com.tas.poc.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Spring AI with Ollama integration.
 * 
 * This configuration enables AI-powered SQL generation using embedded models
 * running locally through Ollama. The primary model used is SQLCoder, which
 * is specifically optimized for natural language to SQL conversion.
 * 
 * Features:
 * - Local model execution (no external API calls)
 * - SQLCoder model for optimal SQL generation
 * - Fallback to pattern matching if AI is unavailable
 * - Zero-cost operation after initial model download
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.ollama", name = "base-url")
public class AIConfig {
    
    /**
     * Spring AI auto-configures the ChatClient bean based on application.yml settings.
     * This method is here to log the configuration status.
     * 
     * @param chatClient The auto-configured Ollama chat client
     * @return The same chat client for use in other services
     */
    @Bean
    public ChatClient chatClient(ChatClient chatClient) {
        log.info("AI Configuration: Ollama ChatClient initialized");
        log.info("Using model: sqlcoder:7b for SQL generation");
        log.info("Ollama base URL: http://localhost:11434");
        
        // Test connection on startup
        try {
            chatClient.call("Test connection");
            log.info("✓ Ollama connection successful");
        } catch (Exception e) {
            log.warn("⚠ Ollama not available - will use pattern matching fallback: {}", e.getMessage());
        }
        
        return chatClient;
    }
}