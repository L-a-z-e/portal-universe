# 리팩토링 진행 로그

> **이 파일을 보고 다음 세션에서 이어서 진행하세요**

---

## 🚀 빠른 재개 명령어

```
다음 세션 시작할 때 이렇게 말하세요:

"리팩토링 이어서 하자. PROGRESS-LOG.md 확인해줘"
```

---

## 📍 현재 상태 (2026-02-04)

### 전체 로드맵 위치

```
Week 1 (Phase 0): ✅ 완료
  └─ 브랜치, 테스트 기준선, 분석 완료

Week 2~: ⏸️ 일시 중단
  └─ Bootstrap 리팩토링 먼저 진행 중 (사용자 요청)
```

### Bootstrap 리팩토링 진행률

```
[██████░░░░] 60%

✅ 완료:
  - frontend/react-bootstrap/package.json
  - frontend/react-bootstrap/src/types.ts
  - frontend/react-bootstrap/src/createAppBootstrap.tsx

⏳ 남은 작업:
  - frontend/react-bootstrap/src/index.ts (export 파일)
  - shopping-frontend/src/bootstrap.tsx 수정
  - prism-frontend/src/bootstrap.tsx 수정
  - 테스트 및 검증
```

### Clean Code 학습 진행률

```
[████████░░] 80% (createAppBootstrap.tsx 코드 리뷰)

✅ 학습 완료:
  - import / import type 차이
  - React vs ReactDOM 차이
  - @portal 네임스페이스
  - WeakMap
  - 구조 분해 할당 (const { a, b } = obj)
  - 화살표 함수 () => () vs () => {}
  - Props 개념
  - 렌더링 함수 & 스프레드 문법
  - window as any
  - MutationObserver 전체

⏳ 남은 학습:
  - createAppInstance 함수 내부
  - cleanupInstance 함수 내부
  - 실제 적용 시 발생하는 이슈들
```

---

## 📂 생성된 파일 목록

### 이번 세션에서 생성

| 파일 | 상태 | 설명 |
|------|------|------|
| `docs/refactoring/BRANCH-STRATEGY.md` | ✅ | 브랜치 전략 |
| `docs/refactoring/BASELINE-REPORT.md` | ✅ | 테스트 커버리지 기준선 |
| `docs/refactoring/BOOTSTRAP-ANALYSIS.md` | ✅ | Bootstrap 중복 분석 |
| `docs/refactoring/PROGRESS-LOG.md` | ✅ | 이 파일 (진행 로그) |
| `frontend/react-bootstrap/package.json` | ✅ | 패키지 설정 |
| `frontend/react-bootstrap/src/types.ts` | ✅ | 타입 정의 |
| `frontend/react-bootstrap/src/createAppBootstrap.tsx` | ✅ | 팩토리 함수 |

### 다음에 생성/수정할 파일

| 파일 | 작업 |
|------|------|
| `frontend/react-bootstrap/src/index.ts` | 생성 (export) |
| `frontend/shopping-frontend/src/bootstrap.tsx` | 수정 (287줄 → ~15줄) |
| `frontend/prism-frontend/src/bootstrap.tsx` | 수정 (235줄 → ~15줄) |

---

## 🎯 다음 세션 TODO

### 1. 코드 작업 (20분)

```
1. index.ts 생성 (export 파일)
2. shopping-frontend/bootstrap.tsx 수정
3. prism-frontend/bootstrap.tsx 수정
```

### 2. 학습 (선택)

```
- createAppInstance 내부 코드 리뷰
- cleanupInstance 내부 코드 리뷰
```

### 3. 테스트 (10분)

```
- npm install (의존성 설치)
- 빌드 확인
- 기존 E2E 테스트 통과 확인
```

---

## 📚 학습 노트 (나중에 복습용)

### TypeScript 핵심 문법

| 문법 | 의미 | 예시 |
|------|------|------|
| `import type` | 타입만 가져옴 (컴파일 시 삭제) | `import type { User } from './types'` |
| `as any` | 타입 검사 무시 | `(window as any).foo = 1` |
| `() => ({})` | 객체를 바로 반환하는 화살표 함수 | `const fn = () => ({ name: 'Kim' })` |
| `{ a, b } = obj` | 구조 분해 할당 | `const { name, age } = user` |
| `...obj` | 스프레드 (펼치기) | `<App {...props} />` |
| `?.` | 옵셔널 체이닝 (null이면 멈춤) | `state?.isActive` |

### React 핵심 개념

| 개념 | 설명 |
|------|------|
| Props | 부모 → 자식으로 전달하는 값 |
| render() | 컴포넌트를 DOM에 그리기 |
| StrictMode | 개발 중 실수 찾아주는 검사기 |

### 브라우저 API

| API | 설명 |
|-----|------|
| `window` | 브라우저 전역 객체 |
| `document` | HTML 문서 전체 |
| `MutationObserver` | DOM 변화 감시자 |

---

## 🔖 현재 Git 브랜치

```
refactor/phase0-setup
```

작업 완료 후 커밋하고 PR 생성 예정.

---

## 📞 도움말

### 진행 방식

1. **학습 + 코딩 병행**: 코드 만들면서 문법 설명
2. **질문 환영**: 모르는 거 바로바로 물어보기
3. **짧게 끊기**: 한 번에 너무 많이 안 보기

### 속도 조절

- 빠르게: "설명 생략하고 코드만"
- 천천히: "이 부분 더 자세히"
