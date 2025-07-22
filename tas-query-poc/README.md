# TAS Query POC - Natural Language Database Interface

A proof-of-concept Spring Boot application that enables natural language querying of the Time and Attendance System (TAS) database.

## Prerequisites

1. Java 17+
2. PostgreSQL with TAS schema loaded
3. Ollama installed locally
4. Gradle 8.x (or use included wrapper)

## Quick Start

1. **Install Ollama**
   ```bash
   curl -fsSL https://ollama.com/install.sh | sh
   ```

2. **Pull Llama2 model**
   ```bash
   ollama pull llama2
   ```

3. **Configure Database**
   - Update `src/main/resources/application.yml` with your PostgreSQL credentials
   - Ensure the TAS schema is loaded (use `tas_demo_schema_with_data.sql`)

4. **Run the Application**
   ```bash
   ./gradlew bootRun
   ```

5. **Access the Chat Interface**
   - Open browser to http://localhost:8080

## Example Queries

- "Show all active tenants"
- "How many colleagues are in each location?"
- "List exceptions for the last week"
- "Show planned shifts for today"
- "Which exceptions are resolved?"

## Project Structure

```
tas-query-poc/
├── src/main/java/com/tas/poc/
│   ├── config/         # Configuration classes
│   ├── controller/     # REST controllers
│   ├── service/        # Business logic
│   └── model/          # Data models
└── src/main/resources/
    ├── templates/      # Thymeleaf templates
    └── application.yml # Configuration
```

## Development Status

This is a POC demonstrating core functionality. See `tas-query-poc-plan.md` for implementation details and roadmap.