# PromQL 쿼리 모음

고가용성 모니터링을 위한 PromQL 쿼리 모음입니다.

---

## 서비스 가용성

```promql
# 서비스 UP 여부
up{job="api-gateway"}

# 서비스별 UP 상태 집계
sum(up) by (job)

# 가용성 (24시간)
avg_over_time(up{job="api-gateway"}[24h]) * 100
```

---

## 요청 성공률

```promql
# 성공률 (5분)
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# 에러율
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# 특정 서비스 성공률
sum(rate(http_server_requests_seconds_count{job="api-gateway",status=~"2.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{job="api-gateway"}[5m]))
```

---

## 응답 시간

```promql
# p50 (중앙값)
histogram_quantile(0.50,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)

# p95
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)

# p99
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)

# 평균 응답 시간
sum(rate(http_server_requests_seconds_sum[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)
```

---

## JVM 메트릭

```promql
# 힙 메모리 사용률
jvm_memory_used_bytes{area="heap"}
/
jvm_memory_max_bytes{area="heap"}
* 100

# GC 일시정지 시간
rate(jvm_gc_pause_seconds_sum[5m])

# 스레드 수
jvm_threads_live_threads
jvm_threads_daemon_threads
```

---

## Circuit Breaker

```promql
# Circuit Breaker 상태
resilience4j_circuitbreaker_state

# 열린 Circuit Breaker
resilience4j_circuitbreaker_state{state="open"}

# 실패율
resilience4j_circuitbreaker_failure_rate

# 호출 수
rate(resilience4j_circuitbreaker_calls_seconds_count[5m])
```

---

## Kafka

```promql
# Consumer Lag
kafka_consumergroup_lag

# Lag 합계 (그룹별)
sum(kafka_consumergroup_lag) by (consumergroup)

# 브로커 수
kafka_brokers
```

---

## Redis

```promql
# 메모리 사용률
redis_memory_used_bytes / redis_memory_max_bytes * 100

# 연결된 클라이언트
redis_connected_clients

# Evicted 키
increase(redis_evicted_keys_total[5m])

# 명령 처리량
rate(redis_commands_processed_total[5m])
```

---

## MySQL

```promql
# 커넥션 사용률
mysql_global_status_threads_connected
/
mysql_global_variables_max_connections
* 100

# 슬로우 쿼리 (분당)
rate(mysql_global_status_slow_queries[5m]) * 60

# QPS
rate(mysql_global_status_queries[5m])
```

---

## Kubernetes

```promql
# Pod Ready 수
sum(kube_pod_status_ready{namespace="portal-universe"} == 1) by (deployment)

# Pod 재시작 수
increase(kube_pod_container_status_restarts_total{namespace="portal-universe"}[1h])

# CPU 사용률
sum(rate(container_cpu_usage_seconds_total{namespace="portal-universe"}[5m])) by (pod)
/
sum(kube_pod_container_resource_limits{namespace="portal-universe",resource="cpu"}) by (pod)
* 100

# 메모리 사용률
sum(container_memory_usage_bytes{namespace="portal-universe"}) by (pod)
/
sum(kube_pod_container_resource_limits{namespace="portal-universe",resource="memory"}) by (pod)
* 100
```

---

## 알림용 쿼리

```promql
# 서비스 다운 (1분 이상)
up == 0

# 높은 에러율 (5% 초과)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)
> 0.05

# 높은 응답 시간 (p99 > 1초)
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))
> 1.0

# 높은 메모리 사용률 (85% 초과)
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85

# Circuit Breaker 열림
resilience4j_circuitbreaker_state{state="open"} == 1

# Kafka Consumer Lag 높음
kafka_consumergroup_lag > 1000
```

---

## 대시보드 패널용

### Single Stat

```promql
# 전체 성공률 (%)
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
* 100

# 활성 서비스 수
count(up == 1)

# 열린 Circuit Breaker 수
count(resilience4j_circuitbreaker_state{state="open"} == 1) or vector(0)
```

### Time Series

```promql
# 서비스별 요청 수
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# 서비스별 에러 수
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
```
