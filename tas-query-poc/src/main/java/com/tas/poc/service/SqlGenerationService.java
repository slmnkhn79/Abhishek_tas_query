package com.tas.poc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
    }
    
    public String generateSql(String naturalLanguageQuery, String sessionId) {
        log.info("Generating SQL for query: {}", naturalLanguageQuery);
        
        // Get conversation context
        String context = conversationService.getConversationContext(sessionId);
        log.debug("Conversation context: {}", context);
        
        // For POC Phase 2, use simple pattern matching
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
            - colleague activity' as message""";
    }
    
    public String buildPromptWithContext(String query, String context) {
        // This will be used in Phase 3 for AI integration
        return String.format("""
            You are a SQL expert for a Time and Attendance System (TAS).
            
            Database Schema:
            - tenant (tenant_id UUID, tenant_name TEXT, tenant_code TEXT, onboarded_date_time_utc TIMESTAMP, is_active BOOLEAN)
            - location (location_id UUID, location_name TEXT, tenant_id UUID, is_active BOOLEAN)
            - colleague_details (id UUID, colleague_uuid UUID, colleague_payload TEXT, tenant_id UUID, location_id UUID)
            - planned_shift (planned_shift_id UUID, colleague_uuid UUID, start_date_time_utc TIMESTAMP, end_date_time_utc TIMESTAMP, shift_payload TEXT, tenant_id UUID)
            - exception (exception_id UUID, exception_type TEXT, location_uuid UUID, colleague_uuid UUID, exception_date_utc TIMESTAMP, tenant_id UUID, exception_duration INT)
            - exception_detail (exception_summary_id UUID, exception_id UUID, tenant_id UUID, start_date_time_utc TIMESTAMP, end_date_time_utc TIMESTAMP, duration_mins INT, is_balanced BOOLEAN, status TEXT)
            - exception_audit (audit_exception_id UUID, exception_id UUID, exceptionAction TEXT, created_date_time_utc TIMESTAMP, manager_uuid UUID, payload TEXT)
            
            Previous conversation:
            %s
            
            User query: %s
            
            Generate a PostgreSQL query to answer the user's question. Return only the SQL query without any explanation.
            Use the schema 'tas_demo' for all tables.
            """, context, query);
    }
}