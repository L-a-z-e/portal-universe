# RBAC 리팩토링 구현 가이드 - Blog Service

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- SecurityConfig: GET 공개, POST/PUT/DELETE authenticated, 파일 삭제만 `hasRole("ADMIN")`
- @PreAuthorize: FileController에만 사용 (파일 업로드: isAuthenticated, 파일 삭제: hasRole("ADMIN"))
- 소유권 검증: PostServiceImpl, CommentService, SeriesService에서 authorId 비교
- Admin 전용 API: `/posts/all` (전체 게시물 조회) 존재하나 권한 검증 부재
- Feign Client: 미사용 (Gateway 헤더 기반 인증)
- MongoDB 사용 (Post, Comment, Series 문서)

## 변경 목표
- BLOG_ADMIN 역할 도입: 모든 게시물/댓글/시리즈 관리
- SUPER_ADMIN: 전체 관리 권한
- Admin 전용 API 경로 정리: `/admin/**`
- @PreAuthorize 일관성 확보
- Membership 기반 기능 분기 (PREMIUM → 커스텀 테마, 통계 대시보드 등)
- SecurityConfig 권한 체계 개편

---

## 구현 단계

### Phase 2: GatewayAuthenticationFilter 수신 변경

common-library의 EnhancedGatewayAuthenticationFilter 적용:
- X-User-Roles: 콤마 구분 복수 Authority
- X-User-Memberships: JSON → MembershipContext

### Phase 3: SecurityConfig 변경

```
// 공개 (변경 없음)
GET /posts, /posts/**                      → permitAll
GET /tags, /tags/**                        → permitAll
GET /categories, /categories/**            → permitAll

// 인증 필요 (변경 없음)
POST /posts                                → authenticated
PUT /posts/**                              → authenticated
DELETE /posts/**                           → authenticated
POST /posts/*/like                         → authenticated
POST /posts/*/comments                     → authenticated
PUT /comments/**                           → authenticated
DELETE /comments/**                        → authenticated
POST /file/upload                          → authenticated
POST /series                               → authenticated
PUT /series/**                             → authenticated
DELETE /series/**                          → authenticated

// BLOG_ADMIN (신규)
/admin/**                                  → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
DELETE /file/delete                        → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
```

### Phase 3: Admin 전용 API 정리

#### AdminPostController (신규)
```
GET /admin/posts                → 전체 게시물 목록 (필터/검색)
GET /admin/posts/{id}           → 게시물 상세 (비공개 포함)
DELETE /admin/posts/{id}        → 게시물 강제 삭제
PUT /admin/posts/{id}/status    → 게시물 상태 변경 (비공개 전환, 스팸 처리 등)
```

#### AdminCommentController (신규)
```
GET /admin/comments             → 전체 댓글 목록
DELETE /admin/comments/{id}     → 댓글 강제 삭제
```

#### AdminDashboardController (신규)
```
GET /admin/dashboard            → 블로그 통계 요약 (총 게시물, 활성 사용자 등)
```

### Phase 3: 소유권 검증 + ADMIN 바이패스

```java
// PostServiceImpl에 수정
public PostResponse updatePost(String postId, PostUpdateRequest request, String userId, List<String> roles) {
    Post post = postRepository.findById(postId)
        .orElseThrow(...);

    // BLOG_ADMIN/SUPER_ADMIN → 무조건 허용
    if (roles.contains("ROLE_BLOG_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
        return updateAndReturn(post, request);
    }

    // 일반 사용자 → 본인 게시물만
    if (!post.getAuthorId().equals(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
    }
    return updateAndReturn(post, request);
}
```

동일 패턴을 CommentService, SeriesService에도 적용.

### Phase 3: Membership 기반 기능 분기

```java
// PREMIUM 멤버십 전용 기능 예시
@GetMapping("/my/analytics")
public ApiResponse<BlogAnalyticsResponse> getMyAnalytics(
        @AuthenticationPrincipal String userId) {
    String tier = MembershipContext.getTier("blog");
    if (!"PREMIUM".equals(tier) && !"VIP".equals(tier)) {
        throw new CustomBusinessException(BlogErrorCode.MEMBERSHIP_REQUIRED);
    }
    return ApiResponse.success(analyticsService.getUserAnalytics(userId));
}
```

