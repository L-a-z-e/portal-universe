# Auth Service Refactoring - Gap Analysis

## Overview
- **Feature**: auth-service-refactoring
- **Plan Document**: `docs/pdca/01-plan/features/auth-service-refactoring.plan.md`
- **Analysis Date**: 2026-02-01
- **Match Rate**: 100% (11/11 items fully implemented)

---

## Item-by-Item Analysis

### Tier 1: Security + Bug Fixes

#### 1-1. TokenBlacklistService Redis key SHA-256
- **Plan**: `BLACKLIST_PREFIX + SHA-256(token)` (64 chars fixed)
- **Implementation**: `hashToken()` method added using `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(hash)`
- **Applied to**: `addToBlacklist()` (Line 42), `isBlacklisted()` (Line 55)
- **Status**: ✅ PASS

#### 1-2. Hardcoded 900 expiresIn
- **Plan**: `jwtProperties.getAccessTokenExpiration() / 1000`
- **Implementation**:
  - `AuthController.login()`: `long expiresIn = jwtProperties.getAccessTokenExpiration() / 1000` ✅
  - `AuthController.refresh()`: same pattern ✅
  - `OAuth2AuthenticationSuccessHandler`: `"&expires_in=" + expiresIn` (dynamic) ✅
- **Status**: ✅ PASS (all 3 locations fixed)

#### 1-3. DataInitializer Security & Consistency
- **Plan**: Remove `default` profile, add RBAC initialization
- **Implementation**:
  - `@Profile({"local", "docker"})` (default removed) ✅
  - `@Order(2)` (after RbacDataMigrationRunner) ✅
  - `rbacInitializationService.initializeNewUser()` called for both users ✅
  - Admin user gets `ROLE_SUPER_ADMIN` in addition ✅
- **Status**: ✅ PASS

#### 1-4. ProfileService.changePassword PasswordValidator
- **Plan**: Add PasswordValidator + PasswordHistory to ProfileService.changePassword()
- **Implementation**:
  - `PasswordValidator` injected and `validate()` called ✅
  - `PasswordHistoryRepository` injected, history saved after change ✅
  - All validation errors joined with `String.join("; ", ...)` ✅
  - `GET /api/v1/auth/password-policy` endpoint added ✅
  - `PasswordPolicyResponse` DTO created ✅
  - Frontend `SignupPage.vue` shows password policy via "?" toggle ✅
  - `users.ts` API function `getPasswordPolicy()` added ✅
- **Status**: ✅ PASS

### Tier 2: Dead Code Removal

#### 2-1. JwtConfig.java Deletion + RefreshTokenService
- **Plan**: Delete JwtConfig, change RefreshTokenService to use JwtProperties
- **Implementation**:
  - `JwtConfig.java` deleted (file not found) ✅
  - `RefreshTokenService` imports `JwtProperties` (Line 3) ✅
  - Uses `jwtProperties.getRefreshTokenExpiration()` ✅
- **Status**: ✅ PASS

#### 2-2. LoginController + login.html Deletion
- **Plan**: Delete unused form-based login controller and template
- **Implementation**:
  - `LoginController.java` deleted (file not found) ✅
  - `login.html` deleted (file not found) ✅
- **Status**: ✅ PASS

#### 2-3. Cookie Management Logic Deduplication
- **Plan**: Extract CookieHelper, derive maxAge from jwtProperties
- **Implementation**:
  - `RefreshTokenCookieHelper.java` created with `setCookie()`, `clearCookie()` ✅
  - `maxAge = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())` (not hardcoded) ✅
  - `AuthController`: removed private cookie methods, uses `cookieHelper.setCookie/clearCookie` ✅
  - `OAuth2AuthenticationSuccessHandler`: same pattern ✅
  - `@Value` fields for cookie config centralized in helper ✅
  - `COOKIE_NAME` public static for `@CookieValue` annotation reference ✅
- **Status**: ✅ PASS

### Tier 3: Structural Improvements

#### 3-1. TokenService.extractKeyId JSON Parsing
- **Plan**: Replace indexOf-based parsing with ObjectMapper
- **Implementation**:
  - `private static final ObjectMapper objectMapper = new ObjectMapper()` ✅
  - `objectMapper.readTree(header).get("kid")` used ✅
  - indexOf/substring parsing removed ✅
- **Status**: ✅ PASS

#### 3-2. UserService ID/UUID Method Duplication
- **Plan**: Remove Long ID public methods, unify to UUID
- **Implementation**:
  - `getMyProfile(Long)` removed ✅
  - `updateProfile(Long, ...)` removed ✅
  - `setUsername(Long, ...)` removed ✅
  - `changePassword(Long, ...)` removed ✅
  - `findUserByIdOrThrow(Long)` helper removed ✅
  - UUID methods remain as primary interface ✅
- **Status**: ✅ PASS

#### 3-3. ProfileController Double DB Query
- **Plan**: ProfileService accepts UUID, remove getCurrentUserId()
- **Implementation**:
  - `ProfileService` all methods changed from `Long userId` to `String uuid` ✅
  - `findUserById(Long)` replaced with `findUserByUuid(String)` ✅
  - `ProfileController.getCurrentUserId()` removed ✅
  - All controller methods use `getCurrentUserUuid()` directly ✅
  - `updateProfile` returns `User` entity to avoid extra DB query for token ✅
  - `UserRepository` dependency removed from ProfileController ✅
  - **DB query reduction**: updateProfile 3 queries → 1 query ✅
- **Status**: ✅ PASS

#### 3-4. AuthErrorCode Gateway Conflict
- **Plan**: Gateway JWT error codes get distinct prefix
- **Implementation**:
  - `A005` → `GW-A005` (Token revoked) ✅
  - `A006` → `GW-A006` (Token expired) ✅
  - `A007` → `GW-A007` (Invalid token) ✅
  - auth-service error codes unchanged (source of truth) ✅
- **Status**: ✅ PASS

### Tier 4: Full Review

#### 4-1. JWT/UUID Structure Review
- **Plan**: Review UUID-based identification across all services
- **Findings**:
  - Gateway → downstream header forwarding (X-User-Id, X-User-Nickname, etc.) is standard microservice pattern ✅
  - Blog service `authorName` snapshot at creation time is reasonable design ✅
  - Frontend JWT decoding for logged-in user display is necessary ✅
  - **Actual issues were in auth-service internal code** (3-2, 3-3) — resolved ✅
- **Status**: ✅ PASS (no additional changes needed beyond 3-2/3-3)

---

## Deleted Files Verification

| File | Expected | Actual |
|------|----------|--------|
| `JwtConfig.java` | Deleted | ✅ Not found |
| `LoginController.java` | Deleted | ✅ Not found |
| `login.html` | Deleted | ✅ Not found |

## Created Files

| File | Purpose |
|------|---------|
| `RefreshTokenCookieHelper.java` | Centralized cookie management |
| `PasswordPolicyResponse.java` | Password policy API DTO |

## Build Verification

- `./gradlew build -x test` — BUILD SUCCESSFUL ✅
- All services compile without errors ✅

---

## Summary

| Metric | Value |
|--------|-------|
| Total Items | 11 (+1 full review) |
| Passed | 11/11 |
| Failed | 0 |
| Match Rate | **100%** |
| Iteration Needed | No |

**Recommendation**: Proceed to Report phase (`/pdca report auth-service-refactoring`)
