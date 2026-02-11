---
id: arch-system-overview
title: Auth Service System Overview
type: architecture
status: current
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [architecture, auth, jwt, redis, rbac, security]
related:
  - arch-data-flow
  - arch-security-mechanisms
  - api-auth
---

# Auth Service System Overview

## 개요

Auth Service는 Portal Universe의 중앙 인증/인가 서비스로, JWT Stateless + Redis 기반 하이브리드 아키텍처를 채택하여 확장성과 보안을 동시에 달성합니다.

| 항목 | 내용 |
|------|------|
| 범위 | Service |
| 주요 기술 | Spring Boot 3.x, Spring Security, JWT (HMAC-SHA256), Redis, MySQL, Kafka |
| 배포 환경 | Docker Compose, Kubernetes |
| 관련 서비스 | API Gateway, notification-service, shopping-service, blog-service |

## 핵심 특징

1. **JWT + Redis 하이브리드 인증**: Access Token Stateless + Refresh Token/Blacklist Redis 조합으로 확장성과 즉시 무효화 기능을 모두 확보
2. **RBAC (Role-Based Access Control)**: 6개 엔티티 기반 역할/권한 관리 시스템으로 세밀한 접근 제어
3. **멤버십 시스템**: 서비스별 티어 관리 (shopping/blog, FREE/PREMIUM/VIP)로 차등화된 기능 제공
4. **소셜 로그인**: Google, Naver, Kakao OAuth2 지원, 조건부 활성화로 유연한 구성
5. **Kafka 이벤트 발행**: user-signup 토픽으로 TransactionalEventListener AFTER_COMMIT 기반 안전한 이벤트 전파

## 서비스 정보

