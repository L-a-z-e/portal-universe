# Elasticsearch 쿼리 타임아웃 시나리오

복잡한 쿼리로 인한 타임아웃 상황을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 성능 저하 |
| **영향 범위** | 검색 기능 지연 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 타임아웃 원인

1. **대량 데이터 스캔**: 필터 없는 검색
2. **복잡한 집계**: 중첩 aggregation
3. **와일드카드 쿼리**: `*keyword*`
4. **정렬 필드 누락**: 인덱스 없는 필드로 정렬

---

## 2. 느린 쿼리 확인

```bash
# 느린 로그 활성화
curl -X PUT "localhost:9200/products/_settings" -H 'Content-Type: application/json' -d'
{
  "index.search.slowlog.threshold.query.warn": "1s",
  "index.search.slowlog.threshold.fetch.warn": "1s"
}'
```

---

## 3. 개선 방안

### 쿼리 타임아웃 설정

```json
{
  "query": { ... },
  "timeout": "5s"
}
```

### 인덱스 최적화

- 적절한 mapping 설정
- 필요한 필드만 저장
- doc_values 활용

---

## 다음 섹션

[Services 장애 시나리오](../services/01-service-memory-leak.md)
