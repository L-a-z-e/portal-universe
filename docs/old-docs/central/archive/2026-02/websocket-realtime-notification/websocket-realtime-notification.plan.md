# Feature Plan: websocket-realtime-notification

> **Feature**: WebSocket 실시간 알림
> **Author**: Claude
> **Created**: 2026-02-03
> **Status**: Draft

---

## 1. 개요

### 1.1 배경

현재 Portal Universe의 알림 시스템은 **30초 폴링 방식**으로 동작합니다:

```
현재 상태:
Frontend (portal-shell)        Backend (notification-service)
        │                              │
        │──── 30초마다 GET 요청 ──────▶│
        │◀───── 알림 목록 응답 ────────│
        │                              │
```

**문제점:**
- 최대 30초 지연 (새 알림 발생 시)
- 알림이 없어도 계속 요청 → 서버 부하
- 실시간 UX 제공 불가

### 1.2 목표

Backend에 이미 구현된 WebSocket 인프라를 Frontend에서 활용하여 **실시간 푸시 알림**을 구현합니다.

```
목표 상태:
Frontend (portal-shell)        Backend (notification-service)
        │                              │
        │══════ WebSocket 연결 ════════│ (상시 연결)
        │                              │
        │◀───── 새 알림 즉시 푸시 ─────│ (1초 이내)
        │                              │
```

---

## 2. 현재 구현 상태 분석

### 2.1 Backend (✅ 이미 구현됨)

| 컴포넌트 | 파일 | 상태 |
|---------|------|------|
| WebSocket 설정 | `WebSocketConfig.java` | ✅ 완료 |
| STOMP 엔드포인트 | `/ws/notifications` + SockJS | ✅ 완료 |
| 메시지 푸시 | `NotificationPushService.java` | ✅ 완료 |
| Redis Pub/Sub | 다중 인스턴스 지원 | ✅ 완료 |

**WebSocket 설정:**
```java
// WebSocketConfig.java
registry.enableSimpleBroker("/topic", "/queue");
registry.setUserDestinationPrefix("/user");
registry.addEndpoint("/ws/notifications").withSockJS();
```

**푸시 서비스:**
```java
// NotificationPushService.java
messagingTemplate.convertAndSendToUser(
    notification.getUserId().toString(),
    "/queue/notifications",
    response
);
```

### 2.2 Frontend (❌ 미구현)

| 컴포넌트 | 파일 | 상태 |
|---------|------|------|
| WebSocket 클라이언트 | - | ❌ 없음 |
| STOMP 라이브러리 | - | ❌ 설치 필요 |
| 연결 관리 Composable | - | ❌ 없음 |
| Store 연동 | `notification.ts` | ⚠️ `addNotification()` 준비됨 |

---

## 3. 요구사항

### 3.1 기능 요구사항

| ID | 요구사항 | 우선순위 |
|----|---------|:--------:|
| FR-01 | WebSocket 연결 (STOMP + SockJS) | High |
| FR-02 | 사용자별 알림 구독 (`/user/{userId}/queue/notifications`) | High |
| FR-03 | 실시간 알림 수신 → Store 업데이트 | High |
| FR-04 | 연결 끊김 시 자동 재연결 | High |
| FR-05 | 폴링 제거 또는 fallback으로 전환 | Medium |
| FR-06 | 연결 상태 표시 (선택적) | Low |

### 3.2 비기능 요구사항

| ID | 요구사항 | 측정 기준 |
|----|---------|----------|
| NFR-01 | 알림 지연 시간 | < 2초 |
| NFR-02 | 재연결 시간 | < 5초 |
| NFR-03 | 메모리 누수 없음 | 컴포넌트 언마운트 시 연결 해제 |

---

## 4. 범위

### 4.1 포함 (In Scope)

- [x] STOMP/SockJS 라이브러리 설치
- [x] WebSocket 연결 composable 생성 (`useWebSocket.ts`)
- [x] 알림 구독 및 Store 연동
- [x] 자동 재연결 로직
- [x] 로그인/로그아웃 시 연결 관리
- [x] 기존 폴링 로직 제거

### 4.2 제외 (Out of Scope)

- 알림 설정 (알림 끄기/켜기)
- 알림 소리/진동
- 브라우저 Push Notification
- 오프라인 알림 큐잉

---

## 5. 기술 스택

### 5.1 필요 라이브러리

