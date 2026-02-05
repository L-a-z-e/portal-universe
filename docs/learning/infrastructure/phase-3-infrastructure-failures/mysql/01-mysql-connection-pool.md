# MySQL 커넥션 풀 고갈 시나리오

MySQL 커넥션 풀이 고갈되었을 때의 영향과 대응을 학습합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 연결 리소스 고갈 |
| **영향 범위** | 데이터베이스 의존 서비스 전체 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 현재 설정

### HikariCP 기본 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 최대 연결
      minimum-idle: 5            # 최소 유휴 연결
      connection-timeout: 30000  # 연결 대기 타임아웃 (ms)
```

### MySQL max_connections

```sql
SHOW VARIABLES LIKE 'max_connections';
-- 기본값: 151
```

---

## 2. 고갈 원인

1. **트래픽 급증**: 동시 요청 > pool size
2. **느린 쿼리**: 연결 점유 시간 증가
3. **연결 누수**: 트랜잭션 미종료

---

## 3. 증상

```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

---

## 4. 시뮬레이션

### 부하 테스트

```bash
# k6 부하 테스트
k6 run services/load-tests/k6/scenarios/a-shopping-flow.js
```

### 연결 수 모니터링

```sql
SHOW STATUS LIKE 'Threads_connected';
SHOW PROCESSLIST;
```

---

## 5. 개선 방안

### 풀 사이즈 조정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      leak-detection-threshold: 60000  # 60초 이상 점유 시 경고
```

### 알림 규칙

```yaml
- alert: MySQLConnectionsHigh
  expr: |
    mysql_global_status_threads_connected
    /
    mysql_global_variables_max_connections
    > 0.8
  for: 5m
  labels:
    severity: warning
```

---

## 다음 시나리오

[02-mysql-slow-query.md](./02-mysql-slow-query.md) - 슬로우 쿼리
