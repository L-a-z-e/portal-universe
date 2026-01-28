# Architecture Decision Records (ADR)

Portal Universe 프로젝트의 아키텍처 결정을 기록하는 ADR 문서입니다.

## ADR 목록

### ADR-001: Admin 컴포넌트 구조
**상태**: Accepted | **작성일**: 2026-01-17

Admin 페이지 UI를 구성하는 컴포넌트 구조를 3계층(Pages → Containers → UI Components)으로 설계합니다.

**결정 요약**:
- Pages: 라우트와 연결된 최상위 컴포넌트
- Containers: 비즈니스 로직 및 상태 관리 (React Query, React Hook Form)
- UI Components: 순수 프레젠테이션 컴포넌트

**파일**: [ADR-001-admin-component-structure.md](./ADR-001-admin-component-structure.md)

**영향 범위**:
- `frontend/shopping-frontend/src/` 구조
- Admin 페이지 개발 패턴
- UI 컴포넌트 재사용성

---

### ADR-002: Admin API 엔드포인트 설계
**상태**: Accepted | **작성일**: 2026-01-17

Admin API 엔드포인트 설계 방식을 결정합니다.

**결정 요약**:
- 기존 상품 CRUD API(`/api/shopping/product`)를 Admin에서도 재사용
- Controller 레벨에서 `@PreAuthorize("hasRole('ADMIN')")` 추가
- RequestBody Validation 및 ProductResponse 보완

**파일**: [ADR-002-api-endpoint-design.md](./ADR-002-api-endpoint-design.md)

**영향 범위**:
- `services/shopping-service/src/.../controller/ProductController.java`
- Admin과 고객용 API의 통일된 인터페이스
- 개발 기간 단축 (약 40% 이상)

**대안 검토**:
- ❌ Admin 전용 엔드포인트 분리: 코드 중복, 유지보수 비용 증가
- ✅ 기존 API 활용: 개발 효율성, 코드 중복 방지

---

### ADR-003: Admin 권한 검증 전략
**상태**: Accepted | **작성일**: 2026-01-17

권한 검증을 위한 심층 방어 전략을 수립합니다.

**결정 요약**:
- **Frontend Route Guard**: 권한 없는 페이지 접근 방지 (UX 보호)
- **API Gateway**: JWT 토큰 검증 (인증)
- **Backend @PreAuthorize**: 실제 권한 검증 (인가) - 최종 방어선
- **Business Logic**: Resource Owner 검증 (본인 확인)

**파일**: [ADR-003-authorization-strategy.md](./ADR-003-authorization-strategy.md)

**영향 범위**:
- `frontend/shopping-frontend/src/components/guards/`
- `services/shopping-service/src/.../config/SecurityConfig.java`
- Error handling 및 로깅 전략

**대안 검토**:
- ❌ Frontend만 검증: 보안 보장 없음, API 우회 가능
- ❌ Backend만 검증: UX 저하
- ✅ Frontend + Backend: 안전성 + 좋은 UX

---

### ADR-004: JWT RBAC 자동 설정 전략
**상태**: Accepted | **작성일**: 2026-01-19

각 마이크로서비스의 JWT RBAC 설정 중복을 해결하기 위한 자동 설정 전략을 수립합니다.

**결정 요약**:
- Common Library에 `JwtSecurityAutoConfiguration` 추가
- Servlet/Reactive 환경별로 자동으로 JWT 권한 변환기 Bean 등록
- `@ConditionalOnMissingBean`으로 서비스별 커스터마이징 허용

**파일**: [ADR-004-jwt-rbac-auto-configuration.md](./ADR-004-jwt-rbac-auto-configuration.md)

**영향 범위**:
- `services/common-library/.../security/config/JwtSecurityAutoConfiguration.java`
- 모든 마이크로서비스의 SecurityConfig 간소화
- 코드 중복 제거 및 일관된 보안 설정

**대안 검토**:
- ❌ 각 서비스마다 개별 구현: 코드 중복, 유지보수 비용 증가
- ⚠️ 유틸리티 클래스 제공: 부분적 개선
- ✅ Auto-Configuration: Zero Configuration, 환경별 자동 감지

**결과**:
- **긍정적**: 코드 중복 제거, 일관된 보안 설정, 신규 서비스 개발 속도 향상
- **부정적**: Common Library 결합도 증가 (완화 방안 포함)

---

### ADR-005: 민감 데이터 관리 전략
**상태**: Accepted | **작성일**: 2026-01-19

민감한 정보(DB 비밀번호, API 키 등)를 Git에 커밋하지 않기 위한 보안 전략을 수립합니다.

**결정 요약**:
- .env 파일 + .gitignore 방식 채택
- 템플릿 파일 제공 (.env.example, .env.docker.example, secret.yaml.example)
- 환경별(Local, Docker, K8s) 일관된 환경 변수 관리

**파일**: [ADR-005-sensitive-data-management.md](./ADR-005-sensitive-data-management.md)

