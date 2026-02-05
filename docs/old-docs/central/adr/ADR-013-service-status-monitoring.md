---
id: ADR-013
title: 서비스 상태 모니터링 전략
type: adr
status: accepted
created: 2026-01-21
updated: 2026-01-21
author: Laze
decision_date: 2026-01-21
reviewers: []
tags:
  - monitoring
  - health-check
  - observability
  - polling
related:
  - SCENARIO-005-service-status
  - ADR-006-remove-config-service
---

# ADR-013: 서비스 상태 모니터링 전략

## 상태
Accepted

## 날짜
2026-01-21

---

## 컨텍스트

Portal Universe는 여러 마이크로서비스로 구성된 시스템입니다. 개발자와 관리자가 각 서비스의 상태를 실시간으로 확인할 수 있는 모니터링 기능이 필요합니다.

### 현재 아키텍처

```
Portal Shell (Vue 3, :30000)
  └── API Gateway (:8080)
        ├── Auth Service (:8081)
        ├── Blog Service (:8082)
        ├── Shopping Service (:8083)
        └── Notification Service (:8084) [예정]
```

### 요구사항

1. **Health Check**: 각 서비스의 UP/DOWN/DEGRADED 상태 확인
2. **자동 갱신**: 주기적으로 상태 업데이트 (5-30초 간격)
3. **인증 불필요**: 공개 모니터링 페이지 (JWT 인증 제외)
4. **응답 시간**: 각 서비스의 Health Check 응답 시간 표시
5. **히스토리**: (선택) 최근 24시간 상태 기록

### 제약 조건

- Spring Boot Actuator `/actuator/health` 사용
- JWT 인증 작업과 충돌 회피
- 프론트엔드 단독으로 구현 가능해야 함 (백엔드 최소 수정)
- Kubernetes 환경에서도 동작해야 함

---

## Decision Drivers (결정 요인)

1. **구현 복잡도**: 빠르게 구현 가능해야 함
2. **서버 부하**: Health Check가 서비스에 과부하를 주면 안 됨
3. **실시간성**: 상태 변화를 빠르게 감지해야 함 (단, 초 단위는 불필요)
4. **확장성**: 향후 서비스 추가 시 쉽게 확장 가능해야 함
5. **인프라 부담**: 추가 인프라 없이 구현 가능해야 함

---

## Considered Options (검토한 대안)

### 옵션 1: Client-Side Polling (채택)

**설명**: 프론트엔드에서 주기적으로 각 서비스의 `/actuator/health` 호출

```typescript
// Portal Shell
setInterval(() => {
  const services = ['auth', 'blog', 'shopping']
  services.forEach(service => {
    fetch(`/api/${service}/actuator/health`)
      .then(res => updateStatus(service, res.status === 200))
  })
}, 10000) // 10초 간격
```

**장점:**
- ✅ 구현 매우 간단 (프론트엔드만 수정)
- ✅ 백엔드 변경 최소화 (Actuator 설정만)
- ✅ 추가 인프라 불필요
- ✅ 서버 부하 낮음 (10-30초 간격)
- ✅ Kubernetes 환경 호환

**단점:**
- ❌ 실시간성 낮음 (최대 10-30초 지연)
- ❌ 브라우저 탭 많을수록 중복 요청 (완화 가능)
- ❌ 히스토리 기능 없음 (별도 구현 필요)

**비용:**
- 개발: 2-3시간
- 인프라: 없음
- 유지보수: 낮음

---

### 옵션 2: WebSocket Push

**설명**: 각 서비스가 상태 변화 시 WebSocket으로 Portal Shell에 Push

```
[Service] ---(Health Event)---> [API Gateway WebSocket] ---> [Portal Shell]
```

**장점:**
- ✅ 실시간성 우수 (즉시 반영)
- ✅ 네트워크 트래픽 절약 (변화 시만 전송)

**단점:**
- ❌ 구현 복잡도 높음 (Spring WebFlux, WebSocket 설정)
- ❌ 각 서비스에 WebSocket 발행 로직 추가 필요
- ❌ 연결 관리 복잡 (재연결, Heartbeat 등)
- ❌ API Gateway에 WebSocket 라우팅 추가
- ❌ Kubernetes Ingress WebSocket 설정 필요

**비용:**
- 개발: 2-3일
- 인프라: Redis Pub/Sub 또는 Kafka Topic 추가
- 유지보수: 높음

