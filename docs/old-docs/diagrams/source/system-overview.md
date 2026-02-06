# Portal Universe 시스템 아키텍처 개요

## 전체 시스템 구조

```mermaid
graph TB
    subgraph "Frontend Layer"
        PS[portal-shell<br/>Host]
        BF[blog-frontend<br/>Remote]
        SF[shopping-frontend<br/>Remote]
        DS[design-system<br/>Shared Library]
    end

    subgraph "API Gateway"
        GW[api-gateway<br/>:8080]
    end

    subgraph "Backend Services"
        CS[config-service<br/>:8888]
        AS[auth-service<br/>:8081]
        BS[blog-service<br/>:8082]
        SS[shopping-service<br/>:8083]
        NS[notification-service<br/>:8084]
    end

    subgraph "Infrastructure"
        KAFKA[Apache Kafka<br/>KRaft Mode]
        MYSQL[(MySQL 8.0)]
        MONGO[(MongoDB)]
        REDIS[(Redis)]
    end

    PS --> GW
    BF --> PS
    SF --> PS
    DS --> BF
    DS --> SF

    GW --> AS
    GW --> BS
    GW --> SS
    GW --> NS

    AS --> CS
    BS --> CS
    SS --> CS
    NS --> CS

    AS --> MYSQL
    SS --> MYSQL
    BS --> MONGO

    AS --> KAFKA
    SS --> KAFKA
    NS --> KAFKA

    SS --> REDIS
```

## Frontend Module Federation

```mermaid
graph LR
    subgraph "Host (portal-shell)"
        A[Shell App] --> |expose| B[apiClient]
        A --> |expose| C[authStore]
    end

    subgraph "Remote (blog-frontend)"
        D[Blog Module] --> |import| B
        D --> |import| C
    end

    subgraph "Remote (shopping-frontend)"
        E[Shopping Module] --> |import| B
        E --> |import| C
    end

    subgraph "Shared (design-system)"
        F[UI Components]
    end

    D --> |use| F
    E --> |use| F
```

## Backend Service Communication

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as auth-service
    participant Shopping as shopping-service
    participant Kafka

    Client->>Gateway: Request with JWT
    Gateway->>Auth: Validate Token (Feign)
    Auth-->>Gateway: Token Valid
    Gateway->>Shopping: Forward Request
    Shopping->>Shopping: Process Order
    Shopping->>Kafka: Publish OrderEvent
    Kafka-->>Auth: Consume Event
    Note over Auth: Update user activity
```

## 포트 매핑

| 서비스 | Local Port | K8s NodePort |
|--------|------------|--------------|
| portal-shell | 5173 | 30000 |
| blog-frontend | 5174 | 30001 |
| shopping-frontend | 5175 | 30002 |
| design-system | 5176 | 30003 |
| api-gateway | 8080 | 30080 |
| config-service | 8888 | 30888 |
| auth-service | 8081 | 30081 |
| blog-service | 8082 | 30082 |
| shopping-service | 8083 | 30083 |
| notification-service | 8084 | 30084 |

## 관련 문서

- [Architecture - Identity Model](../../architecture/system/identity-model.md)
- [Architecture - Signup Flow](../../old-docs/signup-flow.md)
- [PRD-001 E-commerce Core](../../prd/PRD-001-ecommerce-core.md)
