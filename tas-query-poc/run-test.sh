#!/bin/bash

echo "=== TAS Query POC Test Runner ==="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Function to test API endpoint
test_api() {
    echo -e "\n${YELLOW}Testing API: $1${NC}"
    response=$(curl -s -w "\n%{http_code}" "$2")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ Success (HTTP $http_code)${NC}"
        echo "Response: $(echo $body | jq -r '.message // .' 2>/dev/null || echo $body | head -c 100)"
    else
        echo -e "${RED}✗ Failed (HTTP $http_code)${NC}"
    fi
}

# Start the application in background
echo "Starting the application..."
./gradlew bootRun > app.log 2>&1 &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 10

# Check if app is running
if ! ps -p $APP_PID > /dev/null; then
    echo -e "${RED}Application failed to start. Check app.log for details${NC}"
    cat app.log
    exit 1
fi

# Wait for port to be available
for i in {1..30}; do
    if curl -s http://localhost:8080 > /dev/null; then
        echo -e "${GREEN}✓ Application is running${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Application did not start in time${NC}"
        kill $APP_PID
        exit 1
    fi
    sleep 1
done

# Test endpoints
echo -e "\n${YELLOW}=== Testing Endpoints ===${NC}"

# Test home page
test_api "Home Page" "http://localhost:8080/"

# Test suggestions
test_api "Query Suggestions" "http://localhost:8080/api/chat/suggestions"

# Test basic query
echo -e "\n${YELLOW}Testing Chat Query${NC}"
response=$(curl -s -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show all tenants"}')

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Chat query successful${NC}"
    echo "Response: $(echo $response | jq -r '.message' 2>/dev/null || echo $response | head -c 100)"
else
    echo -e "${RED}✗ Chat query failed${NC}"
fi

# Test pattern matching queries
echo -e "\n${YELLOW}Testing Pattern Matching Queries${NC}"

queries=(
    "show active tenants"
    "colleagues by location"
    "daily exceptions"
    "tenant overview"
)

for query in "${queries[@]}"; do
    echo -e "\nTesting: $query"
    response=$(curl -s -X POST http://localhost:8080/api/chat/message \
      -H "Content-Type: application/json" \
      -d "{\"message\": \"$query\"}")
    
    if echo "$response" | grep -q "message"; then
        echo -e "${GREEN}✓ Query processed${NC}"
    else
        echo -e "${RED}✗ Query failed${NC}"
    fi
done

# Summary
echo -e "\n${YELLOW}=== Test Summary ===${NC}"
echo "Application is running on: http://localhost:8080"
echo "Process ID: $APP_PID"
echo ""
echo "To stop the application:"
echo "  kill $APP_PID"
echo ""
echo "To view logs:"
echo "  tail -f app.log"
echo ""

# Keep running for manual testing
echo -e "${GREEN}Application is ready for manual testing!${NC}"
echo "Press Ctrl+C to stop..."

# Wait for user to stop
trap "kill $APP_PID; exit" INT
wait $APP_PID