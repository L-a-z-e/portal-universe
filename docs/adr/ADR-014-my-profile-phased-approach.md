---
id: ADR-014
title: 마이프로필 단계별 구현 전략
type: adr
status: accepted
created: 2026-01-21
updated: 2026-01-21
author: Laze
decision_date: 2026-01-21
reviewers:
  - Development Team
tags:
  - profile
  - phased-implementation
  - jwt
  - risk-mitigation
related:
  - SCENARIO-003-my-profile
  - ADR-004-jwt-rbac-auto-configuration
---

# ADR-014: 마이프로필 단계별 구현 전략

## 상태
**Accepted**

## 날짜
2026-01-21

---

## 컨텍스트

Portal Shell에 마이프로필 기능을 추가하려고 합니다. 사용자는 자신의 프로필 정보를 조회하고 수정할 수 있어야 합니다.

### 현재 상황

1. **JWT 작업 진행 중**
   - ADR-004에서 JWT RBAC 자동 설정 구현 중
   - Auth Service의 JWT 처리 로직이 변경되고 있음
   - `auth.ts`, `authService.ts` 수정 시 충돌 위험

2. **기존 Auth Store 구조**
   - OIDC 로그인 시 사용자 정보를 Auth Store에 저장
   - `user.profile`에 모든 프로필 정보 포함
   - 이미 메모리에 로드된 데이터 존재

3. **요구사항**
   - 프로필 조회 (필수)
   - 프로필 수정 (닉네임, 이미지)
   - 비밀번호 변경
   - 회원 탈퇴

### 결정 배경

| 제약 조건 | 영향 |
|----------|------|
| JWT 작업 진행 중 | Auth 관련 코드 수정 제한 |
| Auth Store에 데이터 존재 | API 호출 없이도 조회 가능 |
| 빠른 기능 제공 필요 | 최소 기능부터 단계적 구현 |
| 코드 충돌 회피 | 기존 파일 수정 금지 |

---

## 결정

**2단계(Phase) 전략을 채택하여 마이프로필 기능을 구현합니다.**

### Phase 1: 읽기 전용 프로필 (즉시 구현)
- **목표**: 사용자 프로필 정보 조회만 제공
- **데이터 소스**: Auth Store (`user.profile`)
- **API 호출**: 없음
- **구현 범위**:
  - 프로필 페이지 (`/my-profile`)
  - 프로필 정보 표시 (이미지, 닉네임, 이메일, 가입일)
  - 소셜 계정 연결 정보 표시
- **제약**: 기존 `auth.ts`, `authService.ts` 수정 금지

### Phase 2: 프로필 수정 기능 (JWT 작업 완료 후)
- **목표**: 프로필 수정/삭제 기능 추가
- **데이터 소스**: REST API (`/api/auth/profile`)
- **API 호출**: JWT 토큰 기반 인증
- **구현 범위**:
  - 프로필 수정 (닉네임, 이미지)
  - 비밀번호 변경
  - 회원 탈퇴
  - 소셜 계정 연결/해제

---

## Decision Drivers

1. **리스크 회피**
   - JWT 작업과 병렬 개발로 인한 충돌 방지
   - 기존 인증 로직에 영향 없음

2. **빠른 가치 제공**
   - Phase 1로 핵심 기능(조회) 즉시 제공
   - 사용자 피드백 조기 수집

3. **점진적 개선**
   - Phase 2에서 안정적으로 수정 기능 추가
   - JWT 작업 완료 후 안전하게 통합

4. **코드 격리**
   - 새 컴포넌트로 구현하여 기존 코드 보호
   - Auth Store의 공개 API만 사용

---

## Considered Options

### Option 1: 한 번에 완성 (읽기 + 수정)

**장점**:
- 기능 완전성
- 일관된 UX

**단점**:
- JWT 작업과 충돌 위험 ❌
- 개발 기간 길어짐 (2-3주)
- 병렬 개발 불가

**평가**: ❌ 리스크가 너무 높음

