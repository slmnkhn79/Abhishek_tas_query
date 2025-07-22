# TAS Query Assistant POC - Implementation Plan

## Project Overview
A Spring Boot proof-of-concept application that enables natural language querying of the Time and Attendance System (TAS) database using Spring AI with embedded models and conversation memory.

## Architecture Summary
- **Backend**: Spring Boot 3.x with Gradle
- **AI**: Spring AI with Ollama (local LLM)
- **Database**: PostgreSQL with existing TAS schema
- **Frontend**: Thymeleaf + Basic HTML/CSS
- **Memory**: In-memory conversation storage

## Essential Features for POC
1. Natural language to SQL conversion for 5 predefined query patterns
2. Basic conversation memory (last 5 messages)
3. Simple web-based chat interface
4. Query execution and tabular result display
5. Session-based conversation context

## Implementation Phases

### Phase 1: Basic Setup (Day 1)
**Objective**: Create project foundation with database connectivity

**Tasks**:
1. Initialize Spring Boot project with Gradle
2. Configure Gradle dependencies:
   - Spring Boot Starter Web
   - Spring Boot Starter Data JPA
   - PostgreSQL Driver
   - Spring AI Ollama Starter
   - Thymeleaf
3. Set up application.yml with database configuration
4. Create basic package structure
5. Verify database connectivity to TAS schema

**Deliverables**:
- Running Spring Boot application
- Successful connection to PostgreSQL
- Basic project structure

### Phase 2: Core Services (Day 2)
**Objective**: Implement essential backend services

**Tasks**:
1. **ConversationService.java**
   - Store last 5 messages per session
   - Provide conversation context for prompts
   
2. **QueryService.java**
   - Execute SQL queries safely
   - Return results as List<Map<String, Object>>
   
3. **SqlGenerationService.java**
   - Create prompt template with TAS schema context
   - Generate SQL from natural language
   
4. **Data Models**
   - ChatMessage.java (user input, bot response, timestamp)
   - QueryResult.java (query, results, execution time)

**Deliverables**:
- Working service layer
- Basic data models

### Phase 3: AI Integration (Day 3)
**Objective**: Connect Spring AI with Ollama for SQL generation

**Tasks**:
1. Install Ollama locally
2. Download Llama2 or Mistral model
3. Configure Spring AI to connect to Ollama
4. Create TAS-specific prompt template:
   ```
   You are a SQL expert for a Time and Attendance System.
   
   Schema:
   - tenant (tenant_id, tenant_name, tenant_code, is_active)
   - location (location_id, location_name, tenant_id)
   - colleague_details (colleague_uuid, tenant_id, location_id)
   - planned_shift (colleague_uuid, start_date_time_utc, end_date_time_utc, tenant_id)
   - exception (exception_type, colleague_uuid, location_uuid, exception_date_utc, tenant_id)
   
   Recent conversation:
   {conversation_history}
   
   Convert this to SQL: {user_query}
   Return only the SQL query, no explanations.
   ```

5. Implement support for complex query patterns:
   - Basic: active tenants, all tenants
   - Complex with joins:
     - Colleagues by location (bar chart)
     - Exceptions by type (donut chart)
     - Daily exceptions trend (line chart)
     - Tenant overview with metrics (multi-bar chart)
     - Shift patterns by hour (line chart)
     - Exception status distribution (stacked bar chart)
     - Colleague activity analysis (table)

**Deliverables**:
- Working AI integration
- SQL generation for 5 patterns

### Phase 4: Simple UI (Day 4)
**Objective**: Create basic chat interface

**Tasks**:
1. **chat.html** - Thymeleaf template
   - Chat message display area
   - Input form
   - Results table
   
2. **ChatController.java**
   - Handle chat endpoints
   - Manage sessions
   - Return results to view
   
3. **Basic styling**
   - Simple CSS for readability
   - Responsive table for results
   
4. **JavaScript** (minimal)
   - Form submission
   - Auto-scroll chat

**Deliverables**:
- Functional chat interface
- Result display capability

### Phase 5: Integration & Testing (Day 5)
**Objective**: Connect all components and verify functionality

**Tasks**:
1. Wire all components together
2. Test 5 predefined scenarios:
   - "Show all active tenants"
   - "How many colleagues are in Location_652f?"
   - "List exceptions from last week"
   - "Show planned shifts for today"
   - "Count resolved exceptions"
3. Fix critical bugs only
4. Create simple README.md
5. Document example queries

**Deliverables**:
- Working end-to-end POC
- Basic documentation

## Project Structure
```
tas-query-poc/
├── build.gradle
├── settings.gradle
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/tas/poc/
│   │   │       ├── TasQueryPocApplication.java
│   │   │       ├── config/
│   │   │       │   ├── DatabaseConfig.java
│   │   │       │   └── AIConfig.java
│   │   │       ├── controller/
│   │   │       │   └── ChatController.java
│   │   │       ├── service/
│   │   │       │   ├── ConversationService.java
│   │   │       │   ├── QueryService.java
│   │   │       │   └── SqlGenerationService.java
│   │   │       └── model/
│   │   │           ├── ChatMessage.java
│   │   │           └── QueryResult.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/
│   │       │   └── chat.html
│   │       └── static/
│   │           └── style.css
│   └── test/
│       └── java/
└── docker-compose.yml (optional for PostgreSQL)
```

## Configuration Example

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tas_database
    username: postgres
    password: password
    hikari:
      schema: tas_demo
      
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama2
          temperature: 0.1
          
server:
  port: 8080
  
tas:
  conversation:
    history-size: 5
    session-timeout: 30m
```

## Success Metrics
1. ✅ Natural language queries convert to valid SQL
2. ✅ Results display correctly in table format
3. ✅ Conversation context affects query generation
4. ✅ No crashes during normal usage
5. ✅ Response time under 3 seconds

## Future Enhancements (Out of POC Scope)
- Authentication and authorization
- Advanced error handling and recovery
- Query validation and SQL injection prevention
- WebSocket for real-time updates
- Export functionality
- Query history and favorites
- Performance optimization
- Multi-tenant support
- Advanced NLP with entity recognition
- Visualization charts
- API endpoints for external integration

## Example Queries for Testing

### Basic Queries:
1. "Show active tenants"
2. "Show all tenants"

### Complex Queries with Visualizations:
3. "Show colleagues by location" - Bar chart showing colleague distribution
4. "Show exceptions by type" - Donut chart of exception types
5. "Show daily exceptions" - Line chart with total, resolved, and open counts
6. "Show tenant overview" - Multi-metric bar chart per tenant
7. "Show shift patterns" - Line chart of shifts by hour
8. "Show exception status distribution" - Stacked bar chart by tenant and status
9. "Show colleague activity" - Table with shift and exception counts

### Natural Language Variations:
- "How many colleagues work at each location?"
- "What types of exceptions occur most?"
- "Show me exception trends over time"
- "Which tenants have the most colleagues?"
- "When do most shifts start?"
- "Break down exception status by tenant"

## Development Notes
- Keep it simple - this is a POC
- Focus on demonstrating core capability
- Use hardcoded values where appropriate
- Don't over-engineer for the POC phase
- Document assumptions and limitations