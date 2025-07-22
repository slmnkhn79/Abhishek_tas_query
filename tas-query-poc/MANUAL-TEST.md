# Manual Testing Guide - TAS Query POC

## Quick Test Commands

Once the application is running on http://localhost:8080, use these commands to test:

### 1. Basic Health Check
```bash
curl http://localhost:8080/api/test/health
```
Expected: `"Application is running. Database connected: true"`

### 2. Test Natural Language Queries

#### Show Active Tenants
```bash
curl -X POST "http://localhost:8080/api/test/query?query=show%20active%20tenants" | jq
```

#### Show Colleagues by Location (with Chart)
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show colleagues by location"}' | jq
```

#### Show Daily Exceptions (with Insights)
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "show daily exceptions"}' | jq
```

### 3. Test Conversational Context

Start a conversation:
```bash
SESSION_ID="test-session-123"

# First message
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "'$SESSION_ID'",
    "message": "show tenant overview"
  }' | jq

# Follow-up with context
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "'$SESSION_ID'",
    "message": "tell me more about Tenant_3be9"
  }' | jq
```

### 4. View Conversation History
```bash
curl http://localhost:8080/api/chat/history/test-session-123 | jq
```

### 5. Get Query Suggestions
```bash
curl http://localhost:8080/api/chat/suggestions | jq
```

## Expected Responses

### Query with Insights Response Structure:
```json
{
  "sessionId": "test-session-123",
  "message": "Found 5 colleagues distributed across 5 locations.\n\n📊 Key Findings:\n• Location_652f has the most colleagues (8)\n• 3 locations have fewer than 3 colleagues\n\n📈 I've prepared a bar chart to visualize this data.\n\n💡 You might also want to:\n• Show colleague activity\n• Show exceptions by location",
  "insights": {
    "summary": "Found 5 colleagues distributed across 5 locations.",
    "keyFindings": [...],
    "followUpSuggestions": [...]
  },
  "chartData": {
    "chartType": "bar",
    "labels": [...],
    "datasets": [...]
  },
  "queryResult": {
    "query": "SELECT ...",
    "results": [...],
    "rowCount": 5,
    "success": true
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

## Testing All Query Patterns

```bash
# Run all test queries
for query in "show active tenants" "show all tenants" "show colleagues by location" \
             "show exceptions by type" "show daily exceptions" "show tenant overview" \
             "show shift patterns" "show exception status distribution"; do
    echo "Testing: $query"
    curl -s -X POST http://localhost:8080/api/chat/message \
      -H "Content-Type: application/json" \
      -d "{\"message\": \"$query\"}" | jq '.message'
    echo "---"
    sleep 1
done
```

## Browser Testing

1. Install a JSON viewer extension in your browser
2. Navigate to:
   - http://localhost:8080/api/test/health
   - http://localhost:8080/api/chat/suggestions
   - http://localhost:8080/api/test/tables

## Using Postman/Insomnia

Import these requests:

### Chat Message
- **Method**: POST
- **URL**: http://localhost:8080/api/chat/message
- **Headers**: Content-Type: application/json
- **Body**:
```json
{
  "sessionId": "my-session",
  "message": "show tenant overview"
}
```

### Query Test
- **Method**: POST
- **URL**: http://localhost:8080/api/test/query?query=show%20exceptions%20by%20type
- **Headers**: None required

## Verifying Features

✅ **Natural Language Processing**: Try variations like "list colleagues", "show me colleagues", "colleagues by location"

✅ **Chart Generation**: Look for `chartData` in responses for visualization-ready data

✅ **Insights**: Check for `insights.keyFindings` with meaningful analysis

✅ **Context Awareness**: Use "tell me more", "what about", "show details" after a query

✅ **Follow-up Suggestions**: Each response includes relevant next queries

## Common Issues

1. **"No matching query pattern found"**: Use exact phrases from suggestions
2. **Connection refused**: Ensure both PostgreSQL and Spring Boot are running
3. **Empty results**: Check database is loaded with test data
4. **Port conflicts**: Change ports in application.yml and docker-compose.yml