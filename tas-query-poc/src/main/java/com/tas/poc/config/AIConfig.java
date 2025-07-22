package com.tas.poc.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;
    
    @Value("${spring.ai.ollama.chat.model:sqlcoder:7b}")
    private String model;
    
    @Bean
    public OllamaApi ollamaApi() {
        log.info("Configuring Ollama API with base URL: {}", baseUrl);
        return new OllamaApi(baseUrl);
    }
    
    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        log.info("Configuring Ollama Chat Model with model: {}", model);
        
        OllamaOptions options = OllamaOptions.create()
                .withModel(model)
                .withTemperature(0.0f);
        
        OllamaChatModel chatModel = new OllamaChatModel(ollamaApi, options);
        
        // Test connection
        try {
            chatModel.call("Test connection");
            log.info("✓ Ollama connection successful");
        } catch (Exception e) {
            log.warn("⚠ Ollama not available - will use pattern matching fallback: {}", e.getMessage());
        }
        
        return chatModel;
    }
}