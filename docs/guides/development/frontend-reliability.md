# Frontend Reliability Patterns

> 프론트엔드 앱에서 적용된 안정성 패턴 정리

- author: Laze
- created: 2026-03-01
- updated: 2026-03-01

## 1. Route Guards (RequireAuth)

인증이 필요한 라우트에 `RequireAuth` 컴포넌트를 감싸서 비인증 접근을 차단한다.

```tsx
import { RequireAuth } from '@portal/react-bridge';

// router 정의에서
{
  path: 'cart',
  element: (
    <RequireAuth>
      <CartPage />
    </RequireAuth>
  )
}
```

**동작**:
- Embedded 모드: Portal Shell 로그인 모달 트리거 (1회만)
- Standalone 모드: `redirectTo` 경로로 Navigate

**적용 대상** (shopping-frontend):
- cart, checkout, orders, orders/:orderNumber, coupons, time-deals/purchases, queue

**비적용** (공개 페이지):
- products (목록/상세), time-deals (목록/상세)

## 2. Unsaved Changes Protection (beforeunload + Draft)

편집 페이지에서 실수로 이탈 시 데이터 유실을 방지한다.

### beforeunload

```ts
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value && !isSaved.value) {
    e.preventDefault();
  }
}

window.addEventListener('beforeunload', handleBeforeUnload);
```

### localStorage Draft Auto-save

```ts
const DRAFT_KEY = `post-edit-draft-${postId}`;

// 30초 주기 자동 저장
setInterval(() => {
  if (isDirty && editor) saveDraft();
}, 30_000);

// 페이지 진입 시 복원 제안
const draft = loadDraft();
if (draft) {
  const useDraft = confirm('이전에 저장되지 않은 수정 내용이 있습니다. 복원하시겠습니까?');
}
```

**적용**: blog-frontend PostEditPage

## 3. SSE Connection State

실시간 연결 상태를 UI에 표시하여 사용자가 연결 끊김을 인지할 수 있게 한다.

```ts
type SseConnectionState = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'failed';

const { connectionState, reconnect } = useSse({ boardId, onEvent, enabled });
```

**상태 전이**: idle → connecting → connected ↔ disconnected → failed (MAX_RECONNECT_ATTEMPTS 초과)

**UI 표시** (BoardPage):
- `failed` 상태일 때 "Real-time updates disconnected" 배너 + Reconnect 버튼
- Reconnect 클릭 시 재연결 카운터 리셋

**적용**: prism-frontend useSse + BoardPage

## 4. Chat Error State

API 호출 실패 시 사용자에게 에러를 표시한다.

```ts
const { error, sendMessageStream } = useChat();
// error: Ref<string | null>
```

**동작**:
- `sendMessage` / `sendMessageStream` 실패 시 `error.value` 설정
- 스트리밍 실패 시 빈 assistant placeholder 메시지 자동 제거
- 새 메시지 전송 시 `error` 초기화

**적용**: portal-shell useChat + ChatPanel
