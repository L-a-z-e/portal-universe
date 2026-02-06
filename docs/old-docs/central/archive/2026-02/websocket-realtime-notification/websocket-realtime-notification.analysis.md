# Gap Analysis: websocket-realtime-notification

> **Feature**: WebSocket 실시간 알림
> **Analysis Date**: 2026-02-03
> **Analyzer**: Claude (gap-detector)
> **Design Reference**: `docs/pdca/02-design/features/websocket-realtime-notification.design.md`

---

## Match Rate: 100%

---

## 1. Implementation Status

### 1.1 신규 파일

| 파일 | 상태 | 비고 |
|------|:----:|------|
| `frontend/portal-shell/src/composables/useWebSocket.ts` | ✅ | WebSocket 연결 관리 구현 완료 |
| `frontend/blog-frontend/e2e/tests/websocket.spec.ts` | ✅ | Playwright 테스트 추가 완료 |

### 1.2 수정 파일

| 파일 | 상태 | 변경 내용 |
|------|:----:|----------|
| `frontend/portal-shell/package.json` | ✅ | @stomp/stompjs ^7.3.0, sockjs-client ^1.6.1, @types/sockjs-client ^1.5.4 |
| `frontend/portal-shell/src/App.vue` | ✅ | useWebSocket() 초기화 추가 (line 6, 23) |
| `services/api-gateway/src/main/resources/application.yml` | ✅ | WebSocket 라우팅 추가 (lines 331-338) |

### 1.3 버그 수정

| 파일 | 상태 | 수정 내용 |
|------|:----:|----------|
| `services/notification-service/.../NotificationResponse.java` | ✅ | @NoArgsConstructor, @AllArgsConstructor 추가 (Redis Pub/Sub 역직렬화 오류 수정) |

---

## 2. Design Requirements Checklist

| 항목 | 설계 | 구현 | 상태 |
|------|------|------|:----:|
| STOMP Client | @stomp/stompjs 사용 | Client from @stomp/stompjs | ✅ |
| SockJS fallback | webSocketFactory 필수 | `new SockJS(getWebSocketUrl())` | ✅ |
| 구독 경로 | `/user/{userId}/queue/notifications` | Line 101 | ✅ |
| 인증 watch | 자동 연결/해제 | Lines 142-153 | ✅ |
| addNotification 호출 | 필수 | Line 116 | ✅ |
| reconnectDelay | 5000ms | Line 89 | ✅ |
| heartbeat incoming | 10000ms | Line 90 | ✅ |
| heartbeat outgoing | 10000ms | Line 91 | ✅ |
| API Gateway 라우팅 | /notification/ws/** | Lines 331-338 | ✅ |
| E2E 테스트 | websocket.spec.ts | 신규 생성 완료 | ✅ |

---

## 3. Gaps Found and Fixed

### 3.1 Redis Pub/Sub Deserialize Error (Fixed)

**문제:**
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
Cannot construct instance of `NotificationResponse` (no Creators, like default constructor, exist)
```

**원인:** `NotificationResponse` DTO에 기본 생성자가 없어서 Jackson ObjectMapper가 JSON을 역직렬화할 수 없었음

**수정:**
```java
// Before
@Getter
@Builder
public class NotificationResponse { ... }

// After
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse { ... }
```

### 3.2 E2E Test File Missing (Fixed)

**문제:** `frontend/blog-frontend/e2e/tests/websocket.spec.ts` 파일 누락

**수정:** 테스트 파일 생성 완료
- WebSocket 연결 성공 테스트
- 로그아웃 시 연결 해제 테스트
- 미인증 시 연결 안 함 테스트
- 알림 UI 표시 테스트

---

## 4. Integration Test Results

| 테스트 항목 | 결과 | 비고 |
|------------|:----:|------|
| WebSocket 연결 | ✅ | 콘솔 로그 "Connected successfully" 확인 |
| STOMP 구독 | ✅ | `/user/{userId}/queue/notifications` |
| Kafka 이벤트 발행 | ✅ | `blog.post.liked` 토픽 확인 |
| notification-service 이벤트 수신 | ✅ | `Received post liked event` 로그 |
| 알림 DB 저장 | ✅ | `Notification created: id=3` |
| API Gateway WebSocket 라우팅 | ✅ | SockJS info 정상 응답 |

---

## 5. Files Changed Summary

### Frontend
- `frontend/portal-shell/src/composables/useWebSocket.ts` (NEW)
- `frontend/portal-shell/src/App.vue` (MODIFIED)
- `frontend/portal-shell/package.json` (MODIFIED)
- `frontend/blog-frontend/e2e/tests/websocket.spec.ts` (NEW)

### Backend
- `services/api-gateway/src/main/resources/application.yml` (MODIFIED)
- `services/notification-service/.../dto/NotificationResponse.java` (MODIFIED)

---

## 6. Recommendations

### 6.1 Service Restart Required
notification-service 재시작 필요 (NotificationResponse 변경 적용)

```bash
# notification-service 재시작
./gradlew :services:notification-service:bootRun --args='--spring.profiles.active=local'
```

### 6.2 E2E Test Execution
```bash
cd frontend/blog-frontend
npx playwright test e2e/tests/websocket.spec.ts --headed
```

---

## Changelog

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-03 | 최초 분석 - Match Rate 92% |
| 1.1 | 2026-02-03 | Gap 수정 완료 - Match Rate 100% |
