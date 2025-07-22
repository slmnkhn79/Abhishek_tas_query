# Spring Boot MCP Architecture for Azure PostgreSQL Integration

## 1. High-Level Architecture (HLD) Diagram

```mermaid
graph TB
    subgraph "Spring Boot MCP Client Application"
        A1[AI Chat Interface]
        A2[Natural Language Processor]
        A3[MCP Client Controller]
        A4[Query Builder]
        A5[Response Handler]
    end
    
    subgraph "MCP Protocol Layer"
        B1[MCP Client Transport<br/>SSE/WebSocket]
        B2[MCP Server Transport<br/>SSE/WebSocket]
        B3[JSON-RPC Protocol]
        B4[Message Serialization]
    end
    
    subgraph "Spring Boot MCP Server Application"
        C1[MCP Server Controller]
        C2[Tool Registry<br/>- list_databases<br/>- list_tables<br/>- execute_query<br/>- insert_data<br/>- create_table]
        C3[Resource Manager<br/>- schema_info<br/>- table_data<br/>- server_config]
        C4[Prompt Templates<br/>- query_suggestions<br/>- analysis_prompts]
        C5[Database Service Layer]
        C6[Authentication Service]
    end
    
    subgraph "Azure Infrastructure"
        D1[Azure Database for PostgreSQL<br/>Flexible Server]
        D2[Microsoft Entra ID]
        D3[Connection Pool]
    end
    
    A3 --> B1
    B1 <--> B3
    B3 <--> B2
    B2 --> C1
    
    C1 --> C2
    C1 --> C3
    C1 --> C4
    C2 --> C5
    C3 --> C5
    C5 --> D3
    D3 --> D1
    C6 --> D2
    
    style A3 fill:#e1f5fe
    style C1 fill:#f3e5f5
    style D1 fill:#fff3e0
```

## 2. Logical Component Diagram

```mermaid
graph TB
    subgraph "Spring Boot MCP Client"
        subgraph "Presentation Layer"
            P1[REST Controllers]
            P2[WebSocket Handlers]
            P3[Chat Interface]
        end
        
        subgraph "Service Layer"
            S1[MCP Client Service]
            S2[Query Translation Service]
            S3[Response Processing Service]
            S4[AI Integration Service]
        end
        
        subgraph "MCP Integration"
            M1[MCP Client Bean]
            M2[Tool Discovery]
            M3[Resource Access]
            M4[Prompt Handling]
        end
    end
    
    subgraph "MCP Communication"
        COM1[SSE Transport]
        COM2[JSON-RPC Messages]
        COM3[Protocol Negotiation]
    end
    
    subgraph "Spring Boot MCP Server"
        subgraph "MCP Layer"
            MS1[MCP Server Bean]
            MS2[Tool Handlers]
            MS3[Resource Handlers]
            MS4[Prompt Handlers]
        end
        
        subgraph "Business Layer"
            BS1[Database Service]
            BS2[Schema Service]
            BS3[Query Execution Service]
            BS4[Analysis Service]
        end
        
        subgraph "Data Layer"
            DS1[PostgreSQL Repository]
            DS2[Connection Manager]
            DS3[Transaction Manager]
        end
    end
    
    subgraph "Azure PostgreSQL"
        DB1[Database Instance]
        DB2[Authentication]
        DB3[Connection Pool]
    end
    
    P1 --> S1
    P2 --> S2
    P3 --> S3
    S1 --> M1
    S2 --> M2
    S3 --> M3
    
    M1 --> COM1
    M2 --> COM2
    M3 --> COM3
    
    COM1 --> MS1
    COM2 --> MS2
    COM3 --> MS3
    
    MS1 --> BS1
    MS2 --> BS2
    MS3 --> BS3
    MS4 --> BS4
    
    BS1 --> DS1
    BS2 --> DS2
    BS3 --> DS3
    
    DS1 --> DB1
    DS2 --> DB2
    DS3 --> DB3
```

## 3. Sequence Diagram - Complete Flow

