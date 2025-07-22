package com.tas.poc.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Spring AI with Ollama integration.
 * 
 * This configuration enables AI-powered SQL generation using embedded models
 * running locally through Ollama. The primary model used is SQLCoder, which
 * is specifically optimized for natural language to SQL conversion.
 * 
 * Spring AI auto-configures the OllamaChatModel based on application.yml settings.
 * No explicit bean definition is needed as Spring Boot's auto-configuration
 * handles it when the Ollama starter is on the classpath.
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
    
    public AIConfig() {
        log.info("AI Configuration: Ollama integration enabled");
        log.info("Model will be configured from application.yml");
        log.info("Using sqlcoder:7b for SQL generation");
    }
}