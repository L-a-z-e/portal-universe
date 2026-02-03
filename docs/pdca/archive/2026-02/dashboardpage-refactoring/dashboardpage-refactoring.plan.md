# Plan: Dashboard Page Refactoring

> Feature: `dashboardpage-refactoring`
> Created: 2026-02-03
> Status: Draft

## 1. 개요

### 1.1 현재 상황
Dashboard 페이지(`portal-shell/src/views/DashboardPage.vue`)에 하드코딩된 mock 데이터가 표시되고 있음.

**현재 Mock 데이터:**
- Stats: 작성한 글 12, 주문 건수 5, 받은 좋아요 48 (모두 가짜)
- 최근 활동: 3개의 정적 mock 데이터

### 1.2 목표
각 마이크로서비스(blog, shopping, notification)에서 실제 데이터를 가져와 Dashboard에 표시

### 1.3 기대 효과
- 사용자에게 실제 활동 내역과 통계 제공
- 각 서비스로의 자연스러운 진입점 역할
- 플랫폼 engagement 향상

## 2. 현황 분석

### 2.1 현재 Dashboard 구조
```
┌─────────────────────────────────────────────────────────┐
│  Header: 인사말 + 사용자명 + 새 글 작성 버튼            │
├─────────────────────────────────────────────────────────┤
│  Stats Overview (3 cards)                               │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                   │
│  │작성한 글│ │주문 건수│ │받은좋아요│  ← Mock 데이터    │
│  │   12    │ │    5    │ │   48    │                   │
│  └─────────┘ └─────────┘ └─────────┘                   │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌────────────────────────────────┐  │
│  │ 빠른 작업    │  │ 최근 활동 ← Mock 데이터        │  │
│  │ - 새 글 작성 │  │ - Vue 3 Guide (2시간 전)       │  │
│  │ - 상품 둘러보│  │ - 상품 3개 주문 (1일 전)       │  │
│  │ - 주문 내역  │  │ - React vs Vue (2일 전)        │  │
│  └──────────────┘  └────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  서비스 그리드 (Blog, Shopping, Prism)                  │
└─────────────────────────────────────────────────────────┘
```

### 2.2 사용 가능한 백엔드 API

#### Blog Service (`/api/blog`)
| Endpoint | Response | 용도 |
|----------|----------|------|
| `GET /posts/stats/author/{authorId}` | `AuthorStats` | 작성자 통계 |
| `GET /posts/my` | `Page<PostResponse>` | 내 글 목록 |
| `GET /posts/recent` | `List<PostResponse>` | 최근 글 |

**AuthorStats 구조:**
```typescript
{
  authorId: string
  authorName: string
  totalPosts: number      // 작성한 글
  publishedPosts: number
  totalViews: number
  totalLikes: number      // 받은 좋아요
  firstPostDate: string
  lastPostDate: string
}
```

#### Shopping Service (`/api/shopping`)
| Endpoint | Response | 용도 |
|----------|----------|------|
| `GET /orders` | `Page<OrderResponse>` | 주문 목록 |
| `GET /coupons/my` | `List<CouponResponse>` | 내 쿠폰 |

#### Notification Service (`/api/notifications`)
| Endpoint | Response | 용도 |
|----------|----------|------|
| `GET /` | `List<NotificationResponse>` | 알림 목록 |
| WebSocket `/ws/notifications` | 실시간 알림 | 이미 구현됨 |

### 2.3 문제점
1. **데이터 소스 없음**: Stats와 최근 활동이 하드코딩
2. **API 통합 미비**: 백엔드 API가 존재하지만 프론트엔드에서 호출하지 않음
3. **로딩 상태 없음**: 비동기 데이터 로딩 UX 미고려
4. **에러 처리 없음**: API 실패 시 fallback 없음

## 3. 구현 계획

### 3.1 구현 범위

#### Phase 1: Dashboard Service 레이어 구축
- [ ] `dashboardService.ts` 생성 - API 호출 통합
- [ ] `useDashboard` composable 생성 - 상태 관리

