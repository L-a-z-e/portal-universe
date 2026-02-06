# Circuit Breaker (Resilience4j)

## 학습 목표
- Circuit Breaker 패턴의 원리 이해
- Resilience4j 설정 학습
- Portal Universe의 장애 대응 전략 분석

---

## 1. Circuit Breaker 패턴

### 1.1 상태 전이 다이어그램

```
                     ┌─────────────────────────────────────────────────────┐
                     │                                                     │
                     │              failure rate ≥ threshold               │
                     │                                                     │
                     ▼                                                     │
┌──────────────┐         ┌──────────────┐         ┌──────────────┐        │
│    CLOSED    │────────►│     OPEN     │────────►│  HALF_OPEN   │────────┘
│              │         │              │         │              │
│  정상 동작   │         │  요청 차단   │         │  제한적 허용  │
│  장애 감시   │         │  즉시 실패   │         │  회복 테스트  │
└──────────────┘         └──────────────┘         └──────────────┘
       ▲                        │                        │
       │                        │                        │
       │                        │ wait-duration          │ success
       │                        │ 경과                    │
       │                        └────────────────────────┘
       │                                                 │
       │                     success rate ≥ threshold    │
       └─────────────────────────────────────────────────┘
```

### 1.2 상태별 동작

| 상태 | 동작 | 전이 조건 |
|------|------|----------|
| **CLOSED** | 모든 요청 허용, 실패율 모니터링 | 실패율 ≥ threshold → OPEN |
| **OPEN** | 모든 요청 즉시 실패, Fallback 반환 | wait-duration 경과 → HALF_OPEN |
| **HALF_OPEN** | 제한된 요청 허용 (테스트) | 성공 → CLOSED, 실패 → OPEN |

---

## 2. Portal Universe 설정 분석

### 2.1 기본 설정

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        # 슬라이딩 윈도우 방식: 최근 N개 요청 기준
        sliding-window-type: count_based
        sliding-window-size: 20

        # 실패율 임계값: 50% 이상이면 OPEN
        failure-rate-threshold: 50

        # OPEN 상태 유지 시간: 10초
        wait-duration-in-open-state: 10s

        # HALF_OPEN에서 허용할 요청 수
        permitted-number-of-calls-in-half-open-state: 5

        # HALF_OPEN으로 자동 전환
        automatic-transition-from-open-to-half-open-enabled: true
```

### 2.2 서비스별 인스턴스

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authCircuitBreaker:
        base-config: default

      blogCircuitBreaker:
        base-config: default

      shoppingCircuitBreaker:
        base-config: default
```

---

## 3. 동작 시나리오

### 3.1 정상 상태 (CLOSED)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Window Size: 20 | Failure Rate: 30% (6/20)                                 │
│  Status: CLOSED (임계값 50% 미만)                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Client ─────► Gateway ─────► Shopping Service                             │
│                    │                  │                                      │
│                    │    성공/실패     │                                      │
│                    │◄─────────────────┘                                      │
│                    │                                                         │
│                    │   모든 요청 정상 처리                                    │
│                    ▼                                                         │
│               Client                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 장애 감지 (CLOSED → OPEN)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Window Size: 20 | Failure Rate: 55% (11/20)                                │
│  Status: OPEN (임계값 50% 초과)                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Client ─────► Gateway ──X──► Shopping Service (호출 차단)                  │
│                    │                                                         │
│                    │   즉시 Fallback 응답                                    │
│                    ▼                                                         │
│   Client ◄───── "Shopping Service is currently unavailable"                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 회복 테스트 (HALF_OPEN)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  wait-duration (10s) 경과                                                    │
│  Status: HALF_OPEN | Permitted Calls: 5                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   5개 요청만 Shopping Service로 전달                                         │
│                                                                              │
│   • 성공률 높음 → CLOSED (정상 복구)                                         │
│   • 실패율 높음 → OPEN (다시 차단)                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Gateway 라우트 설정

### 4.1 CircuitBreaker 필터 적용

```yaml
routes:
  - id: shopping-service-route
    uri: ${services.shopping.url}
    predicates:
      - Path=/api/shopping/**
    filters:
      - StripPrefix=2
      - name: CircuitBreaker
        args:
          name: shoppingCircuitBreaker        # 인스턴스 이름
          fallbackUri: forward:/fallback/shopping  # Fallback 경로
```

