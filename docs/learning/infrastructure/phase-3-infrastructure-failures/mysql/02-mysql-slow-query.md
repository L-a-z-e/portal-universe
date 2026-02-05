# MySQL 슬로우 쿼리 시나리오

슬로우 쿼리로 인한 성능 저하 상황을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 성능 저하 |
| **영향 범위** | 전체 응답 시간 증가 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 슬로우 쿼리 원인

1. **누락된 인덱스**: Full Table Scan
2. **복잡한 JOIN**: 다중 테이블 조인
3. **대량 데이터**: LIMIT 없는 SELECT
4. **락 대기**: 트랜잭션 충돌

---

## 2. 슬로우 쿼리 로그 확인

```sql
-- 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 1초 이상

-- 슬로우 쿼리 확인
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
```

---

## 3. 쿼리 분석

```sql
-- 실행 계획 확인
EXPLAIN ANALYZE SELECT * FROM products WHERE name LIKE '%keyword%';

-- 인덱스 사용 확인
SHOW INDEX FROM products;
```

---

## 4. 개선 방안

### 인덱스 추가

```sql
CREATE INDEX idx_products_name ON products(name);
```

### 쿼리 타임아웃 설정

```yaml
spring:
  jpa:
    properties:
      javax.persistence.query.timeout: 5000  # 5초
```

---

## 5. 알림 규칙

```yaml
- alert: MySQLSlowQueries
  expr: rate(mysql_global_status_slow_queries[5m]) > 0.1
  for: 5m
  labels:
    severity: warning
```

---

## 다음 시나리오

[03-mysql-lock-contention.md](./03-mysql-lock-contention.md) - 락 경합
