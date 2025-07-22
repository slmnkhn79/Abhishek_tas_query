# TAS Query POC - Application Features & Functionality

## Overview
The TAS Query POC is a Spring Boot application that provides natural language querying capabilities for a Time and Attendance System (TAS) database. It features intelligent chat functionality, data visualization, and conversational insights.

## Core Features

### 1. Natural Language Query Processing
- **Pattern-Based SQL Generation**: Converts natural language queries to SQL
- **Supported Query Patterns**:
  - `"show active tenants"` - Lists all active tenants
  - `"show all tenants"` - Lists all tenants with details
  - `"show colleagues by location"` - Distribution with bar chart
  - `"show exceptions by type"` - Exception analysis with donut chart
  - `"show daily exceptions"` - Trend analysis with line chart
  - `"show tenant overview"` - Multi-metric analysis with grouped bar chart
  - `"show shift patterns"` - Hourly distribution with line chart
  - `"show exception status distribution"` - Stacked bar chart by tenant
  - `"show colleague activity"` - Detailed activity table

### 2. Intelligent Chat Interface
- **Conversational Responses**: Natural language responses instead of raw data
- **Query Insights**: Automatic analysis of results with key findings
- **Follow-up Suggestions**: Context-aware next query recommendations
- **Session Management**: Maintains conversation history per session

### 3. Data Visualization
- **Automatic Chart Generation**:
  - Bar charts for distributions
  - Line charts for trends
  - Donut charts for proportions
  - Stacked bar charts for multi-dimensional data
- **Smart Chart Detection**: Automatically selects appropriate chart type

### 4. Conversational Context
- **Pronoun Resolution**: Handles "that", "those", "it" references
- **Context Enhancement**: Understands "tell me more", "show details"
- **Entity Tracking**: Remembers previously mentioned tenants, locations
- **Follow-up Detection**: Recognizes when queries relate to previous results

## API Endpoints

### Test Controller (`/api/test`)
- `GET /api/test/health` - Application and database health check
- `GET /api/test/tables` - List all TAS schema tables
- `POST /api/test/query?query={query}&sessionId={sessionId}` - Execute natural language query
- `GET /api/test/conversation/{sessionId}` - Get conversation history

### Chat Controller (`/api/chat`)
- `POST /api/chat/message` - Send chat message with insights
  ```json
  {
    "sessionId": "optional-session-id",
    "message": "show colleagues by location"
  }
  ```
- `GET /api/chat/history/{sessionId}` - Get full conversation history
- `DELETE /api/chat/session/{sessionId}` - Clear conversation session
- `GET /api/chat/suggestions` - Get query suggestions

## Example Interactions

### Basic Query
**User**: "show active tenants"
**System Response**:
```
Found 2 active tenants in the system.

📊 Key Findings:
• Tenant_cf10 and Tenant_3be9 are currently active
• 3 out of 5 tenants (60%) are inactive

💡 You might also want to:
• Show tenant overview
• Show colleagues for active tenants
• Show recent activity by tenant
```

### Complex Query with Visualization
**User**: "show colleagues by location"
**System Response**:
```
Found 20 colleagues distributed across 5 locations.

📊 Key Findings:
• Location_16599b2c has the most colleagues (8)
• 2 locations have fewer than 3 colleagues

📈 I've prepared a bar chart to visualize this data.

💡 You might also want to:
• Show colleague activity
• Show exceptions by location
• Show shift coverage by location
```

### Contextual Follow-up
**User**: "tell me more about that location"
**System** (recognizes "that location" refers to Location_16599b2c):
```
Location_16599b2c details:
• 8 colleagues assigned
• Connected to 4 different tenants
• 15 planned shifts this week
• 3 open exceptions

📊 Key Findings:
• Highest colleague concentration (40% of total)
• Most diverse tenant representation
• Above average shift coverage

💡 You might also want to:
• Show colleague list for this location
• Show exception details for this location
• Compare with other locations
```

## Data Insights Features

### Trend Analysis
- Identifies increasing/decreasing patterns in time-series data
- Calculates moving averages for exception trends
- Detects anomalies in daily patterns

### Statistical Summaries
- Automatic calculation of totals, averages, and percentages
- Identification of top/bottom performers
- Distribution analysis for categorical data

### Business Intelligence
- **Staffing Insights**: Identifies understaffed locations
- **Exception Monitoring**: Tracks resolution rates and patterns
- **Shift Analysis**: Discovers peak hours and coverage gaps
- **Tenant Metrics**: Compares activity across tenants

## Technical Capabilities

### Performance
- In-memory conversation storage (configurable history size)
- Connection pooling for database efficiency
- Query result caching potential
- Sub-second response times for most queries

### Security
- Read-only query validation
- SQL injection prevention
- Session timeout management
- No direct SQL exposure to users

### Extensibility
- Modular service architecture
- Easy to add new query patterns
- Pluggable chart configurations
- Ready for AI integration (Phase 3)

## Usage Examples

### 1. Daily Operations Dashboard
```
"show daily exceptions"
"show shift patterns"
"show colleague activity"
```

### 2. Management Overview
```
"show tenant overview"
"show exception status distribution"
"show colleagues by location"
```

### 3. Problem Investigation
```
"show exceptions by type"
"tell me more about TYPE_A exceptions"
"which colleagues have the most exceptions?"
```

## Configuration
- **Conversation History**: Last 5 messages per session
- **Session Timeout**: 30 minutes of inactivity
- **Database Schema**: tas_demo
- **Supported Formats**: JSON responses with optional chart data

## Future Enhancements (Not in POC)
- AI-powered SQL generation with Ollama
- Real-time WebSocket updates
- Export functionality (CSV, PDF)
- Advanced filtering and date ranges
- Custom query builder UI
- Role-based access control

## Testing the Application

1. **Start PostgreSQL** with TAS schema loaded
2. **Run the application**: `./gradlew bootRun`
3. **Use the test script**: `./test-app.sh`
4. **Access via browser**: http://localhost:8080

The application transforms complex database queries into conversational interactions, making data exploration intuitive for non-technical users while providing powerful insights for decision-making.