**영향 범위**:
- `.gitignore` - 민감 파일 제외
- `.env.example`, `.env.docker.example` - 환경 변수 템플릿
- `k8s/base/secret.yaml.example` - Kubernetes Secret 템플릿
- `docker-compose.yml` - 환경 변수 사용

**대안 검토**:
- ✅ .env + .gitignore: 간단, 비용 없음 (채택)
- ❌ HashiCorp Vault: 인프라 운영 부담
- 🟡 AWS Secrets Manager: 프로덕션 환경 향후 검토
- ❌ Git-crypt: 관리 복잡도 높음

**다음 단계**:
- Pre-commit hook 추가
- 온보딩 문서 작성
- 프로덕션 환경에서 AWS Secrets Manager 검토

---

### ADR-006: Config Service 및 Discovery Service 제거
**상태**: Accepted | **작성일**: 2026-01-20

Spring Cloud Config Service와 Discovery Service(Eureka)를 제거하고 로컬 설정 파일 및 Kubernetes DNS로 전환합니다.

**결정 요약**:
- 각 서비스에 프로필별 설정 파일 직접 포함 (application-{profile}.yml)
- Kubernetes DNS를 사용하여 서비스 간 통신
- ConfigMap + Secret으로 환경 변수 관리

**파일**: [ADR-006-remove-config-service.md](./ADR-006-remove-config-service.md)

**영향 범위**:
- 모든 마이크로서비스의 설정 파일 구조
- Docker Compose 및 Kubernetes 배포
- CI/CD 파이프라인

**대안 검토**:
- ❌ Config Service 유지: 테스트 복잡성, K8s 중복
- ❌ Eureka 유지: K8s DNS와 중복
- ✅ 로컬 설정 + K8s DNS: 독립적 실행, K8s 네이티브

---

### ADR-007: 서비스 상태 모니터링 전략
**상태**: Accepted | **작성일**: 2026-01-21

Portal Shell에서 마이크로서비스의 Health 상태를 모니터링하는 전략을 수립합니다.

**결정 요약**:
- Client-Side Polling 방식 채택 (10초 간격)
- Spring Boot Actuator `/actuator/health` 엔드포인트 활용
- 인증 불필요 공개 API
- UP/DOWN/DEGRADED 3가지 상태

**파일**: [ADR-007-service-status-monitoring.md](./ADR-007-service-status-monitoring.md)

**영향 범위**:
- `frontend/portal-shell/src/pages/ServiceStatus.vue`
- `frontend/portal-shell/src/composables/useHealthCheck.ts`
- 모든 서비스의 `application.yml` (Actuator 설정)
- API Gateway CORS 설정

**대안 검토**:
- ✅ Polling: 구현 간단, 인프라 불필요 (채택)
- ❌ WebSocket: 구현 복잡, 서버 부하
- 🟡 Prometheus + Grafana: 프로덕션 환경 향후 검토

---

### ADR-008: 마이프로필 단계별 구현 전략
**상태**: Accepted | **작성일**: 2026-01-21

마이프로필 기능을 2단계(Phase)로 나누어 구현합니다.

**결정 요약**:
- **Phase 1 (즉시)**: 읽기 전용 프로필 - Auth Store 데이터 사용
- **Phase 2 (JWT 작업 후)**: 프로필 수정, 비밀번호 변경, 회원 탈퇴 - API 연동

**파일**: [ADR-008-my-profile-phased-approach.md](./ADR-008-my-profile-phased-approach.md)

**영향 범위**:
- `frontend/portal-shell/src/pages/MyProfilePage.vue` (신규 생성)
- JWT 작업 완료 후 Backend API 추가
- Auth Store 읽기 전용 사용

**대안 검토**:
- ❌ 한 번에 완성: JWT 작업 충돌 위험
- ✅ Phase 분리: 리스크 최소화 + 빠른 제공
- ❌ 읽기만 구현: 핵심 기능 누락

**결과**:
- **긍정적**: 리스크 회피, 빠른 기능 제공, 코드 격리
- **부정적**: 2단계 배포, Phase 1 UX 제약

---

### ADR-009: Settings Page 아키텍처 설계
**상태**: Accepted | **작성일**: 2026-01-21

Portal Shell에 사용자 설정 페이지 추가를 위한 아키텍처 설계를 수립합니다.

**결정 요약**:
- Local-First with Optional Sync 방식 채택
- localStorage를 primary storage로 사용
- 백엔드는 선택적 동기화 레이어 (Best Effort)
- 기존 `theme.ts` store 확장 + 새로운 `settings.ts` store 추가

**파일**: [ADR-009-settings-page-architecture.md](./ADR-009-settings-page-architecture.md)

**영향 범위**:
- `frontend/portal-shell/src/store/settings.ts` (신규)
- `frontend/portal-shell/src/pages/SettingsPage.vue` (신규)
- `services/auth-service/.../entity/UserSettings.java` (선택적)
- Auth Service API: `GET/PUT /api/auth/users/me/settings` (선택적)