| 항목 | 값 |
|------|-----|
| Port | 8081 |
| Base Path | /api/v1 |
| Gateway Route | /api/v1/auth/**, /api/v1/users/** |
| Health Check | /actuator/health |
| Profiles | local, docker, kubernetes |

## 아키텍처 다이어그램

```mermaid
graph TD
    Client[Client Application]
    Gateway[API Gateway]
    Auth[Auth Service]

    subgraph Auth Service
        Security[Security Layer<br/>GatewayAuthenticationFilter (common-library)<br/>SecurityConfig]
        AuthLayer[Auth Layer<br/>AuthController<br/>TokenService<br/>RefreshTokenService<br/>TokenBlacklistService]
        UserLayer[User Layer<br/>UserController<br/>UserService<br/>ProfileService<br/>ProfileController]
        OAuth2Layer[OAuth2 Layer<br/>CustomOAuth2UserService<br/>SuccessHandler]
        RBACLayer[RBAC Layer<br/>RbacService<br/>MembershipService<br/>SellerApplicationService]
    end

    MySQL[(MySQL<br/>14 Tables)]
    Redis[(Redis<br/>RT, Blacklist<br/>Login Attempts)]
    Kafka[Kafka<br/>user-signup]

    Client --> Gateway
    Gateway --> Auth
    Auth --> Security
    Security --> AuthLayer
    Security --> UserLayer
    Security --> OAuth2Layer
    Security --> RBACLayer

    AuthLayer --> MySQL
    UserLayer --> MySQL
    RBACLayer --> MySQL
    AuthLayer --> Redis
    UserLayer --> Kafka
```

## 컴포넌트 상세

### Controller (10개)

Auth Service는 인증, 사용자 관리, RBAC, 멤버십, 셀러 관리를 위한 10개의 Controller를 제공합니다.

| Controller | Base Path | 역할 |
|------------|-----------|------|
| AuthController | /api/v1/auth | 로그인, 토큰 갱신, 로그아웃, 비밀번호 정책 조회 |
| UserController | /api/v1/users | 회원가입, 프로필 조회 |
| ProfileController | /api/v1/profile | 내 프로필 관리, 비밀번호 변경, 계정 탈퇴 |
| FollowController | /api/v1/users | 팔로우/언팔로우, 팔로워/팔로잉 목록 |
| PermissionController | /api/v1/permissions | 내 권한 조회 |
| MembershipController | /api/v1/memberships | 내 멤버십 조회/변경 |
| RbacAdminController | /api/v1/admin/rbac | [SUPER_ADMIN] 역할 관리 |
| MembershipAdminController | /api/v1/admin/memberships | [SUPER_ADMIN] 멤버십 관리 |
| SellerController | /api/v1/seller | 셀러 신청/조회 |
| SellerAdminController | /api/v1/admin/seller | [SHOPPING_ADMIN, SUPER_ADMIN] 셀러 심사 |

### Service (13개+)

#### 인증 및 토큰 관리

| Service | 역할 |
|---------|------|
| TokenService | JWT Access Token/Refresh Token 생성 및 검증 (HMAC-SHA256, kid 기반 Key Rotation 지원) |
| RefreshTokenService | Redis에 Refresh Token 저장/조회/삭제, Lua Script를 통한 원자적 Rotation 처리 |
| TokenBlacklistService | Redis 기반 Access Token 블랙리스트 관리 (SHA-256 해시 키 사용) |
| LoginAttemptServiceImpl | Redis 기반 로그인 시도 추적, 단계적 계정 잠금 (3/5/10회) |
| CustomUserDetailsService | Spring Security UserDetailsService 구현, DB에서 사용자 정보 로드 |

#### RBAC 및 멤버십

| Service | 역할 |
|---------|------|
| RbacService | 역할 관리, 권한 Resolution (역할 권한 + 멤버십 티어 권한 합산) |
| RbacInitializationService | 신규 사용자 RBAC 초기화 (ROLE_USER + FREE 멤버십 자동 할당) |
| RbacDataMigrationRunner | 기존 사용자 RBAC 데이터 마이그레이션 (CommandLineRunner) |
| MembershipService | 멤버십 티어 조회/변경/취소, 서비스별(shopping/blog) 티어 관리 |
| SellerApplicationService | 셀러 신청/심사 워크플로우, 승인 시 ROLE_SELLER 자동 할당 |

#### 사용자 관리

| Service | 역할 |
|---------|------|
| UserService | 회원가입, 프로필 관리, 비밀번호 변경, Username 설정 |
| ProfileService | 프로필 조회/수정, 계정 탈퇴 |
| FollowService | 팔로우/언팔로우, 팔로워/팔로잉 목록 조회 |

#### OAuth2 및 비밀번호 정책

- **CustomOAuth2UserService**: OAuth2 소셜 로그인 사용자 처리 (Google, Naver, Kakao)
- **OAuth2AuthenticationSuccessHandler**: OAuth2 로그인 성공 후 JWT 발급 및 프론트엔드 리다이렉트
- **PasswordValidatorImpl**: 10가지 비밀번호 정책 검증 (길이, 복잡도, 재사용 금지 등)

### Repository (13개)

Auth Service는 14개 이상의 엔티티를 관리하기 위한 JPA Repository를 제공합니다.

| Repository | 엔티티 | 주요 역할 |
|------------|--------|----------|
| UserRepository | User | 사용자 핵심 정보 CRUD |
| UserProfileRepository | UserProfile | 프로필 정보 CRUD |
| PasswordHistoryRepository | PasswordHistory | 비밀번호 변경 이력 관리 |
| FollowRepository | Follow | 팔로우 관계 관리 |
| SocialAccountRepository | SocialAccount | 소셜 로그인 연동 정보 |
| RoleEntityRepository | RoleEntity | 역할 정의 관리 |
| UserRoleRepository | UserRole | 사용자-역할 매핑 |
| PermissionRepository | PermissionEntity | 권한 정의 관리 |
| RolePermissionRepository | RolePermission | 역할-권한 매핑 |
| MembershipTierRepository | MembershipTier | 멤버십 티어 정의 |
| MembershipTierPermissionRepository | MembershipTierPermission | 티어-권한 매핑 |
| UserMembershipRepository | UserMembership | 사용자-멤버십 매핑 |
| AuthAuditLogRepository | AuthAuditLog | 감사 로그 저장 |
| SellerApplicationRepository | SellerApplication | 셀러 신청서 관리 |

## 데이터 저장소

### MySQL 엔티티 (14개+)

Auth Service는 사용자, 인증, RBAC, 멤버십을 위한 14개 이상의 MySQL 테이블을 사용합니다.

| 엔티티 | 테이블 | 역할 |
|--------|--------|------|
| User | users | 사용자 핵심 정보 (email, password, uuid, status, 잠금 상태) |
| UserProfile | user_profiles | 프로필 (nickname, username, bio, image) |
| SocialAccount | social_accounts | 소셜 로그인 연동 (provider, providerId) |
| PasswordHistory | password_history | 비밀번호 변경 이력 (재사용 방지) |
| Follow | follows | 팔로우 관계 (follower_id, following_id) |
| RoleEntity | roles | 역할 정의 (ROLE_USER, ROLE_SELLER, ROLE_ADMIN 등) |
| PermissionEntity | permissions | 권한 정의 (READ_POST, CREATE_PRODUCT 등) |
| UserRole | user_roles | 사용자-역할 매핑 (M:N) |
| RolePermission | role_permissions | 역할-권한 매핑 (M:N) |
| MembershipTier | membership_tiers | 멤버십 티어 정의 (서비스별: shopping/blog) |
| MembershipTierPermission | membership_tier_permissions | 티어-권한 매핑 (M:N) |
| UserMembership | user_memberships | 사용자-멤버십 매핑 (서비스별 현재 티어) |
| AuthAuditLog | auth_audit_log | 감사 로그 (로그인 성공/실패, 권한 변경 등) |
| SellerApplication | seller_applications | 셀러 신청서 (상태: PENDING/APPROVED/REJECTED) |

#### ERD 주요 관계

- User 1:1 UserProfile
- User 1:N SocialAccount (다중 소셜 계정 연동 가능)
- User M:N RoleEntity (via UserRole)
- RoleEntity M:N PermissionEntity (via RolePermission)
- User M:N MembershipTier (via UserMembership, 서비스별)
- MembershipTier M:N PermissionEntity (via MembershipTierPermission)

### Redis 키 패턴

Redis는 Refresh Token, Access Token Blacklist, 로그인 시도 추적에 사용됩니다.

| 키 패턴 | 값 타입 | TTL | 용도 |
|---------|---------|-----|------|
| refresh_token:{userId} | String (JWT) | 7일 | Refresh Token 저장 |
| blacklist:{sha256Hash} | String ("blacklisted") | AT 남은 만료 시간 | Access Token 블랙리스트 |
| login_attempt:count:{ip:email} | Integer (실패 횟수) | 1시간 | 로그인 실패 추적 |
| login_attempt:lock:{ip:email} | String (Unix Timestamp) | 잠금 시간 (1분/5분/15분) | 계정 잠금 |

#### Redis 운영 특징

- **Refresh Token Rotation**: Lua Script를 사용한 원자적 GET-DELETE-SET 작업으로 재사용 공격 방지
- **Blacklist TTL**: Access Token의 남은 만료 시간만큼만 블랙리스트 유지하여 메모리 효율성 확보
- **단계적 계정 잠금**: 로그인 실패 횟수에 따라 1분 → 5분 → 15분 단계적 잠금 시간 증가

## 에러 코드 체계

Auth Service는 42개의 커스텀 에러 코드를 정의하여 클라이언트에게 명확한 오류 정보를 제공합니다.

### 인증 관련 (A001-A009)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A001 | 이미 존재하는 이메일입니다 | 409 CONFLICT |
| A002 | 이메일 또는 비밀번호가 일치하지 않습니다 | 401 UNAUTHORIZED |
| A003 | Refresh Token이 유효하지 않습니다 | 401 UNAUTHORIZED |
| A004 | 존재하지 않는 사용자입니다 | 404 NOT_FOUND |
| A005 | 유효하지 않은 소셜 로그인 제공자입니다 | 400 BAD_REQUEST |
| A006 | 이미 연동된 소셜 계정입니다 | 409 CONFLICT |
| A007 | 존재하지 않는 소셜 계정입니다 | 404 NOT_FOUND |
| A008 | 이메일 인증이 필요합니다 | 403 FORBIDDEN |
| A009 | 이메일 인증 토큰이 만료되었습니다 | 400 BAD_REQUEST |

### Username 관련 (A011-A013)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A011 | 이미 사용 중인 사용자명입니다 | 409 CONFLICT |
| A012 | 사용자명은 한 번만 설정할 수 있습니다 | 400 BAD_REQUEST |
| A013 | 사용자명은 3-20자의 영문, 숫자, 언더스코어만 가능합니다 | 400 BAD_REQUEST |

### 팔로우 관련 (A014-A017)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A014 | 이미 팔로우 중입니다 | 409 CONFLICT |
| A015 | 팔로우하지 않은 사용자입니다 | 400 BAD_REQUEST |
| A016 | 자기 자신을 팔로우할 수 없습니다 | 400 BAD_REQUEST |
| A017 | 팔로우 대상 사용자를 찾을 수 없습니다 | 404 NOT_FOUND |

### 계정 잠금 (A018-A019)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A018 | 로그인 시도 실패로 계정이 일시적으로 잠겼습니다 | 423 LOCKED |
| A019 | 로그인 시도 횟수를 초과했습니다 | 429 TOO_MANY_REQUESTS |

### 비밀번호 정책 (A020-A026)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A020 | 비밀번호는 최소 8자 이상이어야 합니다 | 400 BAD_REQUEST |
| A021 | 비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다 | 400 BAD_REQUEST |
| A022 | 최근 사용한 비밀번호는 재사용할 수 없습니다 | 400 BAD_REQUEST |
| A023 | 현재 비밀번호가 일치하지 않습니다 | 400 BAD_REQUEST |
| A024 | 비밀번호에 이메일 주소가 포함될 수 없습니다 | 400 BAD_REQUEST |
| A025 | 비밀번호에 연속된 문자가 포함될 수 없습니다 | 400 BAD_REQUEST |
| A026 | 비밀번호에 반복된 문자가 포함될 수 없습니다 | 400 BAD_REQUEST |

### RBAC 관련 (A030-A034)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A030 | 존재하지 않는 역할입니다 | 404 NOT_FOUND |
| A031 | 이미 할당된 역할입니다 | 409 CONFLICT |
| A032 | 시스템 역할은 수정할 수 없습니다 | 403 FORBIDDEN |
| A033 | 할당되지 않은 역할입니다 | 400 BAD_REQUEST |
| A034 | 존재하지 않는 권한입니다 | 404 NOT_FOUND |

### 멤버십 관련 (A035-A038)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A035 | 멤버십을 찾을 수 없습니다 | 404 NOT_FOUND |
| A036 | 멤버십 티어를 찾을 수 없습니다 | 404 NOT_FOUND |
| A037 | 이미 멤버십이 존재합니다 | 409 CONFLICT |
| A038 | 멤버십 다운그레이드는 허용되지 않습니다 | 400 BAD_REQUEST |

### 셀러 관련 (A040-A042)

| 코드 | 메시지 | HTTP 상태 |
|------|--------|----------|
| A040 | 이미 대기 중인 셀러 신청이 있습니다 | 409 CONFLICT |
| A041 | 셀러 신청을 찾을 수 없습니다 | 404 NOT_FOUND |
| A042 | 이미 처리된 셀러 신청입니다 | 400 BAD_REQUEST |

모든 에러 코드는 `AuthErrorCode` enum에 정의되며, `CustomBusinessException`을 통해 발생합니다. 에러 응답은 `ApiResponse` wrapper를 통해 일관된 형식으로 클라이언트에 전달됩니다.

## 외부 연동

### API Gateway

Auth Service는 API Gateway와 긴밀하게 통합되어 있습니다.

- **JWT 서명 키 공유**: HMAC 공유 시크릿을 통해 Gateway에서도 Access Token의 서명을 검증할 수 있습니다
- **Stateless 검증**: Gateway는 Redis 조회 없이 Access Token만으로 요청을 검증하고 서비스로 전달합니다
- **User Context 전달**: Gateway가 검증한 사용자 정보(userId, roles)를 헤더(`X-User-Id`, `X-User-Roles`)로 전달합니다

### OAuth2 Provider

Auth Service는 3개의 소셜 로그인 제공자를 지원합니다.

- **지원 Provider**: Google, Naver, Kakao
- **조건부 활성화**: `ClientRegistrationRepository` Bean이 있을 때만 OAuth2 로그인 활성화
- **유연한 구성**: 설정이 없는 환경(예: 테스트)에서도 서비스가 정상적으로 기동됩니다
- **자동 계정 연동**: 소셜 로그인 시 기존 계정이 있으면 연동, 없으면 신규 계정 생성

### Kafka

Auth Service는 사용자 가입 이벤트를 Kafka로 발행합니다.

- **토픽**: `user-signup`
- **이벤트**: `UserSignedUpEvent` (userId, email, nickname)
- **발행 시점**: `@TransactionalEventListener(phase = AFTER_COMMIT)` - 트랜잭션 커밋 후에만 발행
- **소비자**: notification-service (환영 이메일, SMS 발송)
- **실패 처리**: Kafka 발행 실패 시 로그 기록, 사용자 가입 트랜잭션은 롤백되지 않음

## 기술적 결정

Auth Service의 핵심 설계 결정은 다음 ADR 문서에서 확인할 수 있습니다.

### ADR-008: JWT Stateless + Redis 인증 아키텍처

- **결정**: Access Token Stateless + Refresh Token/Blacklist Redis 하이브리드
- **배경**: 기존 OIDC Authorization Code Flow에서 전환
- **근거**:
  - Access Token 검증 시 DB/Redis 조회 불필요 → 수평 확장 용이
  - Refresh Token은 Redis에 저장하여 강제 로그아웃 지원
  - Blacklist로 Access Token 즉시 무효화 가능
- **트레이드오프**: Redis 의존성 증가 vs 확장성 및 즉시 무효화 기능 확보
- **상세**: [ADR-008](../../adr/ADR-008-jwt-stateless-redis.md)

### ADR-003: 심층 방어 (Defense in Depth)

- **결정**: Frontend Route Guard + Backend @PreAuthorize 조합
- **4계층 방어**:
  1. Frontend Route Guard (사용자 경험)
  2. API Gateway JWT 검증 (첫 번째 방어선)
  3. Backend Service @PreAuthorize (두 번째 방어선)
  4. Business Logic 권한 체크 (최종 방어선)
- **근거**: 단일 계층 방어의 취약성 방지, 보안 심도 강화
- **상세**: [ADR-003](../../adr/ADR-003-authorization-strategy.md)

### 기타 설계 결정

- **비밀번호 해싱**: BCrypt (work factor 10)
- **JWT 알고리즘**: HMAC-SHA256 (대칭키), kid 기반 Key Rotation 지원
- **Access Token TTL**: 1시간
- **Refresh Token TTL**: 7일
- **로그인 시도 제한**: 3회/5회/10회 단계적 잠금 (1분/5분/15분)
- **비밀번호 재사용 방지**: 최근 5개 비밀번호 이력 보관

## 배포 및 확장

### 환경별 차이

Auth Service는 local, docker, kubernetes 세 가지 프로파일을 지원합니다.

| 항목 | local | docker | kubernetes |
|------|-------|--------|------------|
| DB | localhost MySQL:3306 | docker-compose MySQL | RDS 또는 StatefulSet |
| Redis | localhost:6379 | docker-compose Redis | ElastiCache 또는 StatefulSet |
| OAuth2 | .env.local 설정 시 활성화 | 환경변수로 주입 | Secret으로 관리 |
| Kafka | localhost:9092 | docker-compose Kafka | MSK 또는 StatefulSet |
| 포트 | 8081 | 8081 | Service ClusterIP |

### 수평 확장 전략

Auth Service는 Stateless 아키텍처를 채택하여 수평 확장이 용이합니다.

#### 확장 가능 요소

- **Auth Service 인스턴스**: Access Token 검증이 Stateless이므로 인스턴스를 자유롭게 스케일 아웃할 수 있습니다
- **로드 밸런싱**: API Gateway 또는 Kubernetes Service가 라운드 로빈 방식으로 요청을 분산합니다
- **세션 공유 불필요**: 모든 상태(RT, Blacklist)는 Redis에 저장되므로 sticky session이 필요 없습니다

#### 병목 지점 및 대응

- **Redis 병목**: Refresh Token 조회/Blacklist 확인이 Redis에 의존
  - **대응**: Redis Cluster 또는 Sentinel 구성으로 HA 확보
  - **캐싱**: Blacklist 조회는 로컬 캐시(Caffeine) 적용 가능
- **MySQL 읽기 부하**: 사용자 프로필 조회, RBAC 권한 Resolution
  - **대응**: Read Replica 구성으로 읽기 부하 분산
  - **캐싱**: 사용자별 권한 정보를 Spring Cache(Redis 또는 Caffeine)로 캐싱
- **Kafka 처리 지연**: user-signup 이벤트 발행 실패 시 재시도 로직 필요
  - **대응**: Kafka Producer의 `retries` 설정 조정, Dead Letter Queue 구성

### 모니터링 및 알람

- **Actuator 엔드포인트**: `/actuator/health`, `/actuator/metrics`
- **주요 메트릭**:
  - 로그인 성공/실패율
  - Access Token 발급/검증 시간
  - Refresh Token Rotation 성공/실패율
  - 계정 잠금 발생 횟수
  - Kafka 이벤트 발행 성공/실패율
- **알람 기준**:
  - 로그인 실패율 > 10%
  - Redis 연결 실패
  - MySQL 슬로우 쿼리 (> 1초)
  - Kafka 이벤트 발행 실패 > 5%

## 관련 문서

- **[Data Flow](./data-flow.md)**: 로그인, 토큰 갱신, 로그아웃 등 인증 플로우 상세 설명
- **[Security Mechanisms](./security-mechanisms.md)**: JWT, Redis Blacklist, 비밀번호 정책 등 보안 메커니즘 상세
- **[ADR-008: JWT Stateless + Redis](../../adr/ADR-008-jwt-stateless-redis.md)**: 인증 아키텍처 결정 배경 및 근거
- **[ADR-003: Admin 권한 검증 전략](../../adr/ADR-003-authorization-strategy.md)**: 심층 방어 전략 및 4계층 방어 구조
- **[Auth API 문서](../../api/auth-service/README.md)**: REST API 엔드포인트 명세 및 예제

## 변경 이력

| 날짜 | 작성자 | 변경 내용 |
|------|--------|----------|
| 2026-01-18 | Laze | 최초 작성 |
| 2026-02-06 | Laze | 전체 재작성: 컴포넌트 상세, 에러 코드, 배포 전략 추가 |
| 2026-02-12 | Laze | ADR-039 구현: JwtAuthenticationFilter → GatewayAuthenticationFilter 전환 | Laze |