### 4.2 Fallback Controller

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/blog")
    public Mono<String> blogServiceFallback() {
        return Mono.just(
            "Blog Service is currently unavailable. Please try again later."
        );
    }

    @GetMapping("/fallback/shopping")
    public Mono<String> shoppingServiceFallback() {
        return Mono.just(
            "Shopping Service is currently unavailable. Please try again later."
        );
    }

    @GetMapping("/fallback/auth")
    public Mono<String> authServiceFallback() {
        return Mono.just(
            "Auth Service is currently unavailable. Please try again later."
        );
    }
}
```

---

## 5. TimeLimiter 설정

### 5.1 타임아웃 설정

```yaml
resilience4j:
  timelimiter:
    configs:
      default:
        timeout-duration: 5s    # 5초 타임아웃

    instances:
      authCircuitBreaker:
        base-config: default
      blogCircuitBreaker:
        base-config: default
      shoppingCircuitBreaker:
        base-config: default
```

### 5.2 동작

```
요청 시작 ─────────────────────────────────────────► 5초 경과
    │                                                    │
    │   응답 대기                                        │
    │                                                    ▼
    │                                            TimeoutException
    │                                                    │
    │                                            Circuit Breaker
    │                                            실패로 카운트
    ▼
 정상 응답 (5초 이내)
```

---

## 6. 슬라이딩 윈도우 타입

### 6.1 Count-Based (Portal Universe 사용)

```yaml
sliding-window-type: count_based
sliding-window-size: 20
```

최근 20개 요청의 실패율을 계산합니다.

```
[요청 1] [요청 2] [요청 3] ... [요청 20]
   ✓        ✗        ✓           ✗
          └────────────────────────┘
              최근 20개 요청 기준
              실패율 계산
```

### 6.2 Time-Based

```yaml
sliding-window-type: time_based
sliding-window-size: 10    # 10초
```

최근 10초간의 요청 실패율을 계산합니다.

---

## 7. 설정 파라미터 상세

| 파라미터 | 값 | 설명 |
|----------|-----|------|
| `sliding-window-type` | count_based | 요청 수 기반 윈도우 |
| `sliding-window-size` | 20 | 윈도우 크기 (20개 요청) |
| `failure-rate-threshold` | 50 | 실패율 임계값 (50%) |
| `wait-duration-in-open-state` | 10s | OPEN 상태 유지 시간 |
| `permitted-number-of-calls-in-half-open-state` | 5 | HALF_OPEN에서 허용 요청 수 |
| `timeout-duration` | 5s | 요청 타임아웃 |

---

## 8. 모니터링

### 8.1 Actuator Endpoints

```bash
# Circuit Breaker 상태 조회
GET /actuator/circuitbreakers

# 특정 인스턴스 상태
GET /actuator/circuitbreaker/shoppingCircuitBreaker

# 이벤트 조회
GET /actuator/circuitbreakerevents
```

### 8.2 메트릭

```
resilience4j.circuitbreaker.state
resilience4j.circuitbreaker.calls
resilience4j.circuitbreaker.failure_rate
resilience4j.circuitbreaker.slow_call_rate
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **CLOSED** | 정상 상태, 모든 요청 허용 |
| **OPEN** | 장애 감지, 모든 요청 즉시 실패 |
| **HALF_OPEN** | 회복 테스트, 제한적 요청 허용 |
| **failure-rate-threshold** | OPEN 전이 임계값 (50%) |
| **wait-duration** | OPEN 상태 유지 시간 (10s) |
| **Fallback** | 장애 시 대체 응답 |
| **TimeLimiter** | 요청 타임아웃 설정 |

---

## 10. Fallback 전략 개선 방향

```java
// 향후 개선: 구조화된 에러 응답
@GetMapping("/fallback/shopping")
public Mono<ApiResponse<Void>> shoppingServiceFallback() {
    return Mono.just(ApiResponse.error(
        "SHOPPING_SERVICE_UNAVAILABLE",
        "Shopping Service is currently unavailable",
        HttpStatus.SERVICE_UNAVAILABLE
    ));
}
```

---

## 다음 학습

- [JWT 검증](./jwt-validation.md)
- [Rate Limiting](./rate-limiting.md)
- [분산 추적 (Zipkin)](../../docs/learning/infra/zipkin-tracing.md)
