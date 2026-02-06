# Elasticsearch 힙 메모리 부족 시나리오

Elasticsearch 힙 메모리가 부족할 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 리소스 고갈 |
| **영향 범위** | 검색 기능 전체 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 힙 메모리 설정

```yaml
env:
  - name: ES_JAVA_OPTS
    value: "-Xms512m -Xmx512m"  # 힙 크기
```

**권장**: 물리 메모리의 50% 이하, 최대 32GB

---

## 2. OOM 증상

- `OutOfMemoryError: Java heap space`
- Circuit Breaker 트리거
- 느린 GC로 인한 응답 지연
- 노드 이탈 (Node left cluster)

---

## 3. 모니터링

```bash
# Elasticsearch 힙 사용량
curl -s "localhost:9200/_nodes/stats/jvm?pretty" | grep -A5 "heap"

# Circuit Breaker 상태
curl -s "localhost:9200/_nodes/stats/breaker?pretty"
```

---

## 4. 개선 방안

### 힙 크기 조정

```yaml
env:
  - name: ES_JAVA_OPTS
    value: "-Xms1g -Xmx1g"
```

### 인덱스 설정 최적화

```json
{
  "index.refresh_interval": "30s",
  "index.number_of_replicas": 0
}
```

---

## 다음 시나리오

[02-es-index-corruption.md](./02-es-index-corruption.md) - 인덱스 손상
