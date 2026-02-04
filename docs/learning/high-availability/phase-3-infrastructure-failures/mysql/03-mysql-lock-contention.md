# MySQL 락 경합 시나리오

동시 트랜잭션으로 인한 락 경합 상황을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 동시성 문제 |
| **영향 범위** | 쓰기 작업 지연/실패 |
| **난이도** | ⭐⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 락 경합 원인

1. **동일 레코드 동시 업데이트**
2. **장시간 트랜잭션**
3. **인덱스 누락으로 테이블 락**
4. **데드락 발생**

---

## 2. 락 상태 확인

```sql
-- 현재 락 대기 확인
SELECT * FROM information_schema.INNODB_LOCK_WAITS;

-- 실행 중인 트랜잭션
SELECT * FROM information_schema.INNODB_TRX;

-- 데드락 정보
SHOW ENGINE INNODB STATUS;
```

---

## 3. 시뮬레이션

### 데드락 유발 (테스트용)

```sql
-- Session 1
START TRANSACTION;
UPDATE products SET stock = stock - 1 WHERE id = 1;
-- (대기)

-- Session 2
START TRANSACTION;
UPDATE products SET stock = stock - 1 WHERE id = 2;
UPDATE products SET stock = stock - 1 WHERE id = 1; -- 대기

-- Session 1 (데드락 발생)
UPDATE products SET stock = stock - 1 WHERE id = 2;
```

---

## 4. 개선 방안

### 락 타임아웃 설정

```sql
SET innodb_lock_wait_timeout = 10;  -- 10초
```

### 알림 규칙

```yaml
- alert: MySQLDeadlocks
  expr: rate(mysql_global_status_innodb_deadlocks[5m]) > 0
  labels:
    severity: warning
```

---

## 다음 시나리오

[04-mysql-disk-full.md](./04-mysql-disk-full.md) - 디스크 풀