---

### Option 2: Phase 분리 (읽기 → 수정)

**장점**:
- JWT 작업과 격리 ✅
- 빠른 기능 제공 (1주)
- 단계별 검증 가능
- 코드 충돌 없음

**단점**:
- 2단계 배포 필요
- Phase 1에서 수정 불가

**평가**: ✅ **선택** - 리스크 최소화 + 빠른 제공

---

### Option 3: 읽기만 구현 (수정 미정)

**장점**:
- 가장 빠름
- 리스크 없음

**단점**:
- 불완전한 기능 ❌
- 사용자 기대 불만족

**평가**: ❌ 핵심 기능 누락

---

## Decision (최종 결정)

**Option 2: Phase 분리 전략을 채택합니다.**

### Phase 1 상세 계획

#### 1-1. 구현 범위

```typescript
// MyProfilePage.vue
<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()

// ✅ Auth Store에서 직접 읽기 (API 호출 없음)
const profile = computed(() => authStore.user?.profile)
const joinedDate = computed(() => {
  const issuedAt = authStore.user?._issuedAt
  return issuedAt ? new Date(issuedAt * 1000).toLocaleDateString('ko-KR') : '-'
})
</script>

<template>
  <div class="my-profile">
    <!-- 프로필 이미지 -->
    <img :src="profile?.picture || '/default-avatar.png'" />

    <!-- 닉네임 -->
    <h2>{{ profile?.nickname || profile?.name }}</h2>

    <!-- 이메일 -->
    <p>{{ profile?.email }}</p>

    <!-- 가입일 -->
    <p>가입일: {{ joinedDate }}</p>
  </div>
</template>
```

#### 1-2. 파일 추가

| 파일 | 역할 | 수정 여부 |
|------|------|----------|
| `pages/MyProfilePage.vue` | 프로필 페이지 | ✅ 신규 생성 |
| `components/profile/*` | 프로필 컴포넌트 | ✅ 신규 생성 |
| `router/index.ts` | 라우트 추가 | ⚠️ 라우트만 추가 |
| `store/auth.ts` | (기존) | ❌ 수정 금지 |
| `services/authService.ts` | (기존) | ❌ 수정 금지 |

#### 1-3. 장점

1. **빠른 구현**: 1-2일 내 완료 가능
2. **리스크 없음**: 기존 코드 수정 없음
3. **API 불필요**: Auth Store 데이터만 사용
4. **성능 우수**: 메모리 읽기만 (즉시 렌더링)

#### 1-4. 제약

- 읽기 전용 (수정 버튼 비활성화 또는 숨김)
- Auth Store 데이터와 동기화 (API 재호출 없음)

---

### Phase 2 상세 계획 (JWT 작업 완료 후)

#### 2-1. 구현 범위

```java
// AuthController.java (Backend)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserProfileResponse> updateProfile(
        @Valid @RequestBody ProfileUpdateRequest request,
        Authentication authentication) {

        String userEmail = authentication.getName();
        UserProfile updated = authService.updateProfile(userEmail, request);

        // ✅ JWT 재발급 (닉네임 변경 시)
        String newToken = jwtService.generateToken(updated);

        return ApiResponse.success(updated, Map.of("token", newToken));
    }

    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteAccount(
        @Valid @RequestBody AccountDeleteRequest request,
        Authentication authentication) {

        authService.deleteAccount(authentication.getName(), request);
        return ApiResponse.success(null);
    }
}
```

#### 2-2. Frontend 통합

```typescript
// composables/useProfile.ts
export function useProfile() {
  const authStore = useAuthStore()

  const updateProfile = async (data: ProfileUpdateRequest) => {
    const response = await authService.updateProfile(data)

    // ✅ Auth Store 갱신
    if (response.success && response.meta?.token) {
      // 새 토큰으로 사용자 정보 재설정
      const newUser = parseJwtPayload(response.meta.token)
      authStore.setUser(newUser)
    }

    return response
  }

  return { updateProfile }
}
```