```json
// package.json 추가
{
  "dependencies": {
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1"
  },
  "devDependencies": {
    "@types/sockjs-client": "^1.5.4"
  }
}
```

### 5.2 WebSocket 엔드포인트

| 환경 | URL |
|------|-----|
| Local | `ws://localhost:8080/notification/ws/notifications` |
| Docker | `ws://api-gateway:8080/notification/ws/notifications` |
| K8s | `wss://portal-universe/notification/ws/notifications` |

### 5.3 STOMP 구독 경로

```
/user/{userId}/queue/notifications
```

---

## 6. 구현 전략

### 6.1 파일 구조

```
frontend/portal-shell/src/
├── composables/
│   └── useWebSocket.ts          # 신규: WebSocket 연결 관리
├── store/
│   └── notification.ts          # 수정: WebSocket 연동
├── App.vue                      # 수정: WebSocket 초기화
└── services/
    └── notificationService.ts   # 유지: REST API
```

### 6.2 연결 흐름

```
┌─────────────────────────────────────────────────────────────┐
│  App.vue onMounted                                          │
│    │                                                        │
│    ├─▶ authStore.isAuthenticated 확인                       │
│    │                                                        │
│    └─▶ useWebSocket().connect()                             │
│          │                                                  │
│          ├─▶ SockJS 연결 (HTTP → WebSocket Upgrade)         │
│          │                                                  │
│          ├─▶ STOMP 핸드셰이크                               │
│          │                                                  │
│          └─▶ /user/{userId}/queue/notifications 구독        │
│                │                                            │
│                └─▶ 메시지 수신 시 store.addNotification()   │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 재연결 전략

```
연결 끊김 감지
    │
    ├─▶ 5초 대기
    │
    ├─▶ 재연결 시도 (최대 5회)
    │
    ├─▶ 성공 시 구독 복구
    │
    └─▶ 실패 시 폴링 fallback (30초)
```

---

## 7. 구현 단계

### Phase 1: 라이브러리 설치 및 기본 연결

```
□ 1.1 @stomp/stompjs, sockjs-client 설치
□ 1.2 useWebSocket.ts composable 생성
□ 1.3 기본 STOMP 연결 구현
□ 1.4 연결 성공/실패 로깅
```

### Phase 2: 알림 구독 및 Store 연동

```
□ 2.1 /user/{userId}/queue/notifications 구독
□ 2.2 메시지 수신 시 JSON 파싱
□ 2.3 notificationStore.addNotification() 호출
□ 2.4 unreadCount 자동 증가 확인
```

### Phase 3: 연결 생명주기 관리

```
□ 3.1 App.vue에서 useWebSocket 초기화
□ 3.2 로그인 시 연결, 로그아웃 시 해제
□ 3.3 컴포넌트 언마운트 시 정리
□ 3.4 재연결 로직 구현
```

### Phase 4: 정리 및 테스트

```
□ 4.1 기존 폴링 로직 제거
□ 4.2 연결 상태 표시 (선택적)
□ 4.3 E2E 테스트 (알림 발생 → 실시간 표시)
□ 4.4 재연결 시나리오 테스트
```

---

## 8. 위험 요소

| 위험 | 영향 | 완화 전략 |
|------|-----|----------|
| API Gateway WebSocket 라우팅 | High | Gateway 설정 확인 필요 |
| CORS 이슈 | Medium | allowedOrigins 설정 확인 |
| 인증 토큰 전달 | Medium | SockJS 핸드셰이크 시 쿠키/헤더 |
| 브라우저 호환성 | Low | SockJS fallback 활용 |

---

## 9. 성공 기준

- [ ] WebSocket 연결 성공 (개발자 도구 Network 탭)
- [ ] 새 알림 발생 시 2초 이내 UI 반영
- [ ] 페이지 새로고침 없이 알림 수신
- [ ] 연결 끊김 후 자동 재연결
- [ ] 로그아웃 시 연결 해제

---

## 10. 예상 일정

| Phase | 작업 | 예상 소요 |
|-------|------|----------|
| Phase 1 | 라이브러리 설치 + 기본 연결 | 1시간 |
| Phase 2 | 알림 구독 + Store 연동 | 1시간 |
| Phase 3 | 생명주기 관리 | 1시간 |
| Phase 4 | 정리 + 테스트 | 1시간 |
| **Total** | | **~4시간** |

---

## Changelog

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-03 | 최초 작성 |
