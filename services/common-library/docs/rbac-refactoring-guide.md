# RBAC 리팩토링 구현 가이드 - Common Library

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- `GatewayAuthenticationFilter`: X-User-Id, X-User-Roles 헤더 → SecurityContext 설정
- `List.of(new SimpleGrantedAuthority(roles))` → 단일 Authority
- `JwtAuthenticationConverterAdapter` / `ReactiveJwtAuthenticationConverterAdapter`
- 공통 ErrorCode: `UNAUTHORIZED (C005)`, `FORBIDDEN (C004)`
- Kafka 이벤트 DTO: `UserSignedUpEvent`

## 변경 목표
- `GatewayAuthenticationFilter` 확장: 복수 Authority + Membership 컨텍스트
- `PermissionResolver` 신규 구현: Role+Membership → Permission Set (Redis 캐시)
- 공통 Permission 관련 인터페이스/어노테이션
- Kafka 이벤트 DTO 추가
- 공통 ErrorCode 확장

---

## 구현 단계

### Phase 2: GatewayAuthenticationFilter 확장

#### 복수 Authority 지원
```
Before: X-User-Roles: ROLE_USER
        → List.of(new SimpleGrantedAuthority(roles))

After:  X-User-Roles: ROLE_USER,ROLE_SELLER
        → Arrays.stream(roles.split(","))
              .map(String::trim)
              .map(SimpleGrantedAuthority::new)
              .toList()
```

#### Membership 컨텍스트 설정
```
X-User-Memberships: {"shopping":"PREMIUM","blog":"FREE"}
→ MembershipContext.set(parsedMap)
→ ThreadLocal 또는 RequestAttribute로 전달
```

### Phase 3: PermissionResolver 구현

#### 핵심 인터페이스
```java
public interface PermissionResolver {
    Set<String> resolvePermissions(List<String> roles, Map<String, String> memberships);
    boolean hasPermission(String permissionKey);
    boolean hasAnyPermission(String... permissionKeys);
}
```

#### Redis 캐시 전략
```
Key: perm:{sha256(roles_sorted)}:{sha256(memberships_sorted)}
Value: Set<String> (Permission keys)
TTL: 300초 (5분)

Cache Miss → DB 조회 → 캐시 저장
Redis 장애 → DB Fallback + Circuit Breaker
```

#### SUPER_ADMIN 바이패스
```
if roles.contains("ROLE_SUPER_ADMIN"):
    return WILDCARD → 모든 Permission 체크 true
```

### Phase 3: Kafka 이벤트 DTO 추가

#### 신규 이벤트
```java
// event/RoleChangedEvent.java
public record RoleChangedEvent(
    String eventId,
    String userId,
    String action,          // ASSIGNED, REVOKED
    String roleKey,
    List<String> currentRoles,
    String changedBy,
    Instant timestamp
) {}

// event/PermissionMappingChangedEvent.java
public record PermissionMappingChangedEvent(
    String eventId,
    String roleKey,
    String action,          // ADDED, REMOVED
    String permissionKey,
    String changedBy,
    Instant timestamp
) {}

// event/MembershipChangedEvent.java
public record MembershipChangedEvent(
    String eventId,
    String userId,
    String serviceName,
    String previousTier,
    String newTier,
    String action,          // CREATED, UPGRADED, DOWNGRADED, EXPIRED, RENEWED
    Instant expiresAt,
    Instant timestamp
) {}

// event/TokenRevokedEvent.java
public record TokenRevokedEvent(
    String eventId,
    String userId,
    String reason,          // SECURITY_BREACH, ADMIN_ACTION, PASSWORD_CHANGED
    String revokedBy,
    Instant timestamp
) {}
```

#### 기존 이벤트 확장
```java
// event/UserSignedUpEvent.java 확장
public record UserSignedUpEvent(
    String userId,
    String email,
    String name,
    List<String> roles,                    // NEW
    Map<String, String> defaultMemberships // NEW: {"blog":"FREE","shopping":"FREE"}
) {}
```

### Phase 3: 공통 Permission 어노테이션 (선택)

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();               // e.g., "shopping:product:create"
    LogicType logic() default LogicType.ANY;  // ANY or ALL
}
```

### Phase 3: ErrorCode 확장

```java
// exception/CommonErrorCode.java 추가
ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "C010", "Role not found"),
PERMISSION_DENIED(HttpStatus.FORBIDDEN, "C011", "Permission denied"),
MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "C012", "Membership not found"),
MEMBERSHIP_EXPIRED(HttpStatus.FORBIDDEN, "C013", "Membership expired"),
SELLER_APPLICATION_PENDING(HttpStatus.CONFLICT, "C014", "Seller application already pending"),
SELLER_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "C015", "Seller application not found"),
```

---

## 영향받는 파일

### 신규 생성
```
src/main/java/.../security/filter/EnhancedGatewayAuthenticationFilter.java
src/main/java/.../security/permission/PermissionResolver.java
src/main/java/.../security/permission/RedisPermissionResolver.java
src/main/java/.../security/permission/MembershipContext.java
src/main/java/.../security/annotation/RequirePermission.java (선택)
src/main/java/.../security/annotation/RequirePermissionAspect.java (선택)

src/main/java/.../event/RoleChangedEvent.java
src/main/java/.../event/PermissionMappingChangedEvent.java
src/main/java/.../event/MembershipChangedEvent.java
src/main/java/.../event/TokenRevokedEvent.java
```

### 수정 필요
```
src/main/java/.../security/filter/GatewayAuthenticationFilter.java
  - 복수 Authority 파싱 (콤마 구분)
  - X-User-Memberships 헤더 파싱

src/main/java/.../exception/CommonErrorCode.java
  - RBAC 관련 에러코드 추가

src/main/java/.../event/UserSignedUpEvent.java
  - roles, defaultMemberships 필드 추가
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] GatewayAuthenticationFilter: 복수 roles 파싱 (콤마 구분)
- [ ] GatewayAuthenticationFilter: memberships JSON 파싱
- [ ] GatewayAuthenticationFilter: 빈 roles/memberships 처리
- [ ] PermissionResolver: Role → Permission 매핑
- [ ] PermissionResolver: Role + Membership → 합산 Permission
- [ ] PermissionResolver: SUPER_ADMIN 와일드카드
- [ ] PermissionResolver: Redis 캐시 히트/미스
- [ ] PermissionResolver: Redis 장애 시 DB Fallback

### 통합 테스트
- [ ] 전체 흐름: X-User-Roles 헤더 → 복수 Authority → Permission 해석
- [ ] Kafka 이벤트 DTO 직렬화/역직렬화