**대안 검토**:
- ❌ Backend-First: 네트워크 지연, 비회원 사용 불가
- 🟡 localStorage-Only: 다중 디바이스 동기화 불가
- ✅ Local-First + Optional Sync: 빠른 응답 + 동기화 (채택)

---

### ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템
**상태**: Proposed | **작성일**: 2026-01-28

계층적 RBAC + Permission + Membership 통합 모델로 인증/인가 시스템을 전환합니다.

**결정 요약**:
- Role 계층: SUPER_ADMIN, BLOG_ADMIN, SHOPPING_ADMIN, SELLER, USER
- Permission 모델: `{service}:{resource}:{action}` 형식
- Membership 티어: FREE, BASIC, PREMIUM, VIP (서비스별)
- JWT v2: roles 배열 + memberships 필드
- Lean Token, Rich Resolution: JWT 작게, Redis 캐시 기반 해석

**파일**: [ADR-011-hierarchical-rbac-membership-system.md](./ADR-011-hierarchical-rbac-membership-system.md)

**영향 범위**:
- `services/auth-service/` - Role/Permission/Membership 엔티티, API
- `services/api-gateway/` - JWT v1/v2 dual format 지원
- `services/common-library/.../security/` - PermissionResolver, 필터
- `services/shopping-service/` - Seller 역할, 권한 체크
- `services/blog-service/` - BLOG_ADMIN 역할
- `frontend/` - authStore, usePermission, PermissionGuard

**대안 검토**:
- ❌ 기존 시스템 유지: 확장성 없음
- ❌ ABAC만: 복잡도 과도함
- 🟡 RBAC만: Permission 없어 세밀한 제어 불가
- ✅ RBAC + Membership 하이브리드: 확장성 + 유연성 (채택)

---

## ADR 관리 규칙

### 상태 정의

| 상태 | 설명 | 변경 가능 |
|------|------|----------|
| **Proposed** | 제안됨, 검토 중 | ✅ 다른 상태로 변경 가능 |
| **Accepted** | 승인됨, 현재 적용 중 | ✅ Deprecated로 변경 가능 |
| **Deprecated** | 폐기됨, 더 이상 적용 안 함 | ❌ 변경 불가 |
| **Superseded** | 대체됨, 새로운 ADR이 이를 대체 | ❌ 변경 불가 |

### 작성 과정

1. **Proposed**: 새로운 아키텍처 결정이 필요할 때 작성
2. **Accepted**: 팀 논의 후 합의하면 승인
3. **Deprecated**: 더 이상 적용되지 않으면 폐기 표시

### 파일 명명 규칙

```
ADR-[번호]-[짧은-제목].md
예) ADR-001-admin-component-structure.md
```

---

## ADR 템플릿

새로운 ADR을 작성할 때 다음 템플릿을 사용하세요:

```markdown
# ADR-XXX: [제목]

## 상태
Proposed | Accepted | Deprecated

## 날짜
YYYY-MM-DD

---

## 컨텍스트
배경 설명 및 문제 상황

## 결정
선택한 결정사항

## 대안 검토
| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| ...  | ...  | ...  | ...  |

## 결과
이 결정의 영향

## 다음 단계
구현 계획 및 관련 작업
```

---

## 프로젝트 컨텍스트

**Repository**: Portal Universe
**관련 디렉토리**:
- `/docs/architecture/` - 아키텍처 설계 문서
- `/docs/api/` - API 명세서
- `/frontend/shopping-frontend/` - Admin UI 구현
- `/services/shopping-service/` - Backend API 구현

**관련 CLAUDE.md 섹션**:
- 프로젝트 구조
- 서비스 간 통신 패턴
- API 라우팅

---

## 관련 문서

- [Admin 상품 관리 시스템 아키텍처 설계](../architecture/admin-product-management.md)
- [Admin 상품 관리 API 명세서](../api/admin-products-api.md)
- [Admin 권한 검증 전략](../architecture/admin-authorization-strategy.md)
- [CLAUDE.md - 프로젝트 가이드](../../CLAUDE.md)

---

## 변경 이력

| ADR | 버전 | 날짜 | 변경 사항 |
|-----|------|------|----------|
| ADR-001 | 1.0 | 2026-01-17 | 초기 작성 |
| ADR-002 | 1.0 | 2026-01-17 | 초기 작성 |
| ADR-003 | 1.0 | 2026-01-17 | 초기 작성 |
| ADR-004 | 1.0 | 2026-01-19 | 초기 작성 |
| ADR-005 | 1.0 | 2026-01-19 | 초기 작성 |
| ADR-006 | 1.0 | 2026-01-20 | 초기 작성 |
| ADR-007 | 1.0 | 2026-01-21 | 초기 작성 |
| ADR-008 | 1.0 | 2026-01-21 | 초기 작성 |
| ADR-009 | 1.0 | 2026-01-21 | 초기 작성 |
| ADR-011 | 1.0 | 2026-01-28 | 초기 작성 |

---

**최종 업데이트**: 2026-01-21
**관리자**: Documenter Agent