#### Phase 2: Stats 섹션 실제 데이터 연동
- [ ] Blog 통계 API 연동 (totalPosts, totalLikes)
- [ ] Shopping 주문 수 API 연동
- [ ] 변화량(+N) 계산 로직 (선택적)

#### Phase 3: 최근 활동 섹션 구현
- [ ] Notification 목록 API 연동
- [ ] 또는 통합 활동 피드 구현 (blog + shopping)

#### Phase 4: UX 개선
- [ ] 스켈레톤 로딩 UI
- [ ] 에러 상태 처리
- [ ] Empty 상태 처리

### 3.2 데이터 매핑

| Dashboard 항목 | API Source | 필드 |
|---------------|------------|------|
| 작성한 글 | `GET /api/blog/posts/stats/author/{userId}` | `totalPosts` |
| 받은 좋아요 | `GET /api/blog/posts/stats/author/{userId}` | `totalLikes` |
| 주문 건수 | `GET /api/shopping/orders?size=0` | `page.totalElements` |
| 최근 활동 | `GET /api/notifications?size=5` | notifications |

### 3.3 파일 구조

```
frontend/portal-shell/src/
├── services/
│   └── dashboardService.ts      # NEW: Dashboard API 호출
├── composables/
│   └── useDashboard.ts          # NEW: Dashboard 상태 관리
├── types/
│   └── dashboard.ts             # NEW: Dashboard 타입 정의
└── views/
    └── DashboardPage.vue        # MODIFY: 실제 데이터 사용
```

### 3.4 API 호출 전략

```typescript
// 병렬 호출로 성능 최적화
const [blogStats, orders, notifications] = await Promise.allSettled([
  dashboardService.getBlogStats(userId),
  dashboardService.getOrderCount(userId),
  dashboardService.getRecentNotifications(userId, 5)
])
```

### 3.5 Fallback 전략
- API 실패 시 해당 섹션만 에러 표시 (전체 페이지 실패 X)
- 캐시된 이전 데이터 사용 (선택적)

## 4. 기술적 고려사항

### 4.1 인증
- 모든 API는 인증 필요
- `authStore.user.id`를 사용하여 사용자 ID 획득
- API Gateway 통해 라우팅

### 4.2 성능
- 병렬 API 호출 (`Promise.allSettled`)
- 적절한 캐싱 전략 (stale-while-revalidate 고려)
- 스켈레톤 로딩으로 체감 성능 향상

### 4.3 반응성
- Pinia store 또는 composable로 상태 관리
- 실시간 업데이트는 WebSocket 알림으로 이미 처리됨

## 5. 테스트 계획

### 5.1 단위 테스트
- `dashboardService` API 호출 테스트
- `useDashboard` composable 로직 테스트

### 5.2 E2E 테스트
- Dashboard 로딩 시 스켈레톤 표시 확인
- 실제 데이터 표시 확인
- API 에러 시 fallback UI 확인

## 6. 의존성

### 6.1 선행 조건
- [x] Blog Service `GET /posts/stats/author/{authorId}` API 존재
- [x] Shopping Service `GET /orders` API 존재
- [x] Notification Service API 존재
- [x] 사용자 인증 시스템 동작

### 6.2 관련 컴포넌트
- `DashboardPage.vue` - 메인 수정 대상
- `apiClient.ts` - API 호출 유틸리티
- `authStore.ts` - 사용자 정보

## 7. 일정 (예상)

| Phase | 작업 |
|-------|------|
| Phase 1 | Service/Composable 구축 |
| Phase 2 | Stats 섹션 연동 |
| Phase 3 | 최근 활동 연동 |
| Phase 4 | UX 개선 및 테스트 |

## 8. 위험 요소

| 위험 | 대응 |
|------|------|
| API 응답 지연 | 스켈레톤 UI + 타임아웃 |
| API 스키마 불일치 | 타입 정의로 조기 발견 |
| 인증 토큰 만료 | apiClient의 자동 갱신 활용 |

## 9. 참고 자료

- 현재 Dashboard 스크린샷: `.playwright-mcp/dashboard-current-state.png`
- Blog API: `services/blog-service/src/main/java/.../post/controller/PostController.java`
- Shopping API: `services/shopping-service/src/main/java/.../order/controller/OrderController.java`
