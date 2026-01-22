# User Entity 스키마

## 개요

Portal Universe auth-service의 사용자 정보는 두 개의 엔티티로 분리되어 관리됩니다. User는 핵심 인증 정보를, UserProfile은 부가적인 프로필 정보를 담당합니다. 이러한 분리를 통해 관심사의 분리(Separation of Concerns)를 달성합니다.

## 엔티티 관계도

```
┌─────────────────────────────────────────────────────────────┐
│                        User (Core Identity)                  │
├─────────────────────────────────────────────────────────────┤
│  id (PK)                                                     │
│  uuid (Unique) ─────────────────────────────┐               │
│  email (Unique)                              │               │
│  password (nullable for social)              │               │
│  role                                        │               │
│  status                                      │               │
│  lastLoginAt                                 │               │
│  createdAt                                   │               │
│  updatedAt                                   │               │
└──────────┬───────────────────┬───────────────┘               │
           │                   │                                │
           │ 1:1               │ 1:N                            │
           ▼                   ▼                                │
┌──────────────────┐  ┌─────────────────────┐                  │
│   UserProfile    │  │   SocialAccount     │                  │
├──────────────────┤  ├─────────────────────┤                  │
│  userId (PK,FK)  │  │  id (PK)            │                  │
│  nickname        │  │  userId (FK)        │                  │
│  realName        │  │  provider           │                  │
│  username        │  │  providerId         │                  │
│  bio             │  │  createdAt          │                  │
│  phoneNumber     │  └─────────────────────┘                  │
│  profileImageUrl │                                           │
│  website         │                         ┌─────────────────┘
│  marketingAgree  │                         │
└──────────────────┘                         │
                                             │
                           ┌─────────────────┘
                           │
                           ▼
              ┌─────────────────────┐
              │      Follow         │
              ├─────────────────────┤
              │  id (PK)            │
              │  followerId (FK)    │
              │  followingId (FK)   │
              │  createdAt          │
              └─────────────────────┘
```

## User Entity

### 소스 코드

```java
package com.portal.universe.authservice.domain;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // 외부 노출용 식별자 (UUID v7 권장되나 편의상 기본 UUID 전략 사용)
    @UuidGenerator
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // 소셜 로그인의 경우 null 가능
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 1:1 관계 - UserProfile
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, optional = false)
    private UserProfile profile;

    // 1:N 관계 - SocialAccount
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    // 생성자
    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    // 비즈니스 메서드
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void markForWithdrawal() {
        this.status = UserStatus.WITHDRAWAL_PENDING;
    }

    public boolean isSocialUser() {
        return this.password == null && !this.socialAccounts.isEmpty();
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}
```

### 필드 설명

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | Long | PK, Auto | 내부 식별자 (DB 전용) |
| `uuid` | String | Unique, Not Null | 외부 노출용 식별자 |
| `email` | String | Unique, Not Null | 로그인 ID |
| `password` | String | Nullable | BCrypt 해시 (소셜 로그인 시 null) |
| `role` | Role | Not Null | 사용자 권한 |
| `status` | UserStatus | Not Null | 계정 상태 |
| `lastLoginAt` | LocalDateTime | Nullable | 마지막 로그인 시간 |
| `createdAt` | LocalDateTime | Not Null | 생성 시간 |
| `updatedAt` | LocalDateTime | Nullable | 수정 시간 |

### Role Enum

```java
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;
}
```

### UserStatus Enum

```java
public enum UserStatus {
    ACTIVE,              // 정상 활성 상태
    INACTIVE,            // 비활성화 (휴면)
    WITHDRAWAL_PENDING,  // 탈퇴 대기
    BANNED               // 정지
}
```

## UserProfile Entity

### 소스 코드

