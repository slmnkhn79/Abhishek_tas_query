# Quick Start Guide - TAS Query POC

## Prerequisites Check

Run this command to check all prerequisites:
```bash
./setup.sh
```

## Manual Setup (if setup.sh fails)

### 1. Install Java 17+
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Verify
java -version
```

### 2. Install PostgreSQL
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# macOS
brew install postgresql
brew services start postgresql

# Start PostgreSQL
sudo service postgresql start  # Linux
brew services start postgresql # macOS
```

### 3. Create Database and Load Schema
```bash
# Create database (if needed)
sudo -u postgres createdb postgres

# Load schema
sudo -u postgres psql -d postgres -f tas_demo_schema_with_data.sql

# Or with password
PGPASSWORD=postgres psql -h localhost -U postgres -d postgres -f tas_demo_schema_with_data.sql
```

### 4. Install Ollama (Optional for AI)
```bash
# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# macOS
brew install ollama

# Start Ollama
ollama serve

# Download SQLCoder model (in another terminal)
ollama pull sqlcoder:7b
```

## Running the Application

### Option 1: Using Gradle Wrapper (Recommended)
```bash
# Build the application
./gradlew clean build

# Run the application
./gradlew bootRun
```

### Option 2: Using JAR file
```bash
# Build JAR
./gradlew clean build

# Run JAR
java -jar build/libs/tas-query-poc-0.0.1-SNAPSHOT.jar
```

### Option 3: Using the test runner
```bash
# Run with automatic testing
./run-test.sh
```

## Accessing the Application

1. **Web Interface**: http://localhost:8080
2. **API Documentation**: http://localhost:8080/about

## Testing the Application

### Via Web Interface
1. Open http://localhost:8080
2. Try these queries:
   - "Show all active tenants"
   - "Show daily exceptions"
   - "Top 5 colleagues generating exceptions"

### Via API
```bash
# Basic query
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show all tenants"}'

# With session
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-123",
    "message": "show exceptions by type"
  }'
```

## Troubleshooting

### Build Fails
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches
./gradlew clean build --refresh-dependencies
```

### Database Connection Issues
```bash
# Check PostgreSQL is running
pg_isready

# Check connection
psql -h localhost -U postgres -d postgres -c "SELECT 1"

# Check if schema exists
psql -h localhost -U postgres -d postgres -c "\dt tas_demo.*"
```

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Ollama Not Working
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check available models
ollama list

# The app works without Ollama using pattern matching
```

## Environment Variables (Optional)

Create `.env` file:
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=postgres

# Ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=sqlcoder:7b

# Application
SERVER_PORT=8080
```

## Development Mode

For hot reload during development:
```bash
./gradlew bootRun --continuous
```

## Stopping the Application

- **From terminal**: Press `Ctrl+C`
- **If running in background**: 
  ```bash
  # Find the process
  ps aux | grep tas-query-poc
  
  # Kill it
  kill <PID>
  ```

## Next Steps

1. Try different queries in the chat interface
2. Check the logs for SQL generation details
3. Experiment with AI vs pattern matching
4. View insights and charts for your queries

## Support

- Check application logs: Look at console output
- Enable debug logging: Set `logging.level.com.tas.poc=DEBUG` in application.yml
- Review the README.md for detailed documentation