---

### 옵션 3: Server-Sent Events (SSE)

**설명**: 서버에서 일방향 스트림으로 상태 전송

```
[API Gateway] ---(SSE Stream)---> [Portal Shell]
```

**장점:**
- ✅ WebSocket보다 구현 간단
- ✅ HTTP 기반 (방화벽 문제 없음)

**단점:**
- ❌ 여전히 백엔드 구현 필요
- ❌ 각 서비스 Health 이벤트 발행 필요
- ❌ API Gateway에 SSE 집계 로직 필요
- ❌ 브라우저 호환성 고려 (IE 미지원)

**비용:**
- 개발: 1-2일
- 인프라: 중간
- 유지보수: 중간

---

### 옵션 4: Dedicated Monitoring Service

**설명**: 별도 모니터링 서비스가 Health Check 후 결과 저장

```
[Monitoring Service] ---(Polling)---> [All Services]
[Portal Shell] ---(Query)---> [Monitoring Service DB/Redis]
```

**장점:**
- ✅ 히스토리 기능 기본 제공
- ✅ 다양한 통계 및 알림 기능 추가 가능
- ✅ 클라이언트 부하 없음

**단점:**
- ❌ 새로운 서비스 추가 (코드 + 배포)
- ❌ DB 또는 Redis 스토리지 필요
- ❌ 모니터링 서비스 자체 관리 필요
- ❌ 개발 시간 증가

**비용:**
- 개발: 3-5일
- 인프라: DB/Redis + 새 서비스 배포
- 유지보수: 높음

---

### 옵션 5: 기존 도구 활용 (Prometheus + Grafana)

**설명**: Prometheus가 Actuator Metrics 수집, Grafana로 시각화

**장점:**
- ✅ 업계 표준 도구
- ✅ 다양한 Metrics 수집 가능
- ✅ 강력한 알림 기능

**단점:**
- ❌ Portal Shell UI와 별도 시스템 (iframe 또는 외부 링크)
- ❌ Prometheus + Grafana 인프라 구축 필요
- ❌ Kubernetes 환경 필수
- ❌ 개발 환경에서 사용 어려움

**비용:**
- 개발: Helm Chart 배포 (1일)
- 인프라: Prometheus + Grafana Pod
- 유지보수: 중간

---

## 대안 비교표

| 대안 | 구현 복잡도 | 실시간성 | 서버 부하 | 인프라 비용 | 히스토리 | 평가 |
|------|-------------|----------|----------|------------|---------|------|
| **Polling** | ⭐ 매우 낮음 | 🟡 10-30초 | ⭐ 낮음 | ⭐ 없음 | ❌ | ✅ 채택 |
| **WebSocket** | ⭐⭐⭐ 높음 | ⭐ 즉시 | 🟡 중간 | 🟡 중간 | ✅ | ❌ |
| **SSE** | 🟡 중간 | ⭐ 즉시 | 🟡 중간 | 🟡 중간 | ✅ | 🟡 향후 검토 |
| **Monitoring Service** | ⭐⭐⭐ 높음 | 🟡 중간 | 🟡 중간 | ⭐⭐ 높음 | ⭐ 우수 | 🟡 Phase 2 |
| **Prometheus** | 🟡 중간 | ⭐ 우수 | ⭐ 낮음 | ⭐⭐ 높음 | ⭐ 우수 | 🟡 프로덕션 |

---

## Decision (최종 결정)

**옵션 1: Client-Side Polling 방식 채택**

### 선택 이유

1. **빠른 구현**: 2-3시간 내 완료 가능
2. **백엔드 최소 수정**: Spring Boot Actuator 설정만 변경
3. **인프라 불필요**: 추가 서비스/DB/Redis 없이 동작
4. **충분한 실시간성**: Health 상태는 초 단위로 변화하지 않음 (10초 간격 적절)
5. **확장성**: 서비스 추가 시 프론트엔드 설정만 변경
6. **Kubernetes 호환**: 특별한 설정 없이 동작

### 구현 방식

**1. Spring Boot Actuator 설정 (모든 서비스)**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
```

**2. API Gateway CORS 설정**

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:30000"
            allowedMethods: "GET"
            allowedHeaders: "*"
```

**3. Portal Shell Health Check 로직**

