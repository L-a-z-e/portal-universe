# 서비스 스레드 고갈 시나리오

서버 스레드 풀이 고갈되었을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 리소스 고갈 |
| **영향 범위** | 모든 요청 처리 불가 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 스레드 고갈 원인

1. **느린 외부 호출**: 타임아웃 없는 HTTP 요청
2. **동기 블로킹**: DB 쿼리 대기
3. **부하 급증**: 트래픽 스파이크
4. **데드락**: 스레드 간 교착 상태

---

## 2. 증상

- 새 요청 거부 (Connection Refused)
- 응답 없음 (Hang)
- 타임아웃 급증

---

## 3. 모니터링

```promql
# 활성 스레드 수
jvm_threads_live_threads

# 톰캣 스레드 풀
tomcat_threads_current_threads
tomcat_threads_busy_threads
```

---

## 4. 스레드 덤프 분석

```bash
kubectl exec -it <pod> -n portal-universe -- \
  jstack $(pgrep java) > thread-dump.txt
```

---

## 5. 개선 방안

### 스레드 풀 크기 조정

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 20
```

### 비동기 처리

```java
@Async
public CompletableFuture<Result> asyncMethod() {
    // 비동기 처리
}
```

---

## 다음 시나리오

[03-service-cascade-failure.md](./03-service-cascade-failure.md) - 연쇄 장애
