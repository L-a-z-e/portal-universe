# ADR-051: RBAC Cache Component 구조 분리

**Status**: Accepted
**Date**: 2026-02-27
**Author**: Laze

## Context

### 기존 상태

`RoleHierarchyService`가 `@Cacheable`/`@CacheEvict`와 비즈니스 로직(BFS 탐색, cycle detection)을 동일 클래스에서 처리하고 있었다.

```java
@Service
public class RoleHierarchyService {
    @Lazy private RoleHierarchyService self; // AOP proxy 자기 참조

    @Cacheable(value = "roleHierarchy", key = "'graph'")
    public Map<String, List<String>> getHierarchyGraph() { ... }

    public List<String> resolveEffectiveRoles(List<String> roleKeys) {
        Map<String, List<String>> graph = self.getHierarchyGraph(); // proxy 경유
        // BFS 탐색...
    }
}
```

### 문제점

1. **Lombok `@RequiredArgsConstructor` + `@Lazy` 비호환**: Lombok이 생성한 constructor에 `@Lazy`가 전파되지 않아, Spring이 lazy proxy를 생성하지 못하고 circular reference 발생
2. **AOP Self-Invocation 안티패턴**: 같은 클래스 내에서 `@Cacheable` 메서드를 호출하면 AOP proxy를 경유하지 않아 캐시가 동작하지 않음. `@Lazy` self-injection은 이 문제의 workaround이지만 근본 해결이 아님
3. **단일 책임 원칙 위반**: 캐시 관리와 비즈니스 로직(BFS, cycle detection)이 한 클래스에 혼재

### 검토한 대안

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|:---------:|
| **구조적 분리 (Cache Component)** | SRP 준수, AOP 문제 근본 해결, Lombok 호환 | 클래스 1개 추가 | **선택** |
| `@Lazy` setter injection | 최소 변경 | Lombok 비호환 우회일 뿐, 근본 원인 미해결 | - |
| `ObjectProvider<T>` | lazy resolution | 사용처마다 `.getIfAvailable()` 호출 필요, 가독성 저하 | - |
| `CacheManager` 직접 사용 | AOP 불필요 | Spring Cache abstraction 포기, 수동 캐시 관리 부담 | - |

## Decision

### Cache 전용 Component 분리

`RoleHierarchyCacheComponent`를 생성하여 `@Cacheable`/`@CacheEvict` 책임만 전담한다.

```
[RoleHierarchyService]
    ├── resolveEffectiveRoles()  ← BFS 탐색 로직
    ├── wouldCreateCycle()       ← Cycle detection 로직
    └── delegates to ↓

[RoleHierarchyCacheComponent]
    ├── @Cacheable getHierarchyGraph()  ← DB 조회 + 캐시
    └── @CacheEvict evictHierarchyCache()  ← 캐시 무효화
```

### 설계 원칙

1. **Cache Component**: DB 조회 + 캐시 관리만 담당. `@Transactional(readOnly = true)`
2. **Service**: 비즈니스 로직만 담당. Cache Component에서 그래프를 받아 BFS/cycle detection 수행
3. **Public API 유지**: `RoleHierarchyService`의 기존 public 메서드 시그니처 변경 없음 (하위 호환)
4. **`@Lazy` self-injection 제거**: 일반적인 constructor injection으로 교체

### 파일 구조

| 파일 | 역할 |
|------|------|
| `RoleHierarchyCacheComponent.java` | `@Cacheable` DB 조회, `@CacheEvict` 캐시 무효화 |
| `RoleHierarchyService.java` | BFS effective roles, cycle detection, pass-through 메서드 |

## Consequences

### 긍정적

- AOP self-invocation 문제 근본 해결 (별도 Bean이므로 proxy 정상 동작)
- Lombok `@RequiredArgsConstructor`와 완벽 호환
- 캐시 전략 변경 시 Cache Component만 수정
- 단일 책임 원칙 준수

### 부정적

- 클래스 1개 추가 (복잡도 미미한 증가)

### 적용 범위

이 패턴은 `@Cacheable` self-invocation이 필요한 모든 서비스에 동일하게 적용 가능하다. 향후 유사 패턴 발견 시 같은 방식으로 분리한다.

## Related

- [ADR-046: MySQL → PostgreSQL 마이그레이션](./ADR-046-mysql-to-postgresql-migration.md)
- `services/auth-service/src/main/java/.../auth/service/RoleHierarchyCacheComponent.java`
- `services/auth-service/src/main/java/.../auth/service/RoleHierarchyService.java`
