---
id: TS-20260128-004
title: 좋아요 기능 API 경로 불일치 오류
type: troubleshooting
status: resolved
created: 2026-01-28
updated: 2026-01-28
author: Laze
severity: medium
resolved: true
affected_services:
  - blog-frontend
  - blog-service
tags:
  - api
  - frontend
  - vue
  - api-gateway
  - route-mismatch
---

# TS-20260128-004: 좋아요 기능 API 경로 불일치 오류

## 📊 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟡 Medium |
| **발생일** | 2026-01-28 |
| **해결일** | 2026-01-28 |
| **영향 서비스** | blog-frontend, blog-service |
| **영향 기능** | 좋아요 토글, 좋아요 상태 조회, 좋아요한 사용자 목록 조회 |

## 🔍 증상 (Symptoms)

Frontend에서 좋아요 기능 사용 시 다음과 같은 문제가 발생:

- 좋아요 버튼 클릭 시 404 Not Found 오류 발생
- 좋아요 상태가 로드되지 않음
- 좋아요한 사용자 목록이 표시되지 않음
- 브라우저 콘솔에 API 호출 실패 에러 표시

### 에러 로그 예시

```
GET http://localhost:8080/api/blog/likes/123/status 404 (Not Found)
POST http://localhost:8080/api/blog/likes/123/toggle 404 (Not Found)
GET http://localhost:8080/api/blog/likes/123/likers 404 (Not Found)
```

## 🎯 원인 분석 (Root Cause)

Frontend `likes.ts`의 API 경로가 Backend `LikeController`의 실제 엔드포인트와 완전히 불일치했습니다.

### Backend (LikeController)

Backend는 `@RequestMapping("/posts/{postId}")` 기반으로 설계되어 PostController와 동일한 base path를 사용:

| 기능 | 메서드 | Controller 경로 | Gateway 경로 (StripPrefix=2) |
|------|--------|-----------------|------------------------------|
| 토글 | POST | `/posts/{postId}/like` | `/api/blog/posts/{postId}/like` |
| 상태 | GET | `/posts/{postId}/like` | `/api/blog/posts/{postId}/like` |
| 목록 | GET | `/posts/{postId}/likes` | `/api/blog/posts/{postId}/likes` |

### Frontend (likes.ts) - 수정 전

Frontend는 잘못된 base path와 sub-path를 사용:

```typescript
const BASE_PATH = '/api/blog/likes'  // ❌ 잘못된 base path

export const likesApi = {
  toggleLike: (postId: string) =>
    apiClient.post(`${BASE_PATH}/${postId}/toggle`),  // ❌ /toggle

  getLikeStatus: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/status`),   // ❌ /status

  getLikers: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/likers`)    // ❌ /likers
}
```

### 불일치 사항 정리

**4가지 핵심 불일치:**

1. **Base path 불일치**:
   - Frontend: `/api/blog/likes`
   - Backend: `/api/blog/posts` (LikeController가 PostController와 동일한 base 사용)

2. **토글 sub-path 불일치**:
   - Frontend: `/toggle`
   - Backend: `/like`

3. **상태 sub-path 불일치**:
   - Frontend: `/status`
   - Backend: `/like` (GET 메서드로 구분)

4. **목록 sub-path 불일치**:
   - Frontend: `/likers`
   - Backend: `/likes`

### 근본 원인

- Backend Controller의 `@RequestMapping` 구조를 정확히 파악하지 않고 Frontend API client를 작성
- API Gateway의 `StripPrefix` 설정이 경로 변환에 미치는 영향을 고려하지 않음
- API 문서나 실제 Controller 코드를 참조하지 않고 추측으로 경로 정의

## ✅ 해결 방법 (Solution)

### 1. 수정 파일: `frontend/blog-frontend/src/api/likes.ts`