#### 2-3. 추가 기능

| 기능 | API | 설명 |
|------|-----|------|
| 프로필 수정 | `PATCH /api/auth/profile` | 닉네임, 이미지 변경 |
| 비밀번호 변경 | `POST /api/auth/password/change` | 현재/새 비밀번호 |
| 회원 탈퇴 | `DELETE /api/auth/account` | 확인 + 비밀번호 |
| 소셜 연결 | `POST /api/auth/oauth2/link/{provider}` | 추가 소셜 계정 |
| 소셜 해제 | `DELETE /api/auth/oauth2/unlink/{provider}` | 연결 해제 |

---

## Consequences

### 긍정적 영향

1. **리스크 최소화**
   - JWT 작업과 격리되어 충돌 없음
   - 기존 인증 로직 안정성 유지
   - 단계별 검증으로 버그 조기 발견

2. **빠른 가치 제공**
   - Phase 1을 1-2일 내 구현 가능
   - 사용자에게 핵심 기능(조회) 즉시 제공
   - 조기 피드백 수집 가능

3. **코드 품질**
   - 새 컴포넌트로 격리하여 깔끔한 구조
   - Auth Store의 공개 API만 사용 (낮은 결합도)
   - 테스트 용이

4. **유연성**
   - Phase 2 일정을 JWT 작업에 맞춰 조정 가능
   - 요구사항 변경 시 Phase 2에서 반영

### 부정적 영향

1. **불완전한 UX**
   - Phase 1에서 수정 기능 없음
   - 사용자 기대 불만족 가능성
   - "수정 버튼은 왜 없나요?" 문의 예상

2. **2단계 배포**
   - Phase 1 배포 후 Phase 2 재배포
   - 배포 비용 증가
   - 사용자 공지 필요

3. **데이터 동기화**
   - Phase 1은 Auth Store 데이터만 사용
   - 로그아웃 후 재로그인 시에만 최신 정보 반영
   - (완화: Phase 2에서 API 호출로 해결)

4. **개발자 혼란**
   - "왜 Phase 1에서 수정 못 하나요?"
   - 코드 리뷰 시 설명 필요

### 완화 방안

1. **UX 완화**
   - Phase 1에서 "프로필 수정 기능은 곧 추가됩니다" 안내 메시지
   - 수정 버튼을 비활성화로 표시 (곧 활성화 예정 툴팁)

2. **배포 비용 완화**
   - Phase 1은 Frontend만 배포 (Backend 변경 없음)
   - Phase 2는 JWT 작업 PR과 함께 배포

3. **데이터 동기화 완화**
   - Phase 1에서는 "최신 정보는 재로그인 시 반영됩니다" 안내

4. **문서화**
   - ADR-014 및 SCENARIO-003로 Phase 전략 명확히 기록
   - 팀 공유 및 코드 리뷰 시 참고

---

## 구현 타임라인

### Phase 1: 읽기 전용 (즉시 시작)

| 단계 | 작업 | 예상 시간 |
|------|------|----------|
| 1 | `MyProfilePage.vue` 생성 | 2-3시간 |
| 2 | 프로필 컴포넌트 생성 | 2-3시간 |
| 3 | 라우트 추가 및 네비게이션 연결 | 1시간 |
| 4 | 스타일링 및 반응형 | 2-3시간 |
| 5 | 테스트 (Unit + E2E) | 2-3시간 |
| **합계** | | **1-2일** |

### Phase 2: 수정 기능 (JWT 작업 완료 후)

| 단계 | 작업 | 예상 시간 |
|------|------|----------|
| 1 | Backend API 구현 | 1-2일 |
| 2 | Frontend 편집 모드 추가 | 1일 |
| 3 | API 연동 및 Store 갱신 | 1일 |
| 4 | 비밀번호 변경 / 회원 탈퇴 | 1일 |
| 5 | 테스트 (Unit + Integration + E2E) | 1-2일 |
| **합계** | | **5-7일** |

