# MySQL 디스크 풀 시나리오

MySQL 디스크가 가득 찼을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 스토리지 고갈 |
| **영향 범위** | 쓰기 작업 전체 실패 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 디스크 사용량 확인

```bash
# PVC 사용량
kubectl exec -it <mysql-pod> -n portal-universe -- df -h /var/lib/mysql

# 테이블별 크기
kubectl exec -it <mysql-pod> -n portal-universe -- mysql -e "
SELECT table_name, ROUND(data_length/1024/1024, 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'portal'
ORDER BY data_length DESC;"
```

---

## 2. 디스크 풀 증상

```
ERROR 1114 (HY000): The table 'products' is full
ERROR 3 (HY000): Error writing file '/var/lib/mysql/...' (Errcode: 28)
```

---

## 3. 복구 절차

### 불필요한 데이터 삭제

```sql
-- 오래된 로그 삭제
DELETE FROM audit_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 바이너리 로그 정리
PURGE BINARY LOGS BEFORE DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### PVC 확장 (가능한 경우)

```bash
kubectl patch pvc mysql-data -n portal-universe -p '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'
```

---

## 4. 알림 규칙

```yaml
- alert: LowDiskSpace
  expr: |
    (kubelet_volume_stats_available_bytes / kubelet_volume_stats_capacity_bytes)
    < 0.15
  for: 5m
  labels:
    severity: critical
```

---

## 다음 섹션

[Elasticsearch 장애 시나리오](../elasticsearch/01-es-memory-oom.md)
