# Design Types API Documentation

Design System 공유 타입 정의 명세입니다.

## API 문서 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [component-types.md](./component-types.md) | 컴포넌트 공통 Props 타입 (30개 인터페이스) | current |
| [theme-types.md](./theme-types.md) | 테마/공유 타입 (Size, Variant, Theme Config 등 37개) | current |
| [api-types.md](./api-types.md) | API 응답 타입 (ApiResponse, ApiErrorResponse 등 4개) | current |

## 개요

| 항목 | 내용 |
|------|------|
| **패키지명** | `@portal/design-types` |
| **언어** | TypeScript |
| **소스** | `frontend/design-types/src/` |
| **사용처** | design-system-vue, design-system-react, portal-shell, shopping-frontend, prism-frontend |

## 파일 구조

```
frontend/design-types/src/
├── index.ts        # Re-exports + ThemeConfig 타입
├── common.ts       # 공유 타입 (Size, Variant 등 34개)
├── components.ts   # 컴포넌트 Props (33개 인터페이스)
└── api.ts          # API 응답 타입 (4개 인터페이스)
```

## 타입 수량

| 카테고리 | 타입 수 | 문서 |
|----------|---------|------|
| 공유 타입 (Size, Variant 등) | 34 | theme-types.md |
| 테마 설정 (ServiceType 등) | 3 | theme-types.md |
| 컴포넌트 Props | 33 | component-types.md |
| API 응답 | 4 | api-types.md |
| **합계** | **74** | |

## 관련 문서

- [Design System Architecture](../../architecture/design-system/system-overview.md)

---

**최종 업데이트**: 2026-02-06