```java
package com.portal.universe.authservice.domain;

@Entity
@Table(name = "user_profiles",
       indexes = @Index(name = "idx_username", columnList = "username", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    private Long userId;

    @MapsId // User 엔티티의 PK를 이 엔티티의 PK이자 FK로 사용
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 50)
    private String realName;

    @Column(unique = true, length = 20)
    private String username;  // @username 형식 (설정 후 변경 불가)

    @Column(length = 200)
    private String bio;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String profileImageUrl;

    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    private boolean marketingAgree;

    // 일반 회원가입용 생성자
    public UserProfile(User user, String nickname, String realName, boolean marketingAgree) {
        this.user = user;
        this.nickname = nickname;
        this.realName = realName;
        this.marketingAgree = marketingAgree;
    }

    // 소셜 로그인용 생성자
    public UserProfile(User user, String nickname, String profileImageUrl) {
        this.user = user;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.marketingAgree = false;
    }

    // Update 메서드들
    public void setUsername(String username) {
        this.username = username;
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public void updateProfile(String nickname, String bio,
                             String profileImageUrl, String website) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.website = website;
    }

    // 개별 필드 업데이트 메서드
    public void updateRealName(String realName) { this.realName = realName; }
    public void updatePhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void updateProfileImageUrl(String url) { this.profileImageUrl = url; }
    public void updateBio(String bio) { this.bio = bio; }
    public void updateWebsite(String website) { this.website = website; }
    public void updateMarketingAgree(boolean agree) { this.marketingAgree = agree; }
}
```

### 필드 설명

| 필드 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `userId` | Long | PK, FK | User와 공유하는 식별자 |
| `nickname` | String | Not Null, 50자 | 표시 이름 |
| `realName` | String | 50자 | 실명 |
| `username` | String | Unique, 20자 | @username 형식 |
| `bio` | String | 200자 | 자기소개 |
| `phoneNumber` | String | 20자 | 전화번호 |
| `profileImageUrl` | String | 255자 | 프로필 이미지 URL |
| `website` | String | 255자 | 개인 웹사이트 |
| `marketingAgree` | boolean | Not Null | 마케팅 동의 여부 |

## SocialAccount Entity

```java
@Entity
@Table(name = "social_accounts",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"provider", "provider_id"}
       ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public SocialAccount(User user, SocialProvider provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }
}
```

### SocialProvider Enum

```java
public enum SocialProvider {
    GOOGLE,
    NAVER,
    KAKAO,
    GITHUB
}
```

## 데이터베이스 스키마

### DDL (MySQL)

```sql
-- Users 테이블
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_uuid (uuid)
);

-- User Profiles 테이블
CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    real_name VARCHAR(50),
    username VARCHAR(20) UNIQUE,
    bio VARCHAR(200),
    phone_number VARCHAR(20),
    profile_image_url VARCHAR(255),
    website VARCHAR(255),
    marketing_agree BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_username (username)
);

-- Social Accounts 테이블
CREATE TABLE social_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_provider_provider_id (provider, provider_id),
    INDEX idx_user_id (user_id)
);
```

## 설계 결정 사항

### 1. User와 UserProfile 분리 이유

| 항목 | User | UserProfile |
|------|------|-------------|
| 변경 빈도 | 낮음 | 높음 |
| 민감도 | 높음 (인증 정보) | 중간 (개인 정보) |
| 조회 패턴 | 인증 시 | UI 표시 시 |

### 2. UUID vs Auto Increment ID

```
외부 노출: UUID 사용
- 예측 불가능
- 다른 사용자 정보 추측 방지
- 분산 시스템에서 충돌 없음

내부 처리: Auto Increment ID 사용
- 조인 성능 우수
- 인덱스 효율적
- 저장 공간 절약
```

### 3. Soft Delete 전략

```java
// 즉시 삭제 대신 상태 변경
public void markForWithdrawal() {
    this.status = UserStatus.WITHDRAWAL_PENDING;
}

// 스케줄러로 30일 후 실제 삭제
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
public void processWithdrawals() {
    List<User> pendingUsers = userRepository
        .findByStatusAndUpdatedAtBefore(
            UserStatus.WITHDRAWAL_PENDING,
            LocalDateTime.now().minusDays(30)
        );
    userRepository.deleteAll(pendingUsers);
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/User.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/UserProfile.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/SocialAccount.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/Role.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/UserStatus.java`

## 참고 자료

- [JPA Best Practices](https://docs.spring.io/spring-data/jpa/reference/)
- [Domain-Driven Design Entity Pattern](https://martinfowler.com/bliki/EvansClassification.html)
