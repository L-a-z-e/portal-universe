# Elasticsearch 인덱스 손상 시나리오

인덱스가 손상되었을 때의 복구 방법을 학습합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 데이터 손상 |
| **영향 범위** | 특정 인덱스 검색 불가 |
| **난이도** | ⭐⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 인덱스 상태 확인

```bash
# 클러스터 상태
curl -s "localhost:9200/_cluster/health?pretty"

# 인덱스 상태
curl -s "localhost:9200/_cat/indices?v&health=red"

# 샤드 상태
curl -s "localhost:9200/_cat/shards?v&h=index,shard,prirep,state,unassigned.reason"
```

---

## 2. 복구 절차

### 언할당 샤드 재할당

```bash
curl -X POST "localhost:9200/_cluster/reroute?retry_failed=true"
```

### 인덱스 재생성

```bash
# 손상된 인덱스 삭제
curl -X DELETE "localhost:9200/corrupted_index"

# 재인덱싱 (데이터 소스에서)
# Shopping Service에서 제품 데이터 재색인
```

---

## 3. 알림 규칙

```yaml
- alert: ElasticsearchClusterRed
  expr: elasticsearch_cluster_health_status{color="red"} == 1
  labels:
    severity: critical
```

---

## 다음 시나리오

[03-es-query-timeout.md](./03-es-query-timeout.md) - 쿼리 타임아웃
