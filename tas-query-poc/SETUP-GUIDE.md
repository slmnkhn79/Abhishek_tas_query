# TAS Query POC - Complete Setup Guide

## Prerequisites

### 1. Install Java 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Verify installation
java -version
```

### 2. Install Docker & Docker Compose
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install docker.io docker-compose
sudo usermod -aG docker $USER
# Log out and back in for group changes to take effect

# macOS
# Download Docker Desktop from https://www.docker.com/products/docker-desktop

# Verify installation
docker --version
docker-compose --version
```

### 3. Install Gradle (Optional - we'll use wrapper)
```bash
# Ubuntu/Debian
sudo apt install gradle

# macOS
brew install gradle
```

## Setup Steps

### Step 1: Start PostgreSQL Database

Navigate to the project directory:
```bash
cd /home/slmnkhn79/Documents/GitHub/abhishek/tas-query-poc
```

Start PostgreSQL with the TAS schema:
```bash
docker-compose up -d
```

Wait for PostgreSQL to be ready (about 10-30 seconds):
```bash
docker-compose ps
# Should show "postgres" as "Up"
```

Verify database is initialized:
```bash
docker exec -it tas-postgres psql -U postgres -c "\dt tas_demo.*"
```

### Step 2: Build the Application

Since we don't have a Gradle wrapper, let's create it:
```bash
# If you have Gradle installed globally:
gradle wrapper --gradle-version=8.5

# OR download wrapper manually:
mkdir -p gradle/wrapper
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip
unzip gradle-8.5-bin.zip
./gradle-8.5/bin/gradle wrapper
```

Build the application:
```bash
# Using system Gradle
gradle build -x test

# OR if wrapper is available
./gradlew build -x test
```

### Step 3: Run the Application

```bash
# Using system Gradle
gradle bootRun

# OR if wrapper is available
./gradlew bootRun
```

The application will start on http://localhost:8080

### Step 4: Test the Application

In a new terminal, test the endpoints:

#### Test 1: Health Check
```bash
curl http://localhost:8080/api/test/health
```

#### Test 2: List Tables
```bash
curl http://localhost:8080/api/test/tables
```

#### Test 3: Simple Query
```bash
curl -X POST "http://localhost:8080/api/test/query?query=show%20active%20tenants"
```

#### Test 4: Chat Interface
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show colleagues by location"}'
```

## Alternative: Manual Setup Without Docker

If Docker is not available, you can use a local PostgreSQL:

### 1. Install PostgreSQL
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# macOS
brew install postgresql
brew services start postgresql
```

### 2. Create Database and Load Schema
```bash
# Create database
sudo -u postgres psql -c "CREATE DATABASE tas_database;"
sudo -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE tas_database TO postgres;"

# Load schema
sudo -u postgres psql -d tas_database -f ../tas_demo_schema_with_data.sql
```

### 3. Update application.yml
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tas_database
    username: postgres
    password: postgres
```

## Quick Start Script

Create a `start.sh` file:
```bash
#!/bin/bash
echo "Starting TAS Query POC..."

# Start PostgreSQL
echo "Starting PostgreSQL..."
docker-compose up -d

# Wait for DB
echo "Waiting for database..."
sleep 10

# Build application
echo "Building application..."
gradle build -x test || ./gradlew build -x test

# Run application
echo "Starting Spring Boot application..."
gradle bootRun || ./gradlew bootRun
```

Make it executable:
```bash
chmod +x start.sh
./start.sh
```

## Troubleshooting

### Problem: Port 5432 already in use
```bash
# Check what's using the port
sudo lsof -i :5432

# Stop local PostgreSQL if running
sudo systemctl stop postgresql

# Or change port in docker-compose.yml
ports:
  - "5433:5432"
```

### Problem: Connection refused
- Check PostgreSQL is running: `docker-compose ps`
- Check logs: `docker-compose logs postgres`
- Verify connection: `docker exec -it tas-postgres psql -U postgres`

### Problem: Build fails
- Ensure Java 17 is installed: `java -version`
- Clear Gradle cache: `gradle clean` or `rm -rf .gradle/`
- Check for compilation errors in IDE

### Problem: No Gradle wrapper
```bash
# Download Gradle manually
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip -P /tmp
unzip /tmp/gradle-8.5-bin.zip -d /tmp
/tmp/gradle-8.5/bin/gradle build -x test
/tmp/gradle-8.5/bin/gradle bootRun
```

## Verifying Everything Works

Once running, you should see:
```
Started TasQueryPocApplication in X.XXX seconds
```

Test queries:
1. http://localhost:8080/api/test/health - Should return "Application is running. Database connected: true"
2. http://localhost:8080/api/chat/suggestions - Should return list of query suggestions

## Using the Application

### Via cURL:
```bash
# Get query suggestions
curl http://localhost:8080/api/chat/suggestions | jq

# Run a query
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show tenant overview"}' | jq
```

### Via Browser:
- Open http://localhost:8080
- Use tools like Postman or Insomnia for better API testing

## Next Steps

1. **Phase 3**: Install Ollama for AI-powered SQL generation
2. **Phase 4**: Build the Thymeleaf UI
3. **Phase 5**: Complete integration testing

The application is now ready for testing natural language queries against the TAS database!