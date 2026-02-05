---
id: TS-20260119-002
title: Design System CSS Import 오류
type: troubleshooting
status: resolved
created: 2026-01-19
updated: 2026-01-19
author: Laze
severity: high
resolved: true
affected_services: [portal-shell, blog-frontend, shopping-frontend]
tags: [vite, design-system, css, alias, module-federation]
---

# Design System CSS Import 오류

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-19 |
| **해결일** | 2026-01-19 |
| **영향 서비스** | portal-shell, blog-frontend, shopping-frontend |

## 증상 (Symptoms)

### 현상
- `npm run dev:portal` 실행 시 빌드 실패
- CSS 파일 resolve 실패로 인해 앱이 시작되지 않음
- blog-frontend, shopping-frontend도 동일한 문제 발생

### 에러 메시지
```
[vite]: Rollup failed to resolve import "@portal/design-system-vue/style.css" from
"/Users/laze/Laze/Project/portal-universe/frontend/portal-shell/src/main.ts".
This is most likely unintended because it can break your application at runtime.
If you do want to externalize this module explicitly add it to
`build.rollupOptions.external`
Unable to resolve `@import "@portal/design-system-vue/style.css"` from
/Users/laze/Laze/Project/portal-universe/frontend/portal-shell/src
```

### 모니터링 지표
- 빌드 실패율: 100% (portal, blog, shopping 모두)
- 개발 서버 시작 불가

## 원인 분석 (Root Cause)

### 초기 추정
- design-system-vue 패키지의 exports 설정 오류
- npm workspaces 심볼릭 링크 문제

### 실제 원인
**Vite alias 설정과 실제 import 경로 불일치**

리팩토링 과정에서 design-system이 `design-system-vue`로 이름이 변경되었으나,
각 앱의 vite.config.ts에서 alias 설정이 업데이트되지 않음.

| 구분 | 경로 |
|------|------|
| **실제 import** | `@portal/design-system-vue/style.css` |
| **alias 설정** | `@portal/design-system/style.css` |

### 분석 과정
1. 에러 메시지에서 import 경로 확인: `@portal/design-system-vue/style.css`
2. vite.config.ts의 alias 설정 확인: `@portal/design-system/style.css`로 설정됨
3. 두 경로가 불일치하여 alias가 매칭되지 않고, package exports resolve 시도
4. 빌드 시점에 node_modules 심볼릭 링크로 인해 resolve 실패

## 해결 방법 (Solution)

### 즉시 조치 (Immediate Fix)
```bash
# 브랜치 생성
git checkout -b fix/design-system-import-alias

# design-system-vue 빌드 (dist 폴더 생성)
cd frontend
npm run build -w design-system-vue
```

### 영구 조치 (Permanent Fix)

3개 파일의 vite.config.ts에서 alias 경로 수정:

```typescript
// AS-IS
'@portal/design-system/style.css': resolve(__dirname, '../design-system/dist/design-system.css')

// TO-BE
'@portal/design-system-vue/style.css': resolve(__dirname, '../design-system-vue/dist/design-system.css')
```

### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `frontend/portal-shell/vite.config.ts:36` | alias 키와 경로를 `design-system-vue`로 변경 |
| `frontend/blog-frontend/vite.config.ts:33` | alias 키와 경로를 `design-system-vue`로 변경 |
| `frontend/shopping-frontend/vite.config.ts:34-36` | alias 키와 경로를 `design-system-vue`로 변경 |

### 추가 조치
- 레거시 `frontend/design-system/` 폴더 삭제 (dist와 node_modules만 존재하던 빈 패키지)

## 재발 방지 (Prevention)

### 모니터링
- CI/CD에서 모든 프론트엔드 앱 빌드 테스트 추가
- `npm run dev:portal`, `npm run dev:blog`, `npm run dev:shopping` 통합 테스트

### 프로세스 개선
1. **리팩토링 체크리스트 작성**
   - 패키지명 변경 시 모든 alias 설정 확인
   - import 경로와 alias 설정의 일관성 검증

2. **패키지 네이밍 규칙 명확화**
   - 프레임워크별 접미사 사용: `-vue`, `-react`
   - alias 설정도 동일한 네이밍 따르기

## 학습 포인트

1. **Vite alias와 package exports의 우선순위**
   - alias가 먼저 매칭되고, 매칭 실패 시 package.json exports 확인
   - alias 키가 정확히 일치해야 함 (부분 매칭 X)

2. **모노레포에서 리팩토링 시 주의사항**
   - 패키지명 변경 시 의존하는 모든 곳 확인
   - vite.config.ts의 alias, tsconfig의 paths 모두 업데이트

3. **레거시 폴더 정리의 중요성**
   - 사용하지 않는 폴더가 혼란 야기
   - package.json 없는 폴더는 workspace에서 제외되지만 alias에서 참조 가능

## 관련 링크

- [Vite Resolve Alias 문서](https://vite.dev/config/shared-options.html#resolve-alias)
- [npm Workspaces 문서](https://docs.npmjs.com/cli/v10/using-npm/workspaces)

## 관련 이슈

- 브랜치: `fix/design-system-import-alias`