멤버십별 차별화 기능:
- FREE: 기본 게시물 작성, 10개 이미지 업로드
- BASIC: 시리즈 무제한, 50개 이미지 업로드
- PREMIUM: 커스텀 테마, 통계 대시보드, 무제한 이미지
- VIP: 추천 블로거 배지, 우선 검색 노출

### Phase 5: @PreAuthorize 일관성 확보

모든 Admin 컨트롤러:
```java
@PreAuthorize("hasAnyRole('BLOG_ADMIN', 'SUPER_ADMIN')")
```

FileController 변경:
```java
@PreAuthorize("hasAnyRole('BLOG_ADMIN', 'SUPER_ADMIN')")  // 기존: hasRole('ADMIN')
public ResponseEntity<Void> deleteFile(...)
```

### Phase 5: Kafka Consumer (권한 변경 수신)

```java
@KafkaListener(topics = "auth.role.changed")
public void handleRoleChanged(RoleChangedEvent event) {
    // 필요 시 블로그 내 캐시 무효화
    log.info("Role changed for user {}: {}", event.userId(), event.action());
}

@KafkaListener(topics = "auth.membership.changed")
public void handleMembershipChanged(MembershipChangedEvent event) {
    if ("blog".equals(event.serviceName())) {
        // 멤버십별 기능 제한 캐시 갱신
        membershipCacheService.invalidate(event.userId());
    }
}
```

---

## 영향받는 파일

### 신규 생성
```
src/main/java/.../controller/AdminPostController.java
src/main/java/.../controller/AdminCommentController.java
src/main/java/.../controller/AdminDashboardController.java
src/main/java/.../service/AdminPostService.java
src/main/java/.../service/AdminCommentService.java
src/main/java/.../service/BlogAnalyticsService.java
src/main/java/.../consumer/RoleChangeConsumer.java
src/main/java/.../consumer/MembershipChangeConsumer.java
```

### 수정 필요
```
src/main/java/.../common/config/SecurityConfig.java
  - 경로별 접근 제어 개편
  - /admin/** → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")
  - hasRole("ADMIN") → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")

src/main/java/.../post/service/PostServiceImpl.java
  - ADMIN 바이패스 로직 추가 (소유권 검증 시)

src/main/java/.../comment/service/CommentService.java
  - ADMIN 바이패스 로직 추가

src/main/java/.../series/service/SeriesService.java
  - ADMIN 바이패스 로직 추가

src/main/java/.../file/controller/FileController.java
  - @PreAuthorize → hasAnyRole("BLOG_ADMIN", "SUPER_ADMIN")

src/main/java/.../post/controller/PostController.java
  - /posts/all → /admin/posts로 이동 (AdminPostController)

src/main/java/.../common/exception/BlogErrorCode.java
  - MEMBERSHIP_REQUIRED 등 추가
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] BLOG_ADMIN: 타인 게시물 수정/삭제 성공
- [ ] USER: 본인 게시물 수정 성공, 타인 게시물 수정 → 403
- [ ] SUPER_ADMIN: 모든 게시물 관리 성공
- [ ] Membership PREMIUM: 통계 대시보드 접근 성공
- [ ] Membership FREE: 통계 대시보드 접근 → MEMBERSHIP_REQUIRED

### 통합 테스트
- [ ] Admin API 전체 흐름 (게시물/댓글 관리)
- [ ] Admin API: BLOG_ADMIN 접근 성공, USER 접근 거부
- [ ] /posts/all → /admin/posts 마이그레이션 정상 동작

### 하위 호환 테스트
- [ ] 기존 게시물 CRUD 정상 동작
- [ ] 기존 댓글/시리즈 CRUD 정상 동작
- [ ] 기존 파일 업로드/삭제 정상 동작
