#!/bin/bash

echo "🚀 TAS Query POC Quick Start"
echo "============================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to check command exists
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo -e "${RED}❌ $1 is not installed${NC}"
        return 1
    else
        echo -e "${GREEN}✓ $1 is installed${NC}"
        return 0
    fi
}

# Function to check port
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${YELLOW}⚠️  Port $1 is already in use${NC}"
        return 1
    else
        echo -e "${GREEN}✓ Port $1 is available${NC}"
        return 0
    fi
}

echo "📋 Checking prerequisites..."
echo ""

# Check Java
if check_command java; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "  Version: $java_version"
fi

# Check Docker
check_command docker
check_command docker-compose

# Check Gradle
if ! check_command gradle; then
    echo -e "${YELLOW}  Using Gradle from temp directory${NC}"
    GRADLE_CMD="/tmp/gradle-8.5/bin/gradle"
else
    GRADLE_CMD="gradle"
fi

echo ""
echo "🔍 Checking ports..."
check_port 5432
check_port 8080

echo ""
echo "🗄️  Setting up PostgreSQL..."

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml not found!${NC}"
    exit 1
fi

# Start PostgreSQL
echo "Starting PostgreSQL container..."
docker-compose down 2>/dev/null
docker-compose up -d

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL to be ready"
for i in {1..30}; do
    if docker exec tas-postgres pg_isready -U postgres &>/dev/null; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Verify database
echo "Verifying database schema..."
table_count=$(docker exec tas-postgres psql -U postgres -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'tas_demo';" 2>/dev/null | tr -d ' ')

if [ "$table_count" -gt "0" ]; then
    echo -e "${GREEN}✓ Database schema loaded successfully (${table_count} tables)${NC}"
else
    echo -e "${RED}❌ Database schema not loaded${NC}"
    echo "Loading schema manually..."
    docker exec -i tas-postgres psql -U postgres < ../tas_demo_schema_with_data.sql
fi

echo ""
echo "🔨 Building application..."

# Download Gradle if needed
if [ ! -d "/tmp/gradle-8.5" ] && ! command -v gradle &> /dev/null; then
    echo "Downloading Gradle..."
    wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip -P /tmp
    unzip -q /tmp/gradle-8.5-bin.zip -d /tmp
    GRADLE_CMD="/tmp/gradle-8.5/bin/gradle"
fi

# Build
$GRADLE_CMD build -x test

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo ""
echo "🎯 Starting Spring Boot application..."
echo ""
echo "The application will start on http://localhost:8080"
echo "Press Ctrl+C to stop"
echo ""

# Run application
$GRADLE_CMD bootRun