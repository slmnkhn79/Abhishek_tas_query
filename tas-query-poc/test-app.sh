#!/bin/bash

echo "TAS Query POC - Functionality Test Script"
echo "========================================"
echo ""

# Base URL
BASE_URL="http://localhost:8080"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${BLUE}Testing: $description${NC}"
    echo "Endpoint: $method $endpoint"
    
    if [ "$method" = "GET" ]; then
        curl -s -X GET "$BASE_URL$endpoint" | jq '.' 2>/dev/null || echo "Response received"
    else
        curl -s -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" | jq '.' 2>/dev/null || echo "Response received"
    fi
    
    echo -e "${GREEN}✓ Complete${NC}"
    echo "----------------------------------------"
    echo ""
}

# Test Health Check
test_endpoint "GET" "/api/test/health" "" "Health Check"

# Test Get Tables
test_endpoint "GET" "/api/test/tables" "" "List Database Tables"

# Test Basic Queries
echo -e "${BLUE}=== Testing Query Patterns ===${NC}"
echo ""

# Test 1: Active Tenants
test_endpoint "POST" "/api/test/query?query=show%20active%20tenants" "" "Query: Show Active Tenants"

# Test 2: Colleagues by Location
test_endpoint "POST" "/api/test/query?query=show%20colleagues%20by%20location" "" "Query: Colleagues by Location (Chart)"

# Test 3: Daily Exceptions
test_endpoint "POST" "/api/test/query?query=show%20daily%20exceptions" "" "Query: Daily Exceptions (Chart)"

# Test 4: Tenant Overview
test_endpoint "POST" "/api/test/query?query=show%20tenant%20overview" "" "Query: Tenant Overview (Chart)"

# Test Chat API
echo -e "${BLUE}=== Testing Chat API ===${NC}"
echo ""

SESSION_ID="test-session-$(date +%s)"

# Chat Message 1
test_endpoint "POST" "/api/chat/message" \
    '{"sessionId":"'$SESSION_ID'","message":"Show me all active tenants"}' \
    "Chat: Active Tenants Query"

# Chat Message 2 - Follow-up
test_endpoint "POST" "/api/chat/message" \
    '{"sessionId":"'$SESSION_ID'","message":"Tell me more about Tenant_3be9"}' \
    "Chat: Follow-up Query with Context"

# Get Conversation History
test_endpoint "GET" "/api/chat/history/$SESSION_ID" "" "Get Conversation History"

# Get Query Suggestions
test_endpoint "GET" "/api/chat/suggestions" "" "Get Query Suggestions"

echo -e "${GREEN}=== Test Complete ===${NC}"