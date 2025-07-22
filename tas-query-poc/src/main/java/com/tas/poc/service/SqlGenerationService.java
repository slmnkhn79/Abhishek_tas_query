package com.tas.poc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for converting natural language queries to SQL.
 * 
 * Current implementation (Phase 2 - POC):
 * - Uses pattern matching to map common phrases to predefined SQL queries
 * - Supports 8+ query patterns with complex joins and aggregations
 * - Provides conversation context for future AI integration
 * 
 * Future implementation (Phase 3):
 * - Will integrate with Spring AI and Ollama for dynamic SQL generation
 * - Will use LLM to understand query intent and generate appropriate SQL
 * - Will leverage conversation history for context-aware query generation
 * 
 * The service handles queries for:
 * - Tenant management and overview
 * - Colleague distribution and activity
 * - Exception tracking and analysis
 * - Shift patterns and coverage
 * - Multi-dimensional data analysis with visualization support
 * 
 * @author TAS Query POC Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlGenerationService {
    
    /**
     * Injected ConversationService to access chat history for context.
     * Used to build context-aware prompts for AI integration.
     */
    private final ConversationService conversationService;
    
    /**
     * Optional ChatClient for AI-powered SQL generation.
     * When available, uses SQLCoder model for natural language to SQL conversion.
     * Falls back to pattern matching if not available.
     */
    @Autowired(required = false)
    private ChatClient chatClient;
    
    /**
     * Static map of natural language patterns to SQL queries.
     * This is a temporary solution for POC demonstration.
     * In production, this will be replaced by AI-powered SQL generation.
     * 
     * Pattern matching is case-insensitive and uses contains() for flexibility.
     */
    private static final Map<String, String> QUERY_PATTERNS = new HashMap<>();
    
    static {
        // Basic patterns
        QUERY_PATTERNS.put("active tenants", 
            "SELECT tenant_id, tenant_name, tenant_code, onboarded_date_time_utc FROM tas_demo.tenant WHERE is_active = true");
        
        QUERY_PATTERNS.put("all tenants", 
            "SELECT * FROM tas_demo.tenant ORDER BY tenant_name");
        
        // Complex queries with joins
        QUERY_PATTERNS.put("colleagues by location", 
            """
            SELECT 
                l.location_name,
                t.tenant_name,
                COUNT(cd.colleague_uuid) as colleague_count
            FROM tas_demo.location l
            JOIN tas_demo.tenant t ON l.tenant_id = t.tenant_id
            LEFT JOIN tas_demo.colleague_details cd ON l.location_id = cd.location_id
            GROUP BY l.location_name, t.tenant_name, l.location_id
            ORDER BY colleague_count DESC
            """);
        
        QUERY_PATTERNS.put("exceptions by type", 
            """
            SELECT 
                e.exception_type,
                COUNT(*) as exception_count,
                AVG(e.exception_duration) as avg_duration_mins,
                COUNT(DISTINCT e.colleague_uuid) as affected_colleagues
            FROM tas_demo.exception e
            GROUP BY e.exception_type
            ORDER BY exception_count DESC
            """);
        
        QUERY_PATTERNS.put("daily exceptions", 
            """
            SELECT 
                DATE(e.exception_date_utc) as exception_date,
                COUNT(*) as total_exceptions,
                COUNT(CASE WHEN ed.status = 'RESOLVED' THEN 1 END) as resolved_count,
                COUNT(CASE WHEN ed.status = 'OPEN' THEN 1 END) as open_count
            FROM tas_demo.exception e
            JOIN tas_demo.exception_detail ed ON e.exception_id = ed.exception_id
            WHERE e.exception_date_utc >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY DATE(e.exception_date_utc)
            ORDER BY exception_date DESC
            """);
        
        QUERY_PATTERNS.put("tenant overview", 
            """
            SELECT 
                t.tenant_name,
                t.is_active,
                COUNT(DISTINCT l.location_id) as location_count,
                COUNT(DISTINCT cd.colleague_uuid) as colleague_count,
                COUNT(DISTINCT ps.planned_shift_id) as shift_count
            FROM tas_demo.tenant t
            LEFT JOIN tas_demo.location l ON t.tenant_id = l.tenant_id
            LEFT JOIN tas_demo.colleague_details cd ON t.tenant_id = cd.tenant_id
            LEFT JOIN tas_demo.planned_shift ps ON t.tenant_id = ps.tenant_id
            GROUP BY t.tenant_id, t.tenant_name, t.is_active
            ORDER BY colleague_count DESC
            """);
        
        QUERY_PATTERNS.put("shift patterns", 
            """
            SELECT 
                EXTRACT(HOUR FROM start_date_time_utc) as shift_hour,
                COUNT(*) as shift_count,
                COUNT(DISTINCT colleague_uuid) as unique_colleagues
            FROM tas_demo.planned_shift
            WHERE start_date_time_utc >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY EXTRACT(HOUR FROM start_date_time_utc)
            ORDER BY shift_hour
            """);
        
        QUERY_PATTERNS.put("exception status distribution", 
            """
            SELECT 
                ed.status,
                t.tenant_name,
                COUNT(*) as count
            FROM tas_demo.exception_detail ed
            JOIN tas_demo.tenant t ON ed.tenant_id = t.tenant_id
            GROUP BY ed.status, t.tenant_name
            ORDER BY t.tenant_name, ed.status
            """);
        
        QUERY_PATTERNS.put("colleague activity", 
            """
            SELECT 
                cd.colleague_uuid,
                t.tenant_name,
                l.location_name,
                COUNT(DISTINCT ps.planned_shift_id) as total_shifts,
                COUNT(DISTINCT e.exception_id) as total_exceptions
            FROM tas_demo.colleague_details cd
            JOIN tas_demo.tenant t ON cd.tenant_id = t.tenant_id
            JOIN tas_demo.location l ON cd.location_id = l.location_id
            LEFT JOIN tas_demo.planned_shift ps ON cd.colleague_uuid = ps.colleague_uuid
            LEFT JOIN tas_demo.exception e ON cd.colleague_uuid = e.colleague_uuid
            GROUP BY cd.colleague_uuid, t.tenant_name, l.location_name
            HAVING COUNT(DISTINCT ps.planned_shift_id) > 0 OR COUNT(DISTINCT e.exception_id) > 0
            ORDER BY total_exceptions DESC
            LIMIT 20
            """);
        
        // Complex query: Top 5 colleagues with exceptions and their shift details
        QUERY_PATTERNS.put("top 5 colleagues", 
            """
            WITH colleague_exceptions AS (
                SELECT 
                    e.colleague_uuid,
                    cd.colleague_payload->>'name' as colleague_name,
                    t.tenant_name,
                    l.location_name,
                    COUNT(DISTINCT e.exception_id) as exception_count,
                    STRING_AGG(DISTINCT e.exception_type, ', ') as exception_types,
                    AVG(e.exception_duration) as avg_exception_duration_mins
                FROM tas_demo.exception e
                JOIN tas_demo.colleague_details cd ON e.colleague_uuid = cd.colleague_uuid
                JOIN tas_demo.tenant t ON cd.tenant_id = t.tenant_id
                JOIN tas_demo.location l ON cd.location_id = l.location_id
                WHERE e.exception_date_utc >= CURRENT_DATE - INTERVAL '30 days'
                GROUP BY e.colleague_uuid, cd.colleague_payload->>'name', t.tenant_name, l.location_name
                ORDER BY exception_count DESC
                LIMIT 5
            ),
            recent_shifts AS (
                SELECT 
                    ps.colleague_uuid,
                    COUNT(*) as total_shifts,
                    MIN(ps.start_date_time_utc) as earliest_shift,
                    MAX(ps.end_date_time_utc) as latest_shift,
                    AVG(EXTRACT(EPOCH FROM (ps.end_date_time_utc - ps.start_date_time_utc))/3600) as avg_shift_hours
                FROM tas_demo.planned_shift ps
                WHERE ps.start_date_time_utc >= CURRENT_DATE - INTERVAL '30 days'
                GROUP BY ps.colleague_uuid
            ),
            exception_details AS (
                SELECT 
                    e.colleague_uuid,
                    ed.status,
                    COUNT(*) as status_count
                FROM tas_demo.exception e
                JOIN tas_demo.exception_detail ed ON e.exception_id = ed.exception_id
                WHERE e.exception_date_utc >= CURRENT_DATE - INTERVAL '30 days'
                GROUP BY e.colleague_uuid, ed.status
            )
            SELECT 
                ce.colleague_uuid,
                ce.colleague_name,
                ce.tenant_name,
                ce.location_name,
                ce.exception_count,
                ce.exception_types,
                ce.avg_exception_duration_mins,
                COALESCE(rs.total_shifts, 0) as total_shifts,
                rs.earliest_shift,
                rs.latest_shift,
                ROUND(rs.avg_shift_hours::numeric, 2) as avg_shift_hours,
                STRING_AGG(
                    CASE WHEN ed.status IS NOT NULL 
                    THEN ed.status || ': ' || ed.status_count 
                    END, ', '
                ) as exception_status_breakdown,
                CASE 
                    WHEN ce.exception_count > 10 THEN 'High risk - frequent exceptions'
                    WHEN ce.avg_exception_duration_mins > 60 THEN 'Long duration exceptions'
                    WHEN ce.exception_types LIKE '%ABSENCE%' THEN 'Attendance issues'
                    WHEN ce.exception_types LIKE '%LATE_IN%' OR ce.exception_types LIKE '%EARLY_OUT%' THEN 'Punctuality issues'
                    ELSE 'General exceptions'
                END as exception_reason
            FROM colleague_exceptions ce
            LEFT JOIN recent_shifts rs ON ce.colleague_uuid = rs.colleague_uuid
            LEFT JOIN exception_details ed ON ce.colleague_uuid = ed.colleague_uuid
            GROUP BY 
                ce.colleague_uuid, ce.colleague_name, ce.tenant_name, ce.location_name,
                ce.exception_count, ce.exception_types, ce.avg_exception_duration_mins,
                rs.total_shifts, rs.earliest_shift, rs.latest_shift, rs.avg_shift_hours
            ORDER BY ce.exception_count DESC
            """);
    }
    
    /**
     * Generates SQL from natural language query using AI with pattern matching fallback.
     * 
     * Process:
     * 1. First attempts AI-powered generation using SQLCoder model
     * 2. Falls back to pattern matching if AI is unavailable or fails
     * 3. Returns help message if no patterns match
     * 
     * @param naturalLanguageQuery The user's natural language query
     * @param sessionId The session ID for conversation context
     * @return Generated SQL query string
     */
    public String generateSql(String naturalLanguageQuery, String sessionId) {
        log.info("Generating SQL for query: {}", naturalLanguageQuery);
        
        // Get conversation context
        String context = conversationService.getConversationContext(sessionId);
        log.debug("Conversation context: {}", context);
        
        // Try AI-powered generation first if available
        if (chatClient != null) {
            try {
                String aiGeneratedSql = generateSqlWithAI(naturalLanguageQuery, context);
                if (aiGeneratedSql != null && isValidSql(aiGeneratedSql)) {
                    log.info("Successfully generated SQL using AI");
                    return aiGeneratedSql;
                }
            } catch (Exception e) {
                log.warn("AI SQL generation failed, falling back to pattern matching: {}", e.getMessage());
            }
        }
        
        // Fallback to pattern matching
        String query = naturalLanguageQuery.toLowerCase();
        
        for (Map.Entry<String, String> pattern : QUERY_PATTERNS.entrySet()) {
            if (query.contains(pattern.getKey())) {
                log.info("Matched pattern: {}", pattern.getKey());
                return pattern.getValue();
            }
        }
        
        // Default query if no pattern matches
        log.warn("No pattern matched, returning default query");
        return """
            SELECT 'Available queries:
            - show active tenants
            - show all tenants  
            - colleagues by location (chart)
            - exceptions by type (chart)
            - daily exceptions (chart)
            - tenant overview (chart)
            - shift patterns (chart)
            - exception status distribution (chart)
            - colleague activity
            - top 5 colleagues generating exceptions' as message""";
    }
    
    /**
     * Generates SQL using AI-powered natural language processing.
     * Uses SQLCoder model optimized for SQL generation.
     * 
     * @param query The natural language query
     * @param context Conversation history for context
     * @return Generated SQL or null if generation fails
     */
    private String generateSqlWithAI(String query, String context) {
        String prompt = buildPromptWithContext(query, context);
        
        try {
            // Create messages for the chat
            List<Message> messages = new ArrayList<>();
            
            // System message to set the context
            messages.add(new SystemMessage("""
                You are SQLCoder, an expert at converting natural language queries to SQL.
                You must return ONLY valid PostgreSQL SQL queries without any explanation or markdown.
                Always use the schema prefix 'tas_demo.' for all tables.
                If you cannot generate a valid query, return NULL.
                """));
            
            // User message with the actual query
            messages.add(new UserMessage(prompt));
            
            // Call the AI model
            Prompt aiPrompt = new Prompt(messages);
            ChatResponse response = chatClient.call(aiPrompt);
            
            String generatedSql = response.getResult().getOutput().getContent();
            
            // Clean up the response (remove markdown if present)
            generatedSql = cleanSqlResponse(generatedSql);
            
            log.debug("AI generated SQL: {}", generatedSql);
            return generatedSql;
            
        } catch (Exception e) {
            log.error("Error generating SQL with AI: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Builds a comprehensive prompt for the AI model with database context.
     * Optimized for SQLCoder model to generate accurate SQL queries.
     * 
     * @param query The user's natural language query
     * @param context Previous conversation context
     * @return Formatted prompt string
     */
    private String buildPromptWithContext(String query, String context) {
        return String.format("""
            Database Schema for Time and Attendance System (TAS):
            
            Tables in schema 'tas_demo':
            1. tenant (tenant_id UUID PRIMARY KEY, tenant_name TEXT, tenant_code TEXT, onboarded_date_time_utc TIMESTAMP, is_active BOOLEAN)
            2. location (location_id UUID PRIMARY KEY, location_name TEXT, tenant_id UUID REFERENCES tenant, is_active BOOLEAN)
            3. colleague_details (id UUID PRIMARY KEY, colleague_uuid UUID, colleague_payload TEXT, tenant_id UUID REFERENCES tenant, location_id UUID REFERENCES location)
            4. planned_shift (planned_shift_id UUID PRIMARY KEY, colleague_uuid UUID, start_date_time_utc TIMESTAMP, end_date_time_utc TIMESTAMP, shift_payload TEXT, tenant_id UUID REFERENCES tenant)
            5. exception (exception_id UUID PRIMARY KEY, exception_type TEXT, location_uuid UUID, colleague_uuid UUID, exception_date_utc TIMESTAMP, tenant_id UUID REFERENCES tenant, exception_duration INT)
            6. exception_detail (exception_summary_id UUID PRIMARY KEY, exception_id UUID REFERENCES exception, tenant_id UUID REFERENCES tenant, start_date_time_utc TIMESTAMP, end_date_time_utc TIMESTAMP, duration_mins INT, is_balanced BOOLEAN, status TEXT)
            7. exception_audit (audit_exception_id UUID PRIMARY KEY, exception_id UUID REFERENCES exception, exceptionAction TEXT, created_date_time_utc TIMESTAMP, manager_uuid UUID, payload TEXT)
            
            Common exception types: 'LATE_IN', 'EARLY_OUT', 'MISSED_PUNCH', 'OVERTIME', 'ABSENCE'
            Exception statuses: 'OPEN', 'RESOLVED', 'PENDING'
            
            Previous conversation context:
            %s
            
            User query: %s
            
            Generate a PostgreSQL query to answer this question. Use appropriate JOINs, aggregations, and filters as needed.
            """, context, query);
    }
    
    /**
     * Cleans the AI-generated SQL response.
     * Removes markdown code blocks and extra formatting.
     * 
     * @param sql The raw SQL response from AI
     * @return Cleaned SQL string
     */
    private String cleanSqlResponse(String sql) {
        if (sql == null) return null;
        
        // Remove markdown code blocks
        sql = sql.replaceAll("```sql", "").replaceAll("```", "");
        
        // Remove leading/trailing whitespace
        sql = sql.trim();
        
        // Remove any explanatory text before SELECT/WITH
        int selectIndex = sql.toUpperCase().indexOf("SELECT");
        int withIndex = sql.toUpperCase().indexOf("WITH");
        
        if (selectIndex > 0 || withIndex > 0) {
            int startIndex = Math.min(
                selectIndex > 0 ? selectIndex : Integer.MAX_VALUE,
                withIndex > 0 ? withIndex : Integer.MAX_VALUE
            );
            sql = sql.substring(startIndex);
        }
        
        return sql;
    }
    
    /**
     * Validates if the generated SQL is safe and valid.
     * Performs basic security and syntax checks.
     * 
     * @param sql The SQL query to validate
     * @return true if SQL appears valid and safe
     */
    private boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String upperSql = sql.toUpperCase();
        
        // Basic SQL injection prevention
        if (upperSql.contains("DROP") || upperSql.contains("DELETE") || 
            upperSql.contains("TRUNCATE") || upperSql.contains("ALTER") ||
            upperSql.contains("CREATE") || upperSql.contains("INSERT") ||
            upperSql.contains("UPDATE")) {
            log.warn("Potentially dangerous SQL detected: {}", sql);
            return false;
        }
        
        // Check for basic SELECT statement structure
        if (!upperSql.contains("SELECT")) {
            return false;
        }
        
        // Check for proper schema usage
        if (!sql.contains("tas_demo.")) {
            log.warn("SQL missing schema prefix: {}", sql);
            return false;
        }
        
        return true;
    }
}