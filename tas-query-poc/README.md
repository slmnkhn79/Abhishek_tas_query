# TAS Query POC - Natural Language Database Query Interface

A Spring Boot application that provides a natural language interface for querying Time and Attendance System (TAS) databases using AI-powered SQL generation and predictive analytics.

## Features

- **Natural Language to SQL**: Convert plain English questions into SQL queries using AI
- **Predictive Analytics**: Analyze trends, detect anomalies, and provide actionable insights
- **Interactive Chat Interface**: Web-based UI with real-time responses and data visualization
- **Smart Recommendations**: Context-aware follow-up suggestions based on query results
- **Data Visualization**: Automatic chart generation using Chart.js
- **Conversation Memory**: Maintains context across multiple queries in a session
- **Fallback Mechanism**: Pattern matching ensures functionality even without AI

## Technology Stack

- **Backend**: Spring Boot 3.x, Spring AI 1.0.0
- **Database**: PostgreSQL
- **AI Model**: SQLCoder 7B via Ollama
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Visualization**: Chart.js
- **Build Tool**: Gradle

## Prerequisites

- Java 17 or higher
- PostgreSQL 12+
- Ollama (optional, for AI features)
- Gradle (wrapper included)

## Quick Start

### 1. Database Setup

```bash
# Create database
psql -U postgres -c "CREATE DATABASE postgres"

# Load schema and sample data
psql -U postgres -d postgres -f tas_demo_schema_with_data.sql
```

### 2. Install Ollama (Optional but Recommended)

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama service
ollama serve

# Pull SQLCoder model
ollama pull sqlcoder:7b
```

### 3. Run the Application

```bash
# Clone repository
git clone <repository-url>
cd tas-query-poc

# Build application
./gradlew clean build

# Run application
./gradlew bootRun
```

The application will start at http://localhost:8080

## Configuration

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: sqlcoder:7b
        options:
          temperature: 0.0
```

## Usage Examples

### Basic Queries
- "Show all active tenants"
- "List colleagues by location"
- "Display tenant overview"

### Analytics Queries
- "Show daily exception trends"
- "Analyze shift patterns"
- "Exception status distribution"

### Complex Queries
- "Top 5 colleagues generating exceptions"
- "Show me colleagues with attendance issues and their shift details"
- "Which locations have the most overtime exceptions?"

## API Endpoints

### Chat Interface
- `GET /` - Main chat interface
- `GET /about` - About page

### REST API
- `POST /api/chat/message` - Send chat message
- `GET /api/chat/history/{sessionId}` - Get conversation history
- `GET /api/chat/suggestions` - Get query suggestions
- `DELETE /api/chat/session/{sessionId}` - Clear session

### Example API Request

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "show daily exceptions"
  }'
```

## Architecture

### Core Services

1. **SqlGenerationService**
   - Converts natural language to SQL using AI
   - Falls back to pattern matching if AI unavailable
   - Validates SQL for security

2. **QueryService**
   - Executes SQL queries safely
   - Returns structured results
   - Handles errors gracefully

3. **InsightGenerationService**
   - Analyzes query results
   - Extracts key findings
   - Generates recommendations

4. **PredictiveAnalyticsService**
   - Performs trend analysis
   - Detects anomalies
   - Provides risk assessments

5. **ConversationService**
   - Maintains session context
   - Stores conversation history
   - Enables contextual queries

## Database Schema

The application queries the `tas_demo` schema with these main tables:

- **tenant** - Organizations/companies
- **location** - Physical locations
- **colleague_details** - Employee information
- **planned_shift** - Work schedules
- **exception** - Attendance exceptions
- **exception_detail** - Exception details
- **exception_audit** - Resolution history

## Security

- SQL injection prevention through validation
- Read-only database access (SELECT only)
- Schema restrictions enforced
- Local AI processing (no data leaves infrastructure)

## Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL status
pg_isready

# Verify connection
psql -U postgres -d postgres -c "SELECT 1"
```

### Ollama Not Working
```bash
# Check Ollama status
curl http://localhost:11434/api/tags

# List downloaded models
ollama list
```

### Application Issues
- Check logs in console output
- Verify Java version: `java -version`
- Clear Gradle cache: `./gradlew clean`

## Development

### Project Structure
```
tas-query-poc/
├── src/main/java/com/tas/poc/
│   ├── controller/     # REST and Web controllers
│   ├── service/        # Business logic
│   ├── model/          # Data models
│   └── config/         # Configuration
├── src/main/resources/
│   ├── templates/      # Thymeleaf templates
│   ├── static/         # CSS, JS, images
│   └── application.yml # Configuration
└── build.gradle        # Build configuration
```

### Adding New Query Patterns

1. Add pattern to `SqlGenerationService.QUERY_PATTERNS`
2. Create insight strategy in `InsightGenerationService`
3. Add to suggestions in `ChatController`

## Performance Tips

- First AI query may be slow (model loading)
- Subsequent queries are cached
- Pattern matching provides instant fallback
- Database indexes improve query performance

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

This is a proof of concept for demonstration purposes.

## Support

For issues and questions:
- Check the `/about` page in the application
- Review application logs
- Create an issue in the repository