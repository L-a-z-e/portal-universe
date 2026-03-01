# Query Optimization Guide

> Author: Laze
> Created: 2026-03-01

## N+1 Query Prevention

### Global Batch Fetch Size

모든 JPA 서비스에 `default_batch_fetch_size: 100`이 적용되어 있다.
LAZY 로딩 시 개별 SELECT 대신 IN 절로 최대 100개씩 배치 조회한다.

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100  # LAZY N+1 방지 (읽기)
        jdbc:
          batch_size: 20               # INSERT/UPDATE 배치 (쓰기)
```

- `default_batch_fetch_size`: **읽기** 최적화 — LAZY 프록시 초기화 시 IN 절 배치
- `jdbc.batch_size`: **쓰기** 최적화 — INSERT/UPDATE 문 배치 전송

### 적용 서비스

| Service | batch_fetch_size | jdbc.batch_size |
|---------|-----------------|-----------------|
| auth-service | 100 | 20 |
| shopping-service | 100 | 20 |
| shopping-seller-service | 100 | 20 |
| shopping-settlement-service | 100 | 50 |
| drive-service | 100 | 20 |
| payment-service | 100 | 20 |

### N+1 해결 전략 선택 기준

| 상황 | 전략 | 예시 |
|------|------|------|
| 단건 조회 + 연관 엔티티 | JOIN FETCH | `findByOrderNumberWithItems` |
| 목록 조회 + LAZY 연관 | global batch_fetch_size | Order 목록 → OrderItem |
| 루프 내 findById | findAllById + Map | TimeDeal 상품 검증 |
| MongoDB N번 upsert | BulkOperations | Tag 일괄 생성 |

## JPQL Best Practices

### 명시적 JOIN 사용

```java
// Bad: 묵시적 조인 (의도 불명확)
@Query("SELECT ur.role.roleKey FROM UserRole ur WHERE ur.userId = :userId")

// Good: 명시적 조인 (의도 명확)
@Query("SELECT r.roleKey FROM UserRole ur JOIN ur.role r WHERE ur.userId = :userId")
```

## MongoDB Bulk Upsert Pattern

Race Condition 방지 + N쿼리 → 1쿼리:

```java
BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, Tag.class);
for (String name : tagNames) {
    Query query = Query.query(Criteria.where("name").is(name));
    Update update = new Update()
            .setOnInsert("name", name)        // 없을 때만 설정
            .set("lastUsedAt", Instant.now()); // 항상 갱신
    bulkOps.upsert(query, update);
}
bulkOps.execute();
```

- `$setOnInsert`: 문서가 없을 때만 필드 설정 (기존 문서 보존)
- `BulkMode.UNORDERED`: 순서 무관, 병렬 처리, 부분 실패 허용
