#!/bin/bash

echo "=== TAS Query POC Setup Script ==="
echo "This script will help you set up and run the application"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
        exit 1
    fi
}

# Check Java
echo "1. Checking Java installation..."
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_status 0 "Java 17+ found"
    else
        print_status 1 "Java 17+ required, found Java $JAVA_VERSION"
    fi
else
    print_status 1 "Java not found. Please install Java 17+"
fi

# Check PostgreSQL
echo -e "\n2. Checking PostgreSQL..."
if command_exists psql; then
    print_status 0 "PostgreSQL client found"
    
    # Test connection
    echo "   Testing PostgreSQL connection..."
    PGPASSWORD=postgres psql -h localhost -U postgres -d postgres -c "SELECT 1" >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_status 0 "PostgreSQL connection successful"
    else
        echo -e "${YELLOW}⚠ Could not connect to PostgreSQL. Make sure it's running on localhost:5432${NC}"
        echo "   Starting PostgreSQL might be required: sudo service postgresql start"
    fi
else
    echo -e "${YELLOW}⚠ PostgreSQL client not found. Database operations may fail.${NC}"
fi

# Check Ollama (optional)
echo -e "\n3. Checking Ollama (optional for AI features)..."
if command_exists ollama; then
    print_status 0 "Ollama found"
    
    # Check if Ollama is running
    curl -s http://localhost:11434/api/tags >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_status 0 "Ollama service is running"
        
        # Check for SQLCoder model
        if ollama list | grep -q "sqlcoder:7b"; then
            print_status 0 "SQLCoder model found"
        else
            echo -e "${YELLOW}⚠ SQLCoder model not found. Downloading...${NC}"
            ollama pull sqlcoder:7b
        fi
    else
        echo -e "${YELLOW}⚠ Ollama not running. Start with: ollama serve${NC}"
        echo "   The app will work without AI using pattern matching"
    fi
else
    echo -e "${YELLOW}⚠ Ollama not installed. AI features will be disabled.${NC}"
    echo "   The app will work with pattern matching fallback"
fi

# Make gradlew executable
echo -e "\n4. Setting up Gradle..."
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    print_status 0 "Gradle wrapper is ready"
else
    print_status 1 "Gradle wrapper not found"
fi

# Create database schema
echo -e "\n5. Setting up database schema..."
if [ -f "tas_demo_schema_with_data.sql" ]; then
    echo "   Found schema file. Loading into database..."
    PGPASSWORD=postgres psql -h localhost -U postgres -d postgres -f tas_demo_schema_with_data.sql >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_status 0 "Database schema loaded successfully"
    else
        echo -e "${YELLOW}⚠ Could not load schema. Database might already be set up.${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Schema file not found. Make sure tas_demo_schema_with_data.sql exists${NC}"
fi

# Build the application
echo -e "\n6. Building the application..."
./gradlew clean build --no-daemon
if [ $? -eq 0 ]; then
    print_status 0 "Application built successfully"
else
    print_status 1 "Build failed. Check the error messages above"
fi

# Summary
echo -e "\n${GREEN}=== Setup Complete ===${NC}"
echo ""
echo "To run the application:"
echo "  ./gradlew bootRun"
echo ""
echo "Then open your browser to:"
echo "  http://localhost:8080"
echo ""
echo "Optional: For AI features, run in another terminal:"
echo "  ollama serve"
echo ""