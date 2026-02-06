# Auth Service Code Review & Refactoring Plan

## 개요
auth-service 전체 코드 리뷰를 통해 식별한 보안, 버그, dead code, 구조적 문제를 리팩토링합니다.
113개 Java 파일 중 핵심 파일 20+개를 검토하여 14건의 개선 사항을 도출했습니다.

---

## Tier 1: 보안 + 버그 (4건)

### 1-1. TokenBlacklistService - Redis key 최적화
- **파일**: `services/auth-service/.../auth/service/TokenBlacklistService.java`
- **Line 38**: `String key = BLACKLIST_PREFIX + token` → JWT 전체 문자열(~500+ chars)을 Redis key로 사용
- **문제**: Redis key가 비정상적으로 길어 메모리 낭비 및 성능 저하
- **근거**: JWT access token은 보통 500~1000 바이트. Redis key best practice는 짧은 key 권장
- **해결**: `BLACKLIST_PREFIX + SHA-256(token)` 으로 변경 (64 chars 고정)
- `isBlacklisted()` 메서드도 동일하게 hash 적용

### 1-2. Hardcoded `900` expiresIn
- **파일 1**: `services/auth-service/.../auth/controller/AuthController.java`
  - **Line 119**: `new LoginResponse(accessToken, refreshToken, 900)`
  - **Line 167**: `new RefreshResponse(accessToken, newRefreshToken, 900)`
- **파일 2**: `services/auth-service/.../oauth2/OAuth2AuthenticationSuccessHandler.java`
  - **Line 71**: `"&expires_in=900"` (URL fragment에 하드코딩)
- **근거**: `application.yml`에 `access-token-expiration: 900000` (15분)으로 설정되어 있지만, 코드에서 900(초)를 직접 작성. 설정값을 변경해도 응답에 반영되지 않음
- **해결**: `jwtProperties.getAccessTokenExpiration() / 1000` 로 계산하여 사용

### 1-3. DataInitializer 보안 및 정합성 문제
- **파일**: `services/auth-service/.../common/config/DataInitializer.java`
- **Line 26**: `@Profile({"default", "dev", "local", "docker"})` - `default` 프로필 포함
- **문제 1**: Spring Boot에서 프로필 미지정 시 `default`가 활성화됨 → 운영 환경에 테스트 데이터 생성 위험
- **문제 2**: `createTestUser()`가 `rbacInitializationService.initializeNewUser()`를 호출하지 않음
  - 정상 회원가입(`UserService.registerUser()`) 플로우는 Line 79에서 RBAC 초기화 수행
  - DataInitializer로 생성된 사용자는 ROLE_USER, 멤버십이 없음
  - `TokenService.generateAccessToken()` Line 157에서 `IllegalStateException` 발생 가능
- **근거**: 실제 `UserService.registerUser()`는 회원가입 시 8단계를 거치지만, DataInitializer는 DB 저장만 수행
- **해결**:
  1. `default` 프로필 제거 → `@Profile({"dev", "local", "docker"})`
  2. `createTestUser()` 내에서 `rbacInitializationService.initializeNewUser()` 호출 추가

### 1-4. ProfileService.changePassword - PasswordValidator 미적용
- **파일**: `services/auth-service/.../user/service/ProfileService.java`
- **Line 79-101**: `changePassword()`에서 비밀번호 정책 검증 없이 변경 허용
- **근거**: `UserService.changePassword()` (Line 232-251)은 `passwordValidator.validate(newPassword, user)` 호출하여 최소 8자, 대소문자, 특수문자, 이전 비밀번호 재사용 금지 등 검증
- **문제**: 같은 비밀번호 변경인데 경로에 따라 검증 수준이 다름
  - `ProfileController → ProfileService.changePassword()` → 정책 미검증
  - `UserService.changePassword()` → 정책 검증
- **해결**: `ProfileService.changePassword()`에 `PasswordValidator` 주입 및 검증 로직 추가, 또는 `UserService.doChangePassword()` 호출로 통합

---

## Tier 2: Dead Code / 불필요한 중복 (3건)

