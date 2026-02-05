# 네트워크 파티션 시나리오

서비스 간 네트워크가 분리되었을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 네트워크 장애 |
| **영향 범위** | 서비스 간 통신 불가 |
| **난이도** | ⭐⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 네트워크 파티션이란?

```
[정상 상태]
API Gateway ←→ Auth Service ←→ MySQL

[파티션 발생]
API Gateway ←→ Auth Service  ✗  MySQL
                              │
                        네트워크 단절
```

---

## 2. 시뮬레이션

### NetworkPolicy로 트래픽 차단

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-auth-to-mysql
  namespace: portal-universe
spec:
  podSelector:
    matchLabels:
      app: auth-service
  policyTypes:
    - Egress
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: api-gateway  # MySQL 제외
```

---

## 3. 증상

- 커넥션 타임아웃
- Retry 시도
- Circuit Breaker 열림

---

## 4. 개선 방안

### 재시도 설정

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 10000
      validation-timeout: 5000
```

### Health Check 강화

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  failureThreshold: 3
  periodSeconds: 5
```

---

## 다음 단계

[Phase 4: 고가용성 아키텍처 개선](../../phase-4-ha-architecture/01-replicas-scaling.md)
