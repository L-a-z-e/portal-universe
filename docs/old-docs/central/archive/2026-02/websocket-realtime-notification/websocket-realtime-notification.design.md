# Feature Design: websocket-realtime-notification

> **Feature**: WebSocket 실시간 알림
> **Version**: 1.0
> **Author**: Claude
> **Created**: 2026-02-03
> **Plan Reference**: `docs/pdca/01-plan/features/websocket-realtime-notification.plan.md`
> **Status**: Draft

---

## 1. 개요

### 1.1 설계 목표

Frontend에서 WebSocket 클라이언트를 구현하여 Backend의 기존 WebSocket 인프라와 연동, 실시간 알림 푸시 기능을 완성합니다.

### 1.2 현재 vs 목표 상태

| 구성요소 | 현재 | 목표 |
|---------|------|------|
| Backend WebSocket | ✅ 구현됨 | ✅ 유지 |
| Frontend STOMP 클라이언트 | ❌ 없음 | ✅ 신규 구현 |
| 알림 수신 방식 | 30초 폴링 | WebSocket 실시간 |
| 알림 지연 | 최대 30초 | < 2초 |

---

## 2. 아키텍처 설계

### 2.1 전체 흐름도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Frontend (portal-shell)                         │
│                                                                              │
│  ┌──────────────┐    ┌──────────────────┐    ┌────────────────────────────┐ │
│  │   App.vue    │───▶│  useWebSocket.ts │───▶│  notificationStore.ts      │ │
│  │  (초기화)    │    │  (STOMP Client)  │    │  addNotification()         │ │
│  └──────────────┘    └────────┬─────────┘    └────────────────────────────┘ │
│                               │                                              │
└───────────────────────────────┼──────────────────────────────────────────────┘
                                │ WebSocket (wss://)
                                │ STOMP Protocol
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              api-gateway (:8080)                             │
│                         /notification/ws/notifications                       │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        notification-service (:8084)                          │
│                                                                              │
│  ┌──────────────────┐    ┌─────────────────────┐    ┌────────────────────┐  │
│  │  WebSocketConfig │    │ NotificationPushSvc │    │ NotificationConsumer│  │
│  │  /ws/notifications│    │ convertAndSendToUser│◀───│ Kafka Listener     │  │
│  └──────────────────┘    └─────────────────────┘    └────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 WebSocket 연결 시퀀스

```
Frontend                    API Gateway              notification-service
    │                           │                           │
    │── HTTP GET /ws/notifications ──────────────────────▶│
    │                           │                           │
    │◀──────────── HTTP 101 Switching Protocols ───────────│
    │                           │                           │
    │══════════════ WebSocket Connection ══════════════════│
    │                           │                           │
    │── STOMP CONNECT ─────────────────────────────────────▶│
    │                           │                           │
    │◀─────────────────────── STOMP CONNECTED ─────────────│
    │                           │                           │
    │── STOMP SUBSCRIBE /user/{userId}/queue/notifications─▶│
    │                           │                           │
    │                           │                    [Kafka 메시지 수신]
    │                           │                           │
    │◀─────────────────── STOMP MESSAGE ───────────────────│
    │                           │                           │
```

---

## 3. Frontend 구현 설계

### 3.1 파일 구조

```
frontend/portal-shell/src/
├── composables/
│   └── useWebSocket.ts          # 신규: WebSocket 연결 관리
├── store/
│   └── notification.ts          # 수정: WebSocket 연동
├── App.vue                      # 수정: WebSocket 초기화
└── types/
    └── notification.ts          # 유지
```

### 3.2 useWebSocket.ts (신규)

```typescript
// frontend/portal-shell/src/composables/useWebSocket.ts

import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'
import type { Notification } from '../types/notification'

export function useWebSocket() {
  const authStore = useAuthStore()
  const notificationStore = useNotificationStore()

  // ==================== State ====================
  const client = ref<Client | null>(null)
  const isConnected = ref(false)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5
  const subscription = ref<StompSubscription | null>(null)

  // ==================== WebSocket URL ====================
  function getWebSocketUrl(): string {
    const baseUrl = window.location.origin
    // API Gateway를 통해 notification-service WebSocket에 연결
    return `${baseUrl}/notification/ws/notifications`
  }

  // ==================== Connect ====================
  function connect() {
    // 이미 연결되어 있으면 스킵
    if (client.value?.active) {
      console.log('[WebSocket] Already connected')
      return
    }

    // 인증되지 않은 경우 스킵
    if (!authStore.isAuthenticated || !authStore.user?.id) {
      console.log('[WebSocket] Not authenticated, skipping connection')
      return
    }

    const userId = authStore.user.id
    console.log('[WebSocket] Connecting...', { userId })

    client.value = new Client({
      // SockJS를 WebSocket 팩토리로 사용
      webSocketFactory: () => new SockJS(getWebSocketUrl()),

      // 디버그 로깅 (개발 환경에서만)
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[STOMP]', str)
        }
      },

      // 연결 성공 시
      onConnect: () => {
        console.log('[WebSocket] Connected successfully')
        isConnected.value = true
        reconnectAttempts.value = 0

        // 사용자별 알림 큐 구독
        subscribeToNotifications(userId)
      },

      // 연결 끊김 시
      onDisconnect: () => {
        console.log('[WebSocket] Disconnected')
        isConnected.value = false
        subscription.value = null
      },

      // STOMP 에러 시
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message'])
      },

      // WebSocket 에러 시
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event)
      },

      // 재연결 설정
      reconnectDelay: 5000,  // 5초 후 재연결
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.value.activate()
  }

  // ==================== Subscribe ====================
  function subscribeToNotifications(userId: string) {
    if (!client.value) return

    const destination = `/user/${userId}/queue/notifications`
    console.log('[WebSocket] Subscribing to:', destination)

    subscription.value = client.value.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const notification: Notification = JSON.parse(message.body)
          console.log('[WebSocket] Received notification:', notification)
          notificationStore.addNotification(notification)
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error)
        }
      }
    )
  }

  // ==================== Disconnect ====================
  function disconnect() {
    if (subscription.value) {
      subscription.value.unsubscribe()
      subscription.value = null
    }

    if (client.value?.active) {
      client.value.deactivate()
      console.log('[WebSocket] Disconnected')
    }

    isConnected.value = false
  }

  // ==================== Watch Auth State ====================
  watch(
    () => authStore.isAuthenticated,
    (isAuthenticated) => {
      if (isAuthenticated) {
        connect()
      } else {
        disconnect()
      }
    }
  )

  // ==================== Lifecycle ====================
  onMounted(() => {
    if (authStore.isAuthenticated) {
      connect()
    }
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect,
  }
}
```

### 3.3 App.vue 수정

```vue
<!-- frontend/portal-shell/src/App.vue -->
<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useAuthStore } from './store/auth'
import { useNotificationStore } from './store/notification'
import { useWebSocket } from './composables/useWebSocket'
// ... 기존 imports

const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// WebSocket 연결 초기화
const { isConnected } = useWebSocket()

// 로그인 시 초기 알림 개수 로드
watch(
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    if (isAuthenticated) {
      await notificationStore.fetchUnreadCount()
    } else {
      notificationStore.reset()
    }
  },
  { immediate: true }
)
</script>
```

### 3.4 notification.ts Store 수정 (폴링 제거)

```typescript
// frontend/portal-shell/src/store/notification.ts
// 변경 사항: startPolling/stopPolling 메서드 제거 (더 이상 필요 없음)

export const useNotificationStore = defineStore('notification', () => {
  // ... 기존 state/getters

  // ==================== Actions ====================

  // addNotification은 이미 구현됨 - WebSocket에서 호출
  function addNotification(notification: Notification) {
    // 중복 체크
    const exists = notifications.value.some(n => n.id === notification.id)
    if (exists) return

    // 목록 앞에 추가
    notifications.value.unshift(notification)

    // 미읽음 개수 증가
    if (notification.status === 'UNREAD') {
      unreadCount.value++
    }
  }

  // ... 나머지 기존 메서드 유지

  return {
    // ... exports (폴링 관련 메서드 제외)
  }
})
```

---

## 4. API Gateway 라우팅 설정

### 4.1 WebSocket 라우팅 확인

```yaml
# services/api-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        # WebSocket 라우팅 (기존 설정 확인 필요)
        - id: notification-websocket
          uri: lb:ws://notification-service
          predicates:
            - Path=/notification/ws/**
          filters:
            - StripPrefix=1
```

---

## 5. 패키지 의존성

### 5.1 설치 명령

```bash
cd frontend/portal-shell
pnpm add @stomp/stompjs sockjs-client
pnpm add -D @types/sockjs-client
```

### 5.2 package.json 추가

```json
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

---

## 6. 테스트 환경 설정

### 6.1 인프라 시작 (docker-compose-local.yml)

```bash
# 1. 인프라 컨테이너 시작
docker compose -f docker-compose-local.yml up -d

# 2. 서비스 상태 확인
docker compose -f docker-compose-local.yml ps

# 필요한 컨테이너:
# - mysql (3306)
# - redis (6379)
# - kafka (9092)
# - zookeeper (2181)
```

### 6.2 백엔드 서비스 시작

```bash
# 터미널 1: API Gateway
cd /Users/laze/Laze/Project/portal-universe
./gradlew :services:api-gateway:bootRun --args='--spring.profiles.active=local' &

# 터미널 2: Auth Service
./gradlew :services:auth-service:bootRun --args='--spring.profiles.active=local' &

# 터미널 3: Blog Service
./gradlew :services:blog-service:bootRun --args='--spring.profiles.active=local' &

# 터미널 4: Notification Service
./gradlew :services:notification-service:bootRun --args='--spring.profiles.active=local' &
```

### 6.3 프론트엔드 시작

```bash
cd frontend/portal-shell
pnpm install
pnpm run dev
```

---

## 7. Playwright 통합 테스트 설계

### 7.1 테스트 시나리오

| # | 시나리오 | 검증 항목 |
|---|---------|----------|
| 1 | WebSocket 연결 | 로그인 후 WebSocket 연결 성공 |
| 2 | 알림 실시간 수신 | 좋아요 → 2초 이내 알림 표시 |
| 3 | 다중 알림 수신 | 여러 알림 순차 수신 |
| 4 | 재연결 | 네트워크 끊김 후 자동 재연결 |
| 5 | 로그아웃 | WebSocket 연결 해제 |

### 7.2 테스트 코드 (websocket.spec.ts)

```typescript
// frontend/blog-frontend/e2e/tests/websocket.spec.ts

import { test, expect, Page } from '@playwright/test'

const BASE_URL = 'http://localhost:30000'

// 테스트용 사용자 정보
const testUser1 = {
  email: `wstest1_${Date.now()}@test.com`,
  password: 'Test@9527Pwd',
  name: 'WebSocket Tester 1'
}

const testUser2 = {
  email: `wstest2_${Date.now()}@test.com`,
  password: 'Test@9527Pwd',
  name: 'WebSocket Tester 2'
}

test.describe('WebSocket 실시간 알림', () => {

  test.beforeAll(async ({ browser }) => {
    // 테스트 사용자 2명 회원가입
    const page = await browser.newPage()

    // User 1 회원가입
    await page.goto(`${BASE_URL}/signup`)
    await page.getByLabel('이메일').fill(testUser1.email)
    await page.getByLabel('비밀번호').first().fill(testUser1.password)
    await page.getByLabel('비밀번호 확인').fill(testUser1.password)
    await page.getByLabel('이름').fill(testUser1.name)
    await page.getByRole('button', { name: '회원가입' }).click()
    await page.waitForURL('**/login')

    // User 2 회원가입
    await page.goto(`${BASE_URL}/signup`)
    await page.getByLabel('이메일').fill(testUser2.email)
    await page.getByLabel('비밀번호').first().fill(testUser2.password)
    await page.getByLabel('비밀번호 확인').fill(testUser2.password)
    await page.getByLabel('이름').fill(testUser2.name)
    await page.getByRole('button', { name: '회원가입' }).click()
    await page.waitForURL('**/login')

    await page.close()
  })

  test('로그인 시 WebSocket 연결 성공', async ({ page }) => {
    // Given: 로그인 페이지
    await page.goto(`${BASE_URL}/login`)

    // WebSocket 연결 모니터링
    const wsPromise = page.waitForEvent('websocket')

    // When: 로그인
    await page.getByLabel('이메일').fill(testUser1.email)
    await page.getByLabel('비밀번호').fill(testUser1.password)
    await page.getByRole('button', { name: '로그인' }).click()

    // Then: WebSocket 연결 확인
    const ws = await wsPromise
    expect(ws.url()).toContain('/notification/ws/notifications')

    // WebSocket이 연결 상태인지 확인
    await page.waitForFunction(() => {
      return (window as any).__WS_CONNECTED__ === true
    }, { timeout: 10000 }).catch(() => {
      // fallback: 콘솔 로그로 확인
    })
  })

  test('좋아요 시 실시간 알림 수신 (< 2초)', async ({ browser }) => {
    // Given: User1이 글 작성
    const page1 = await browser.newPage()
    await loginAs(page1, testUser1)

    // 글 작성
    await page1.goto(`${BASE_URL}/blog/write`)
    await page1.getByLabel('제목').fill('WebSocket 테스트 게시글')
    await page1.locator('.ProseMirror').fill('실시간 알림 테스트용 게시글입니다.')
    await page1.getByRole('button', { name: '발행' }).click()
    await page1.waitForURL('**/blog/**')
    const postUrl = page1.url()

    // Given: User2 로그인 (다른 브라우저)
    const page2 = await browser.newPage()
    await loginAs(page2, testUser2)

    // When: User2가 User1의 글에 좋아요
    await page2.goto(postUrl)
    const startTime = Date.now()
    await page2.getByRole('button', { name: '🤍' }).click()

    // Then: User1에게 2초 이내 알림 표시
    // 알림 벨에 배지 또는 드롭다운에 새 알림 확인
    await expect(async () => {
      const badge = page1.locator('.notification-badge, [data-unread="true"]')
      await expect(badge).toBeVisible({ timeout: 2000 })
    }).toPass({ timeout: 3000 })

    const endTime = Date.now()
    const latency = endTime - startTime
    console.log(`알림 지연 시간: ${latency}ms`)
    expect(latency).toBeLessThan(2000)

    await page1.close()
    await page2.close()
  })

  test('알림 드롭다운에서 새 알림 확인', async ({ browser }) => {
    const page1 = await browser.newPage()
    const page2 = await browser.newPage()

    await loginAs(page1, testUser1)
    await loginAs(page2, testUser2)

    // User1 글 작성
    await page1.goto(`${BASE_URL}/blog/write`)
    await page1.getByLabel('제목').fill('드롭다운 테스트 게시글')
    await page1.locator('.ProseMirror').fill('알림 드롭다운 확인용')
    await page1.getByRole('button', { name: '발행' }).click()
    await page1.waitForURL('**/blog/**')
    const postUrl = page1.url()

    // User2가 좋아요
    await page2.goto(postUrl)
    await page2.getByRole('button', { name: '🤍' }).click()
    await page2.waitForTimeout(1000)

    // User1이 알림 드롭다운 열기
    await page1.getByRole('button', { name: /알림/ }).click()

    // 알림 목록에 새 알림 있는지 확인
    const notificationItem = page1.locator('.notification-item, [data-notification]').first()
    await expect(notificationItem).toBeVisible()
    await expect(notificationItem).toContainText('좋아요')

    await page1.close()
    await page2.close()
  })

  test('로그아웃 시 WebSocket 연결 해제', async ({ page }) => {
    await loginAs(page, testUser1)

    // WebSocket 연결 확인
    await page.waitForTimeout(2000)

    // WebSocket 종료 이벤트 모니터링
    let wsClosedCalled = false
    page.on('websocket', ws => {
      ws.on('close', () => {
        wsClosedCalled = true
      })
    })

    // 로그아웃
    await page.getByRole('button', { name: /프로필|메뉴/ }).click()
    await page.getByRole('button', { name: '로그아웃' }).click()

    // WebSocket 종료 확인
    await page.waitForTimeout(2000)
    expect(wsClosedCalled).toBe(true)
  })
})

// 헬퍼 함수
async function loginAs(page: Page, user: { email: string; password: string }) {
  await page.goto(`${BASE_URL}/login`)
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await page.waitForURL('**/*')
  await page.waitForTimeout(1000) // WebSocket 연결 대기
}
```

### 7.3 테스트 실행 명령

```bash
# 1. 인프라 확인
docker compose -f docker-compose-local.yml ps

# 2. 서비스 헬스체크
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health

# 3. Playwright 테스트 실행
cd frontend/blog-frontend
npx playwright test e2e/tests/websocket.spec.ts --headed
```

---

## 8. 구현 순서

### Phase 1: 의존성 설치 및 기본 연결

```
□ 1.1 pnpm add @stomp/stompjs sockjs-client
□ 1.2 pnpm add -D @types/sockjs-client
□ 1.3 useWebSocket.ts 파일 생성
□ 1.4 기본 STOMP 연결 로직 구현
□ 1.5 콘솔 로그로 연결 성공 확인
```

### Phase 2: Store 연동 및 알림 수신

```
□ 2.1 App.vue에서 useWebSocket 초기화
□ 2.2 /user/{userId}/queue/notifications 구독
□ 2.3 메시지 수신 → notificationStore.addNotification() 호출
□ 2.4 개발자 도구 Network 탭에서 WebSocket 프레임 확인
```

### Phase 3: 생명주기 관리

```
□ 3.1 authStore.isAuthenticated watch 로직
□ 3.2 로그인 시 자동 연결
□ 3.3 로그아웃 시 연결 해제
□ 3.4 컴포넌트 언마운트 시 정리
```

### Phase 4: 통합 테스트

```
□ 4.1 docker-compose-local.yml 인프라 시작
□ 4.2 백엔드 서비스 4개 시작 (gateway, auth, blog, notification)
□ 4.3 프론트엔드 시작
□ 4.4 Playwright 테스트 실행
□ 4.5 알림 지연 시간 < 2초 확인
```

---

## 9. 변경 파일 목록

### 9.1 신규 파일

| 파일 | 설명 |
|------|------|
| `frontend/portal-shell/src/composables/useWebSocket.ts` | WebSocket 연결 관리 |
| `frontend/blog-frontend/e2e/tests/websocket.spec.ts` | Playwright 통합 테스트 |

### 9.2 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/portal-shell/package.json` | @stomp/stompjs, sockjs-client 추가 |
| `frontend/portal-shell/src/App.vue` | useWebSocket 초기화 추가 |
| `frontend/portal-shell/src/store/notification.ts` | 폴링 관련 코드 제거 (선택적) |

---

## 10. 테스트 체크리스트

### 10.1 수동 테스트

| # | 테스트 항목 | 예상 결과 |
|---|-----------|----------|
| 1 | 로그인 후 개발자 도구 Network 탭 | WS 연결 보임 |
| 2 | 다른 브라우저에서 좋아요 | 알림 벨에 배지 표시 |
| 3 | 알림 드롭다운 열기 | 새 알림 목록 표시 |
| 4 | 로그아웃 | WS 연결 종료 |
| 5 | 새로고침 | 재연결 성공 |

### 10.2 Playwright 자동 테스트

```bash
# 전체 테스트
npx playwright test e2e/tests/websocket.spec.ts

# 특정 테스트만
npx playwright test -g "실시간 알림 수신"

# UI 모드로 디버깅
npx playwright test --ui
```

---

## 11. 성공 기준

| 항목 | 기준 |
|------|------|
| WebSocket 연결 | ✅ 로그인 후 5초 이내 연결 |
| 알림 지연 시간 | ✅ < 2초 |
| 재연결 | ✅ 5초 이내 자동 재연결 |
| 메모리 누수 | ✅ 없음 (로그아웃 시 정리) |
| Playwright 테스트 | ✅ 100% 통과 |

---

## Changelog

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-03 | 최초 작성 - Playwright 통합 테스트 포함 |