```mermaid
sequenceDiagram
    participant User as User
    participant Client as Spring Boot MCP Client
    participant Transport as MCP Transport Layer
    participant Server as Spring Boot MCP Server
    participant DB as Azure PostgreSQL
    participant Auth as Microsoft Entra ID
    
    Note over User,Auth: Application Startup & Authentication
    User->>Client: Start Application
    Client->>Transport: Initialize MCP Client
    Transport->>Server: Protocol Negotiation
    Server->>Transport: Capability Exchange
    Server->>Auth: Authenticate with Entra ID
    Auth-->>Server: Authentication Token
    Server->>DB: Establish Connection Pool
    DB-->>Server: Connection Ready
    
    Note over User,Auth: Natural Language Query Processing
    User->>Client: "Show me all customers from last month"
    Client->>Client: Process Natural Language
    Client->>Transport: Tool Discovery Request
    Transport->>Server: list_tools()
    Server-->>Transport: Available Tools List
    Transport-->>Client: Tools Response
    
    Note over User,Auth: Query Execution Flow
    Client->>Transport: execute_query(sql, params)
    Transport->>Server: Tool Execution Request
    Server->>Server: Validate Query
    Server->>DB: Execute SQL Query
    DB-->>Server: Query Results
    Server->>Server: Format Response
    Server-->>Transport: Tool Response
    Transport-->>Client: Query Results
    Client->>Client: Process & Format Data
    Client-->>User: Display Results
    
    Note over User,Auth: Error Handling
    Client->>Transport: invalid_query()
    Transport->>Server: Tool Execution
    Server->>Server: Validate Query (Failed)
    Server-->>Transport: Error Response
    Transport-->>Client: Error Message
    Client-->>User: User-friendly Error
```

## 4. Data Flow Diagram

```mermaid
graph LR
    subgraph "Input Processing"
        I1[Natural Language Input]
        I2[Query Intent Analysis]
        I3[Parameter Extraction]
    end
    
    subgraph "MCP Client Processing"
        C1[Tool Selection]
        C2[Request Formation]
        C3[MCP Message Creation]
    end
    
    subgraph "Transport Layer"
        T1[Message Serialization]
        T2[SSE/WebSocket Transport]
        T3[Message Deserialization]
    end
    
    subgraph "MCP Server Processing"
        S1[Tool Handler Routing]
        S2[Parameter Validation]
        S3[SQL Generation]
        S4[Query Execution]
    end
    
    subgraph "Database Layer"
        D1[Connection Pool]
        D2[Query Processor]
        D3[Result Set]
        D4[Metadata Extraction]
    end
    
    subgraph "Response Processing"
        R1[Data Formatting]
        R2[Response Serialization]
        R3[Client Processing]
        R4[User Presentation]
    end
    
    I1 --> I2
    I2 --> I3
    I3 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> T1
    T1 --> T2
    T2 --> T3
    T3 --> S1
    S1 --> S2
    S2 --> S3
    S3 --> S4
    S4 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> D4
    D4 --> R1
    R1 --> R2
    R2 --> R3
    R3 --> R4
```

## 5. Usage Flow Diagram

```mermaid
graph TD
    subgraph "User Interaction Flows"
        U1[Database Schema Discovery]
        U2[Data Query & Analysis]
        U3[Data Modification]
        U4[Report Generation]
    end
    
    subgraph "Schema Discovery Flow"
        SD1[List Databases] --> SD2[List Tables]
        SD2 --> SD3[Get Table Schema]
        SD3 --> SD4[Display Structure]
    end
    
    subgraph "Query & Analysis Flow"
        QA1[Natural Language Query] --> QA2[Intent Recognition]
        QA2 --> QA3[SQL Generation]
        QA3 --> QA4[Query Execution]
        QA4 --> QA5[Result Analysis]
        QA5 --> QA6[Data Visualization]
    end
    
    subgraph "Data Modification Flow"
        DM1[Insert/Update Request] --> DM2[Data Validation]
        DM2 --> DM3[Transaction Start]
        DM3 --> DM4[Execute DML]
        DM4 --> DM5[Commit/Rollback]
        DM5 --> DM6[Confirmation]
    end
    
    subgraph "Report Generation Flow"
        RG1[Report Template] --> RG2[Data Collection]
        RG2 --> RG3[Aggregation]
        RG3 --> RG4[Formatting]
        RG4 --> RG5[Export/Display]
    end
    
    U1 --> SD1
    U2 --> QA1
    U3 --> DM1
    U4 --> RG1
    
    style U1 fill:#e3f2fd
    style U2 fill:#f3e5f5
    style U3 fill:#e8f5e8
    style U4 fill:#fff3e0
```

## 6. Component Integration Diagram

