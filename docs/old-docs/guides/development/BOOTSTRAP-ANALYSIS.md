# Bootstrap 코드 분석 및 중복 패턴 문서

> **분석일**: 2026-02-04
> **관련 플랜**: `.claude/plans/enumerated-purring-volcano.md`
> **Clean Code 참조**: Chapter 1-2 (클린 코드, 의미 있는 이름)

## 1. 분석 대상

| 파일 | 줄 수 | 역할 |
|------|------|------|
| `frontend/shopping-frontend/src/bootstrap.tsx` | 287줄 | Shopping 앱 마운트/언마운트 |
| `frontend/prism-frontend/src/bootstrap.tsx` | 235줄 | Prism 앱 마운트/언마운트 |

## 2. 중복 분석 결과

### 2.1 동일한 부분 (98%)

```typescript
// 1. 타입 정의 패턴 (동일)
export type MountOptions = {
  initialPath?: string
  onNavigate?: (path: string) => void
  theme?: 'light' | 'dark'
}

// 2. 인스턴스 레지스트리 (동일)
const instanceRegistry = new WeakMap<HTMLElement, {
  root: ReactDOM.Root
  navigateCallback: ((path: string) => void) | null
  styleObserver: MutationObserver | null
  isActive: boolean
  currentTheme: 'light' | 'dark'
  rerender: () => void
}>()

// 3. 마운트 로직 (99% 동일)
- Portal Shell 플래그 설정
- 기존 인스턴스 정리
- React Root 생성
- MutationObserver 설정
- 렌더링
- 인스턴스 반환

// 4. 인스턴스 메서드 (동일)
- onParentNavigate
- onActivated
- onDeactivated
- onThemeChange
- unmount
```

### 2.2 다른 부분 (2%)

| 항목 | Shopping | Prism |
|------|----------|-------|
| 앱 이름 | `Shopping` | `Prism` |
| 타입명 | `ShoppingAppInstance` | `PrismAppInstance` |
| 함수명 | `mountShoppingApp` | `mountPrismApp` |
| data-service | `"shopping"` | `"prism"` |
| data-mf-app | `"shopping"` | `"prism"` |
| CSS 경로 | `./styles/index.css` | `./index.css` |
| 로그 접두사 | `[Shopping]` | `[Prism]` |

## 3. Clean Code 위배 사항

### 3.1 DRY 원칙 위배

> "모든 지식은 시스템 내에서 단일하고 명확한 표현을 가져야 한다"

**현재 상태**:
- 521줄의 코드 중 510줄 이상이 동일
- 변경 시 두 곳 모두 수정 필요
- 불일치 가능성 높음

### 3.2 이름 안티패턴

| 현재 이름 | 문제점 | 개선안 |
|----------|--------|--------|
| `instanceRegistry` | 무엇을 등록하는지 불명확 | `mountedAppStates` |
| `navigateCallback` | 콜백의 목적 불명확 | `parentNavigationHandler` |
| `rerender` | 왜 재렌더링하는지 불명확 | `updateWithCurrentProps` |

### 3.3 매직 스트링

```typescript
// 현재 (하드코딩)
document.documentElement.setAttribute('data-service', 'shopping');
(node as HTMLStyleElement).setAttribute('data-mf-app', 'shopping');

// 개선 (상수화)
const APP_CONFIG = { name: 'shopping', dataService: 'shopping' };
document.documentElement.setAttribute('data-service', APP_CONFIG.dataService);
```

## 4. 리팩토링 설계

### 4.1 목표 구조

```
frontend/react-bootstrap/          # 새 공통 패키지
├── src/
│   ├── index.ts                   # 공개 API
│   ├── types.ts                   # 공통 타입
│   ├── createAppBootstrap.tsx     # 팩토리 함수
│   └── utils/
│       ├── instanceManager.ts     # 인스턴스 관리
│       └── styleObserver.ts       # 스타일 관찰자
├── package.json
└── README.md
```

### 4.2 사용 예시 (리팩토링 후)

```typescript
// frontend/shopping-frontend/src/bootstrap.tsx (리팩토링 후)
import { createAppBootstrap } from '@portal/react-bootstrap';
import App from './App';
import './styles/index.css';

export const { mount: mountShoppingApp, unmount } = createAppBootstrap({
  name: 'shopping',
  App,
  dataService: 'shopping'
});

export default { mountShoppingApp };
```

**예상 줄 수**: 287줄 → 15줄 (95% 감소)

### 4.3 공통 패키지 인터페이스

```typescript
// @portal/react-bootstrap/types.ts

export interface AppBootstrapConfig {
  /** 앱 이름 (로깅용) */
  name: string;

  /** React App 컴포넌트 */
  App: React.ComponentType<AppProps>;

  /** data-service 속성 값 */
  dataService: string;

  /** 선택: 라우터 함수 */
  router?: {
    navigateTo: (path: string) => void;
    resetRouter: () => void;
    setAppActive: (active: boolean) => void;
  };
}

export interface MountOptions {
  initialPath?: string;
  onNavigate?: (path: string) => void;
  theme?: 'light' | 'dark';
}

export interface AppInstance {
  onParentNavigate: (path: string) => void;
  unmount: () => void;
  onActivated?: () => void;
  onDeactivated?: () => void;
  onThemeChange?: (theme: 'light' | 'dark') => void;
}
```

## 5. 구현 우선순위

| 순서 | 작업 | 난이도 | 효과 |
|------|------|--------|------|
| 1 | 타입 정의 추출 | 낮음 | 타입 재사용 |
| 2 | 팩토리 함수 구현 | 중간 | 코드 75% 감소 |
| 3 | 유틸리티 분리 | 낮음 | 테스트 용이 |
| 4 | Shopping 적용 | 낮음 | 첫 번째 검증 |
| 5 | Prism 적용 | 낮음 | 두 번째 검증 |

## 6. 테스트 계획

### 6.1 단위 테스트 (새 패키지)

```typescript
describe('createAppBootstrap', () => {
  it('should create mount function', () => {});
  it('should handle theme changes', () => {});
  it('should cleanup on unmount', () => {});
});
```

### 6.2 통합 테스트

- 기존 E2E 테스트가 모두 통과해야 함
- Shopping: `npm run test:shopping`
- Prism: `npm run test:prism`

## 7. 리스크 및 완화

| 리스크 | 확률 | 완화 전략 |
|--------|------|----------|
| Module Federation 호환성 | 중 | 점진적 적용, 롤백 계획 |
| 라우터 동기화 문제 | 낮 | 기존 함수 재사용 |
| 스타일 충돌 | 낮 | MutationObserver 유지 |

---

## 참고: Clean Code 원칙 적용

### Chapter 1: 클린 코드
- ✅ "클린 코드는 한 가지를 제대로 한다" → 공통 패키지가 마운트/언마운트만 담당

### Chapter 2: 의미 있는 이름
- ✅ `createAppBootstrap` - 무엇을 하는지 명확
- ✅ `AppBootstrapConfig` - 설정 객체임을 알 수 있음
- ✅ `mountedAppStates` - 상태 저장소임을 알 수 있음
