# 서비스 메모리 누수 시나리오

애플리케이션의 메모리 누수로 인한 장애를 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 리소스 고갈 |
| **영향 범위** | 해당 서비스 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 메모리 누수 원인

1. **캐시 무한 증가**: 만료 없는 캐시
2. **리스너 미해제**: 이벤트 리스너
3. **커넥션 누수**: 닫히지 않은 연결
4. **스레드 로컬**: ThreadLocal 미정리

---

## 2. 증상

- 힙 메모리 지속적 증가
- GC 빈도 증가
- 응답 시간 점진적 증가
- 결국 OOM 발생

---

## 3. 모니터링

```promql
# 힙 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# GC 시간
rate(jvm_gc_pause_seconds_sum[5m])
```

---

## 4. 진단

```bash
# 힙 덤프 생성
kubectl exec -it <pod> -n portal-universe -- \
  jmap -dump:format=b,file=/tmp/heap.hprof $(pgrep java)

# 힙 덤프 분석 (로컬)
kubectl cp portal-universe/<pod>:/tmp/heap.hprof ./heap.hprof
# Eclipse MAT 또는 VisualVM으로 분석
```

---

## 5. 알림 규칙

```yaml
- alert: HighMemoryUsage
  expr: |
    jvm_memory_used_bytes{area="heap"}
    /
    jvm_memory_max_bytes{area="heap"}
    > 0.85
  for: 5m
  labels:
    severity: warning
```

---

## 다음 시나리오

[02-service-thread-exhaustion.md](./02-service-thread-exhaustion.md) - 스레드 고갈