```typescript
// useHealthCheck.ts
export function useHealthCheck() {
  const store = useServiceStatusStore()

  const services = [
    { name: 'API Gateway', url: '/actuator/health' },
    { name: 'Auth Service', url: '/auth/actuator/health' },
    { name: 'Blog Service', url: '/blog/actuator/health' },
    { name: 'Shopping Service', url: '/shopping/actuator/health' }
  ]

  const checkAll = async () => {
    const results = await Promise.all(
      services.map(async (service) => {
        const start = Date.now()
        try {
          const res = await fetch(service.url, { timeout: 3000 })
          const data = await res.json()
          return {
            ...service,
            status: data.status === 'UP' ? 'UP' : 'DEGRADED',
            responseTime: Date.now() - start,
            timestamp: new Date()
          }
        } catch {
          return {
            ...service,
            status: 'DOWN',
            responseTime: null,
            timestamp: new Date()
          }
        }
      })
    )
    store.updateStatuses(results)
  }

  const startPolling = (interval = 10000) => {
    checkAll() // 즉시 실행
    return setInterval(checkAll, interval)
  }

  return { checkAll, startPolling }
}
```

**4. UI 컴포넌트**

```vue
<!-- ServiceStatus.vue -->
<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useHealthCheck } from '@/composables/useHealthCheck'

const { startPolling } = useHealthCheck()
let intervalId: number

onMounted(() => {
  intervalId = startPolling(10000)
})

onUnmounted(() => {
  clearInterval(intervalId)
})
</script>
```

---

## Consequences (영향)

### 긍정적 영향

1. **빠른 개발**: MVP 빠르게 출시 가능
2. **낮은 유지보수 비용**: 복잡한 인프라 없음
3. **디버깅 용이**: 브라우저 DevTools로 쉽게 확인
4. **유연한 설정**: Polling 간격 사용자가 조정 가능 (5-30초)
5. **탭 비활성화 최적화**: `document.visibilityState`로 Polling 중단 가능

### 부정적 영향

1. **실시간성 제한**: 최대 10-30초 지연
   - **완화**: 수동 새로고침 버튼 제공
   - **완화**: 상태 변화 알림 (브라우저 Notification)

2. **네트워크 트래픽**: 브라우저 탭마다 중복 요청
   - **완화**: Shared Worker 또는 BroadcastChannel 사용 (향후)
   - **완화**: Polling 간격 최적화 (10-30초)

3. **히스토리 기능 없음**: 과거 상태 조회 불가
   - **완화**: Phase 2에서 Monitoring Service 추가 계획

### 완화 전략

**1. 탭 비활성화 시 Polling 중단**
```typescript
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    clearInterval(intervalId)
  } else {
    intervalId = startPolling(10000)
  }
})
```

**2. 응답 시간 표시**
```typescript
const responseTime = Date.now() - startTime
// UI: "Auth Service: UP (23ms)"
```

**3. 에러 재시도 로직**
```typescript
const MAX_RETRIES = 3
let retryCount = 0

async function checkWithRetry(url: string) {
  try {
    return await fetch(url)
  } catch (error) {
    if (retryCount < MAX_RETRIES) {
      retryCount++
      await sleep(1000)
      return checkWithRetry(url)
    }
    throw error
  }
}
```

---

## 다음 단계

### Phase 1: MVP (현재)
- [x] Polling 방식 Health Check 구현
- [ ] Portal Shell UI 개발
- [ ] Polling 간격 설정 UI
- [ ] 수동 새로고침 버튼
- [ ] 모바일 반응형

### Phase 2: 고도화 (향후)
- [ ] Monitoring Service 추가 (히스토리 기능)
- [ ] Redis에 상태 기록 저장
- [ ] 24시간 상태 차트 (Chart.js)
- [ ] 알림 기능 (브라우저 Notification)

### Phase 3: 프로덕션 (선택)
- [ ] Prometheus + Grafana 통합
- [ ] Alertmanager 연동
- [ ] Slack/Discord Webhook

---

## 참고

### 관련 문서
- [Spring Boot Actuator Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Kubernetes Liveness/Readiness Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [SCENARIO-005 서비스 상태 모니터링](../scenarios/SCENARIO-005-service-status.md)

### 기술 스택
- **Frontend**: Vue 3, TypeScript, Pinia
- **Backend**: Spring Boot 3.5.5, Spring Boot Actuator
- **Infra**: Kubernetes (향후 Prometheus)

---

**작성자**: Laze
**검토자**: -
**최종 업데이트**: 2026-01-21
