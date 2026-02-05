# TS-20260130-006: Prism 404 - API 경로 버저닝 누락

## 상태: 해결됨

## 증상

Portal Shell에서 Prism 메뉴 진입 후 API 호출 시 **404 Not Found** 발생.

## 환경

- Portal Shell (Vue 3, :30000)
- Prism Frontend (React 18, :30003)
- API Gateway (Spring Cloud Gateway, :8080)

## 원인

`prism-frontend/src/services/api.ts`의 API 경로에 `/api/v1` 접두사가 누락되어 있었음.

| 구성 요소 | 경로 패턴 | 비고 |
|-----------|----------|------|
| api-gateway route | `/api/v1/prism/**` → prism-service | gateway 라우팅 규칙 |
| prism-frontend (수정 전) | `/prism/providers` | `/api/v1/` 접두사 없음 |
| prism-frontend (수정 후) | `/api/v1/prism/providers` | 정상 |

다른 서비스(blog, shopping)는 이미 `/api/v1/` 경로를 사용 중이었으나, prism-frontend만 누락된 상태였음.

## 해결

**파일:** `frontend/prism-frontend/src/services/api.ts`

모든 API 경로에 `/api/v1` 접두사 추가:

```
Before: /prism/providers, /prism/boards, /prism/agents, /prism/tasks/...
After:  /api/v1/prism/providers, /api/v1/prism/boards, /api/v1/prism/agents, /api/v1/prism/tasks/...
```

## 검증

1. `npm run build` 성공
2. Playwright로 Prism 페이지 진입 확인 (Module Federation 정상 로드)
3. 콘솔에 API 404 에러 없음 확인

## 교훈

- 새 서비스 추가 시 API 경로 컨벤션(`/api/v1/{service}/**`) 준수 확인 필수
- API Gateway route 패턴과 프론트엔드 API 호출 경로의 일관성 검증 필요
- 기존 서비스(blog, shopping)의 패턴을 참조하여 동일 구조 적용

## 관련 문서

- [TS-20260128-004: Like API URL Mismatch](./TS-20260128-004-like-api-url-mismatch.md)