### 2-1. JwtConfig.java 삭제 + RefreshTokenService 수정
- **파일 1**: `services/auth-service/.../common/config/JwtConfig.java` (35줄)
- **파일 2**: `services/auth-service/.../auth/service/RefreshTokenService.java`
- **근거**:
  - `JwtConfig`와 `JwtProperties` 모두 `@ConfigurationProperties(prefix = "jwt")` 사용 → Spring 바인딩 충돌
  - `JwtConfig`는 단순 3필드(`secretKey`, `accessTokenExpiration`, `refreshTokenExpiration`)
  - `JwtProperties`는 key rotation 지원, `KeyConfig` inner class, `isExpired()/isActive()` 포함
  - `TokenService`는 이미 `JwtProperties` 사용 (Line 6)
  - **오직 `RefreshTokenService`만** `JwtConfig` 사용 (Line 3, 23)
  - Gateway에서도 동일한 JwtConfig dead code를 이미 삭제함 (commit `9e05844`)
- **해결**:
  1. `RefreshTokenService`: `JwtConfig` → `JwtProperties` 의존성 변경
  2. `JwtConfig.java` 파일 삭제

### 2-2. LoginController + login.html 검토/삭제
- **파일**: `services/auth-service/.../auth/controller/LoginController.java` (37줄)
- **Line 18**: `model.addAttribute("actionUrl", "/auth-service/login")` → URL 하드코딩
- **근거**:
  - auth-service는 JWT + REST API 기반 stateless 서비스
  - 로그인은 `AuthController.login()` (/api/v1/auth/login)이 처리
  - `LoginController`는 `@Controller` (MVC 뷰 반환) - form-based login 페이지
  - SecurityConfig에서 formLogin 비활성화 여부 확인 필요
  - OAuth2 authorization code flow에서 사용될 수 있으나, 현재 프론트엔드에서 직접 OAuth2 처리
- **해결**: 사용 여부 확인 후 불필요하면 삭제. OAuth2 flow에서 필요하면 URL 하드코딩만 수정

### 2-3. Cookie 관리 로직 중복 제거
- **파일 1**: `services/auth-service/.../auth/controller/AuthController.java` (Line 224-266)
- **파일 2**: `services/auth-service/.../oauth2/OAuth2AuthenticationSuccessHandler.java` (Line 43, 80-89)
- **중복 내용**:
  - `REFRESH_TOKEN_COOKIE_NAME = "portal_refresh_token"` 상수 중복
  - `setRefreshTokenCookie()` 메서드 로직 중복
  - `@Value("${app.cookie.secure}")`, `@Value("${app.cookie.same-site}")` 중복
  - `Duration.ofDays(7)` cookie maxAge 하드코딩 (refresh token expiration과 불일치 가능)
- **근거**: cookie 설정 변경 시 두 곳 모두 수정해야 함. maxAge가 refresh token expiration과 독립적으로 하드코딩
- **해결**: `CookieHelper` 유틸리티 클래스 추출. maxAge를 `jwtProperties.getRefreshTokenExpiration()`에서 파생

---

## Tier 3: 구조적 개선 (4건)

### 3-1. TokenService.extractKeyId - JSON 수동 파싱 개선
- **파일**: `services/auth-service/.../auth/service/TokenService.java`
- **Line 114-137**: indexOf 기반 수동 JSON 파싱으로 kid 추출
- **근거**: Gateway에서 동일한 문제를 이미 수정함 (commit `4cea410`). `"kid\":` 뒤의 문자열을 indexOf로 추출하는 방식은 JSON 형식 변경에 취약
- **해결**: `com.fasterxml.jackson.databind.ObjectMapper`를 주입받아 `objectMapper.readTree(header).get("kid").asText()` 사용

### 3-2. UserService ID/UUID 메서드 중복 제거
- **파일**: `services/auth-service/.../user/service/UserService.java`
- **중복 쌍**:
  - `getMyProfile(Long)` / `getMyProfileByUuid(String)` (Line 106-121)
  - `updateProfile(Long, ...)` / `updateProfileByUuid(String, ...)` (Line 126-147)
  - `setUsername(Long, ...)` / `setUsernameByUuid(String, ...)` (Line 152-200)
  - `changePassword(Long, ...)` / `changePasswordByUuid(String, ...)` (Line 218-230)