```typescript
// Before
const BASE_PATH = '/api/blog/likes'

export const likesApi = {
  toggleLike: (postId: string) =>
    apiClient.post(`${BASE_PATH}/${postId}/toggle`),

  getLikeStatus: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/status`),

  getLikers: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/likers`)
}

// After
const BASE_PATH = '/api/blog/posts'

export const likesApi = {
  toggleLike: (postId: string) =>
    apiClient.post(`${BASE_PATH}/${postId}/like`),

  getLikeStatus: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/like`),

  getLikers: (postId: string) =>
    apiClient.get(`${BASE_PATH}/${postId}/likes`)
}
```

### 2. 변경 사항 요약

| 항목 | 수정 전 | 수정 후 |
|------|--------|--------|
| **BASE_PATH** | `/api/blog/likes` | `/api/blog/posts` |
| **toggleLike** | POST `/{postId}/toggle` | POST `/{postId}/like` |
| **getLikeStatus** | GET `/{postId}/status` | GET `/{postId}/like` |
| **getLikers** | GET `/{postId}/likers` | GET `/{postId}/likes` |

### 3. 추가 수정: `frontend/blog-frontend/src/api/tags.ts`

동일한 패턴 점검 중 `searchTags()` 함수에서 Backend와 일치하지 않는 파라미터 발견:

```typescript
// After
export const tagsApi = {
  searchTags: (keyword: string, limit?: number) =>
    apiClient.get<Tag[]>(`${BASE_PATH}/search`, {
      params: { keyword, limit }  // limit 파라미터 추가
    })
}
```

## 🎨 영향 범위

수정으로 인해 다음 컴포넌트들이 정상 작동:

### 1. LikeButton.vue
- 좋아요 버튼 클릭 시 토글 정상 작동
- 좋아요 상태 실시간 업데이트
- 좋아요 수 카운트 표시

### 2. PostDetailPage.vue
- 게시글 상세 페이지에서 좋아요 수 표시
- 좋아요한 사용자 목록 버튼 활성화

### 3. LikersModal.vue
- 좋아요한 사용자 목록 모달 정상 표시
- 사용자 프로필 정보 로드

## 🛡️ 재발 방지 (Prevention)

### 즉시 조치

1. **API 경로 검증 체크리스트 작성**

```markdown
- [ ] Backend Controller의 @RequestMapping 확인
- [ ] API Gateway의 StripPrefix 설정 확인
- [ ] 실제 호출되는 전체 경로 계산
- [ ] Swagger/OpenAPI 문서와 대조
```

2. **Frontend API Client 작성 가이드 업데이트**

```
1. Backend Controller 코드 직접 확인
2. Gateway 설정 파일 확인 (application.yml)
3. 경로 조합 공식 이해:
   - Controller @RequestMapping + Method mapping
   - Gateway: /api/{service}/** → StripPrefix=2 → /{controller-path}
4. Postman/curl로 실제 호출 테스트
```

### 장기 조치

1. **OpenAPI/Swagger 문서 자동 생성**
   - Backend: Springdoc OpenAPI 적용
   - Frontend: 생성된 스펙으로 API client 자동 생성 검토

2. **E2E 테스트 강화**
   - 실제 API 호출을 포함한 E2E 테스트 작성
   - CI 파이프라인에 통합

3. **API Contract Testing 도입**
   - Pact 등의 Contract Testing 도구 검토
   - Frontend-Backend 간 API 계약 명시

4. **코드 리뷰 체크리스트**
   - API client 추가/수정 시 Backend 경로와 대조 필수
   - Gateway 설정 변경 시 영향받는 Frontend 코드 확인

## 📚 학습 포인트

### API Gateway의 경로 변환 이해

```
Client Request:
  → http://localhost:30001/api/blog/posts/123/like

Gateway (StripPrefix=2):
  → /api/blog/posts/123/like
  → Strip '/api/blog' (2 segments)
  → Forward to blog-service: /posts/123/like

Backend Controller:
  → @RequestMapping("/posts/{postId}")
  → @PostMapping("/like")
  → Matched: /posts/123/like ✅
```

### Controller 설계 패턴 이해

```java
// LikeController는 PostController와 동일한 base path 사용
@RestController
@RequestMapping("/posts/{postId}")
public class LikeController {

    @PostMapping("/like")        // POST /posts/{postId}/like
    @GetMapping("/like")         // GET /posts/{postId}/like (다른 메서드)
    @GetMapping("/likes")        // GET /posts/{postId}/likes (복수형)
}
```

### 동일 경로, 다른 메서드 활용

- `POST /posts/{postId}/like` → 좋아요 토글
- `GET /posts/{postId}/like` → 좋아요 상태 조회
- RESTful 설계: 같은 리소스에 대한 다른 동작은 HTTP 메서드로 구분

## 🔗 관련 파일

### Backend
- `services/blog-service/src/main/java/.../like/controller/LikeController.java`
- `services/api-gateway/src/main/resources/application.yml` (StripPrefix 설정)

### Frontend
- `frontend/blog-frontend/src/api/likes.ts`
- `frontend/blog-frontend/src/api/tags.ts`
- `frontend/blog-frontend/src/components/like/LikeButton.vue`
- `frontend/blog-frontend/src/components/like/LikersModal.vue`
- `frontend/blog-frontend/src/pages/blog/PostDetailPage.vue`

## 🔍 참고 자료

- [Spring @RequestMapping Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestMapping.html)
- [Spring Cloud Gateway - StripPrefix Filter](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-stripprefix-gatewayfilter-factory)
- Portal Universe API Gateway 설정: `services/api-gateway/README.md`
