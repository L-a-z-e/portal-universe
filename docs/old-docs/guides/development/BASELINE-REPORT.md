# 테스트 커버리지 기준선 리포트

> **측정일**: 2026-02-04
> **브랜치**: refactor/phase0-setup
> **관련 플랜**: `.claude/plans/enumerated-purring-volcano.md`

## 1. Java Backend 테스트 현황

### 1.1 테스트 실행 결과

```
총 72개 테스트 실행, 11개 실패
```

| 서비스 | 테스트 결과 | 비고 |
|--------|------------|------|
| common-library | ✅ 성공 | JaCoCo 리포트 생성됨 |
| notification-service | ✅ 성공 | JaCoCo 리포트 생성됨 |
| auth-service | ❌ 실패 | 일부 테스트 실패 |
| blog-service | ❌ 실패 | 일부 테스트 실패 |
| shopping-service | ❌ 실패 | InventoryServiceIntegrationTest 실패 |
| api-gateway | ❌ 실패 | 일부 테스트 실패 |
| integration-tests | ❌ 실패 | NoClassDefFoundError |

### 1.2 JaCoCo 커버리지 (측정 가능한 서비스)

| 서비스 | Instruction | Branch | 비고 |
|--------|-------------|--------|------|
| **common-library** | 34% | 34% | 보안 모듈(XSS, SQL) 95%+ |
| **notification-service** | 32% | 25% | Service 레이어 57% |

### 1.3 상세 분석: common-library

| 패키지 | 커버리지 | 상태 |
|--------|----------|------|
| security.xss | 95% | ✅ 양호 |
| security.sql | 96% | ✅ 양호 |
| security.audit | 0% | ⚠️ 테스트 필요 |
| security.context | 0% | ⚠️ 테스트 필요 |
| security.filter | 0% | ⚠️ 테스트 필요 |
| security.converter | 0% | ⚠️ 테스트 필요 |
| util | 0% | ⚠️ 테스트 필요 |

### 1.4 상세 분석: notification-service

| 패키지 | 커버리지 | 상태 |
|--------|----------|------|
| domain | 87% | ✅ 양호 |
| controller | 67% | ⚠️ 개선 여지 |
| service | 57% | ⚠️ 개선 여지 |
| consumer | 20% | ❌ 낮음 |
| converter | 0% | ❌ 테스트 필요 |

## 2. 테스트 실패 원인 분석

### 2.1 shopping-service 실패

```
InventoryServiceIntegrationTest: 9개 테스트 모두 실패
원인: java.lang.NoClassDefFoundError at Constructor.java:500
      Caused by: ExceptionInInitializerError
```

**추정 원인**: Testcontainers 또는 Redis 관련 초기화 문제

### 2.2 integration-tests 실패

```
원인: NoClassDefFoundError
```

**추정 원인**: 의존성 또는 클래스 로딩 문제

## 3. 기준선 목표

### 3.1 리팩토링 전 기준선 (현재)

| 지표 | 현재 값 |
|------|--------|
| 테스트 성공률 | 85% (61/72) |
| common-library 커버리지 | 34% |
| notification-service 커버리지 | 32% |
| 기타 서비스 | 측정 불가 (테스트 실패) |

### 3.2 리팩토링 후 목표

| 지표 | 목표 값 | 비고 |
|------|--------|------|
| 테스트 성공률 | 100% | 모든 테스트 통과 |
| common-library 커버리지 | 60%+ | 보안 모듈 유지 |
| 각 서비스 커버리지 | 40%+ | 최소 기준 |

## 4. 다음 단계

### 즉시 필요한 작업
1. [ ] shopping-service 테스트 실패 원인 조사
2. [ ] integration-tests 의존성 문제 해결

### 리팩토링 중 고려사항
- 테스트 성공률 하락 시 즉시 롤백
- 새 코드 작성 시 테스트 함께 추가
- CI/CD에 커버리지 게이트 추가 검토

## 5. 참고 명령어

```bash
# 전체 테스트 실행
./gradlew clean test --continue

# JaCoCo 리포트 생성
./gradlew jacocoTestReport

# 특정 서비스만 테스트
./gradlew :services:common-library:test

# 리포트 위치
services/{service}/build/reports/jacoco/test/html/index.html
services/{service}/build/reports/tests/test/index.html
```

## 6. E2E 테스트 현황

### 6.1 테스트 파일 현황

**총 59개 E2E 테스트 파일** (Playwright)

| 영역 | 파일 수 | 테스트 범위 |
|------|--------|------------|
| portal-shell | 10개 | auth, navigation, profile, theme 등 |
| blog | 19개 | post, comment, search, series 등 |
| shopping | 12개 | cart, checkout, product, order 등 |
| admin | 7개 | product, order, coupon, inventory 등 |
| prism | 10개 | task, board, agent, chat 등 |

### 6.2 E2E 테스트 명령어

```bash
# 전체 테스트
npm test

# 서비스별 테스트
npm run test:portal
npm run test:blog
npm run test:shopping
npm run test:prism

# 디버그 모드
npm run test:debug
```

### 6.3 E2E 테스트 실행 조건

- 서비스 실행 필요: `docker-compose up -d` 또는 개별 서비스 실행
- 기본 URL: `http://localhost:30000`

---

## 7. Frontend 단위 테스트 현황

### 6.1 단위 테스트

| 앱 | 테스트 스크립트 | 커버리지 |
|----|----------------|----------|
| portal-shell | ❌ 없음 | N/A |
| blog-frontend | ❌ 없음 | N/A |
| shopping-frontend | ❌ 없음 | N/A |
| prism-frontend | ❌ 없음 | N/A |

**현황**: Frontend 앱에 단위 테스트가 설정되어 있지 않음

### 6.2 Frontend 테스트 기준선 목표

| 지표 | 현재 | 목표 | 비고 |
|------|------|------|------|
| 단위 테스트 | 0% | 30%+ | 공통 패키지 우선 |
| 컴포넌트 테스트 | 0% | 주요 컴포넌트 | Design System |

### 6.3 향후 계획

1. **Phase 2**: 신규 공통 패키지 (api-client, react-bootstrap) 테스트 작성
2. **Phase 4**: Design System 컴포넌트 테스트 추가 검토

---

**작성자**: Laze
**다음 업데이트**: Phase 1 완료 후