---

## 에러 처리 전략

### Phase 1

```typescript
// MyProfilePage.vue
<script setup lang="ts">
const authStore = useAuthStore()
const router = useRouter()

// ✅ 비로그인 처리
if (!authStore.isAuthenticated) {
  router.push('/login')
}

// ✅ 프로필 데이터 없음 처리
if (!authStore.user?.profile) {
  // 에러 페이지 또는 재로그인 안내
  router.push('/error?code=PROFILE_DATA_MISSING')
}
</script>
```

### Phase 2 (예정)

```typescript
// composables/useProfile.ts
const handleError = (error: ApiError) => {
  switch (error.code) {
    case 'A401': // JWT 만료
      router.push('/login?redirect=/my-profile')
      break
    case 'A409': // 닉네임 중복
      toast.error('이미 사용 중인 닉네임입니다')
      break
    case 'A400': // 유효성 검증 실패
      toast.error(error.message)
      break
    default:
      toast.error('오류가 발생했습니다')
  }
}
```

---

## 테스트 전략

### Phase 1 테스트

```typescript
// tests/pages/MyProfilePage.spec.ts
describe('MyProfilePage (Phase 1 - 읽기 전용)', () => {
  it('로그인 상태에서 프로필 표시', () => {
    const mockUser = {
      profile: {
        email: 'test@example.com',
        nickname: '테스터',
        picture: 'http://example.com/avatar.png'
      }
    }
    // useAuthStore mock 설정

    const { getByText } = render(<MyProfilePage />)
    expect(getByText('테스터')).toBeInTheDocument()
    expect(getByText('test@example.com')).toBeInTheDocument()
  })

  it('비로그인 시 /login 리다이렉트', () => {
    // useAuthStore mock: isAuthenticated = false

    const { push } = useRouter()
    render(<MyProfilePage />)

    expect(push).toHaveBeenCalledWith('/login')
  })
})
```

### Phase 2 테스트 (예정)

```java
// AuthControllerTest.java
@Test
@DisplayName("프로필 수정 성공")
@WithMockUser(username = "test@example.com")
void updateProfile_Success() throws Exception {
    ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임", null);

    mockMvc.perform(patch("/api/auth/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
}
```

---

## 보안 고려사항

### Phase 1
- ✅ Route Guard로 비로그인 접근 차단
- ✅ Auth Store 데이터는 로그인 시 검증됨
- ✅ 본인 프로필만 조회 (다른 사용자 프로필 접근 불가)

### Phase 2 (예정)
- ✅ JWT 토큰 기반 인증
- ✅ `@PreAuthorize("isAuthenticated()")` 적용
- ✅ 본인 프로필만 수정 가능 (Backend 검증)
- ✅ 비밀번호 변경 시 현재 비밀번호 확인
- ✅ 회원 탈퇴 시 2단계 확인 (다이얼로그 + 비밀번호)

---

## 다음 단계

### Phase 1 즉시 실행
1. `MyProfilePage.vue` 생성 (1일)
2. 라우트 추가 및 네비게이션 연결
3. 테스트 작성 및 배포

### Phase 2 (JWT 작업 완료 후)
1. JWT 작업 완료 대기 (ADR-004)
2. Backend API 구현 (`/api/auth/profile`)
3. Frontend 편집 모드 추가
4. Auth Store 갱신 로직 구현
5. 통합 테스트 및 배포

---

## 참고 자료

- [SCENARIO-003: 마이프로필 조회 및 관리](../scenarios/SCENARIO-003-my-profile.md)
- [ADR-004: JWT RBAC 자동 설정 전략](./ADR-004-jwt-rbac-auto-configuration.md)
- [Auth Store 구현](../../frontend/portal-shell/src/store/auth.ts)
- [UserProfile 타입 정의](../../frontend/portal-shell/src/types/user.ts)

---

**문서 버전**: 1.0
**작성자**: Laze
**최종 업데이트**: 2026-01-21