```mermaid
graph TB
    subgraph "Spring Boot MCP Client Application"
        subgraph "Configuration"
            CC1[MCP Client Configuration]
            CC2[Transport Configuration]
            CC3[Security Configuration]
        end
        
        subgraph "Core Components"
            CC4[MCP Client Service]
            CC5[Query Processor]
            CC6[Response Handler]
            CC7[AI Integration]
        end
        
        subgraph "Web Layer"
            CC8[REST Controllers]
            CC9[WebSocket Handlers]
            CC10[Static Resources]
        end
    end
    
    subgraph "MCP Communication Channel"
        MC1[SSE Transport]
        MC2[JSON-RPC Protocol]
        MC3[Message Queue]
    end
    
    subgraph "Spring Boot MCP Server Application"
        subgraph "MCP Configuration"
            SC1[MCP Server Configuration]
            SC2[Tool Registration]
            SC3[Resource Configuration]
        end
        
        subgraph "MCP Handlers"
            SC4[Database Tools Handler]
            SC5[Schema Resource Handler]
            SC6[Query Prompt Handler]
        end
        
        subgraph "Database Layer"
            SC7[PostgreSQL Service]
            SC8[Connection Pool]
            SC9[Transaction Manager]
        end
    end
    
    subgraph "External Services"
        ES1[Azure PostgreSQL]
        ES2[Microsoft Entra ID]
        ES3[Azure Monitor]
    end
    
    CC1 --> CC4
    CC2 --> MC1
    CC4 --> CC5
    CC5 --> CC6
    CC6 --> CC7
    CC8 --> CC4
    CC9 --> CC5
    
    MC1 --> MC2
    MC2 --> MC3
    MC3 --> SC1
    
    SC1 --> SC4
    SC2 --> SC5
    SC3 --> SC6
    SC4 --> SC7
    SC5 --> SC8
    SC6 --> SC9
    
    SC7 --> ES1
    SC8 --> ES2
    SC9 --> ES3
```

## 7. Tool Execution Flow

```mermaid
sequenceDiagram
    participant Client as MCP Client
    participant Server as MCP Server
    participant Handler as Tool Handler
    participant Service as Database Service
    participant DB as PostgreSQL
    
    Note over Client,DB: Tool Discovery
    Client->>Server: listTools()
    Server-->>Client: [list_databases, list_tables, execute_query, ...]
    
    Note over Client,DB: Database Listing
    Client->>Server: callTool("list_databases", {})
    Server->>Handler: handle_list_databases()
    Handler->>Service: getAllDatabases()
    Service->>DB: SELECT datname FROM pg_database
    DB-->>Service: Database list
    Service-->>Handler: Formatted results
    Handler-->>Server: Tool response
    Server-->>Client: Database list
    
    Note over Client,DB: Table Schema Discovery
    Client->>Server: callTool("list_tables", {"database": "mydb"})
    Server->>Handler: handle_list_tables(database)
    Handler->>Service: getTableSchema(database)
    Service->>DB: Query information_schema
    DB-->>Service: Table metadata
    Service-->>Handler: Schema information
    Handler-->>Server: Tool response
    Server-->>Client: Table schema
    
    Note over Client,DB: Query Execution
    Client->>Server: callTool("execute_query", {"sql": "SELECT * FROM users", "limit": 100})
    Server->>Handler: handle_execute_query(sql, limit)
    Handler->>Service: executeQuery(sql, params)
    Service->>DB: Execute prepared statement
    DB-->>Service: Result set
    Service-->>Handler: Formatted data
    Handler-->>Server: Tool response
    Server-->>Client: Query results
```

## 8. Error Handling Flow

```mermaid
graph TD
    subgraph "Error Sources"
        E1[Connection Errors]
        E2[Authentication Errors]
        E3[Query Errors]
        E4[Permission Errors]
        E5[Protocol Errors]
    end
    
    subgraph "Error Processing"
        P1[Error Detection]
        P2[Error Classification]
        P3[Error Logging]
        P4[Error Response Formation]
    end
    
    subgraph "Error Responses"
        R1[Client Error Messages]
        R2[Server Error Responses]
        R3[User-Friendly Messages]
        R4[Technical Error Details]
    end
    
    subgraph "Recovery Actions"
        A1[Retry Logic]
        A2[Fallback Mechanisms]
        A3[Connection Reset]
        A4[Authentication Refresh]
    end
    
    E1 --> P1
    E2 --> P1
    E3 --> P1
    E4 --> P1
    E5 --> P1
    
    P1 --> P2
    P2 --> P3
    P3 --> P4
    
    P4 --> R1
    P4 --> R2
    P4 --> R3
    P4 --> R4
    
    P2 --> A1
    P2 --> A2
    P2 --> A3
    P2 --> A4
    
    style E1 fill:#ffebee
    style E2 fill:#ffebee
    style E3 fill:#ffebee
    style P1 fill:#fff3e0
    style A1 fill:#e8f5e8
```