- **근거**: 모든 UUID 메서드는 `findUserByUuidOrThrow()` → user lookup 후 동일 로직 수행. JWT subject가 UUID이므로 외부에서는 UUID만 사용. Long ID 기반 메서드는 내부 호출에서만 필요
- **해결**: UUID 기반 메서드로 통일. Long ID 버전 중 외부에서 호출되지 않는 것 제거. 또는 private `resolveUser(Long id)` / `resolveUser(String uuid)` → 공통 비즈니스 로직 호출 패턴

### 3-3. ProfileController - 불필요한 이중 DB 조회 제거
- **파일**: `services/auth-service/.../user/controller/ProfileController.java`
- **Line 140-145**: `getCurrentUserId()`가 UUID → DB 조회 → Long ID 반환
- **Line 50-55**: `getMyProfile()`에서 Long ID → `ProfileService.getProfile(Long)` → 또 DB 조회
- **근거**: 매 인증 요청마다 같은 사용자를 2번 DB 조회. JWT subject는 UUID인데, Long ID로 변환 후 다시 조회하는 비효율적 패턴
- **해결**: `ProfileService`가 UUID 직접 수용하도록 변경. `getCurrentUserId()` → `getCurrentUserUuid()` 통일

### 3-4. AuthErrorCode A005/A006/A007 - Gateway 에러코드 충돌 정리
- **auth-service**:
  - A005 = Invalid token
  - A006 = Social user cannot change password
  - A007 = Current password is incorrect
- **gateway** (commit `4cea410`에서 수정):
  - A005 = Token revoked
  - A006 = Token expired
  - A007 = Invalid token
- **근거**: 동일 prefix "A"를 두 서비스에서 사용. 프론트엔드에서 에러코드 기반 처리 시 혼란 야기
- **해결 방안**: Gateway JWT 에러코드에 별도 prefix 부여 (예: `GW-A005`, `GW-A006`, `GW-A007`) 또는 auth-service 에러코드 번호 재배치. Gateway가 인증 에러를 프록시할 때 구분 가능해야 함

---

## 커밋 전략

1. `fix(auth): optimize Redis blacklist key with SHA-256 hash` (1-1)
2. `fix(auth): derive expiresIn from JWT config instead of hardcoding` (1-2)
3. `fix(auth): remove default profile from DataInitializer and add RBAC init` (1-3)
4. `fix(auth): add password validation in ProfileService.changePassword` (1-4)
5. `chore(auth): remove dead JwtConfig and unify to JwtProperties` (2-1)
6. `chore(auth): remove unused LoginController` (2-2, 사용 여부 확인 후)
7. `refactor(auth): extract CookieHelper to deduplicate cookie logic` (2-3)
8. `refactor(auth): improve kid parsing with ObjectMapper` (3-1)
9. `refactor(auth): remove ID/UUID method duplication in UserService` (3-2)
10. `refactor(auth): eliminate double DB query in ProfileController` (3-3)
11. `refactor(auth): resolve error code conflict with gateway` (3-4)

## Tier 4: 전체 검수 (리팩토링 마지막 단계)

### 4-1. JWT/UUID 기반 사용자 식별 구조 전체 검수
- **범위**: auth-service, blog-service, shopping-service, 프론트엔드 전체
- **문제**: UUID를 외부 노출용 식별자로 설계했으나, 각 서비스에서 `authorName`, `nickname` 등을 JWT에서 직접 꺼내 사용하는 패턴이 산재
- **목표**: UUID 기반 조회를 일관되게 적용하고, JWT claims에 불필요하게 포함된 정보 정리
- **시점**: 모든 리팩토링 완료 후 마지막에 전체 서비스 대상으로 진행

## 검증
- `cd services/auth-service && ./gradlew build` 컴파일 확인
- Docker 환경에서 로그인/토큰 갱신/로그아웃 흐름 테스트
- DataInitializer 변경 후 테스트 사용자 RBAC 초기화 확인
