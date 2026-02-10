---
id: guide-git-convention
title: Git Convention 가이드
type: guide
status: current
created: 2026-02-10
updated: 2026-02-10
author: Laze
tags: [git, convention, commit, branch, workflow]
related:
  - guide-contributing
  - onboarding-path
---

# Git Convention 가이드

**난이도**: ⭐⭐ | **예상 시간**: 15분 | **카테고리**: Development

> 프로젝트의 Git 커밋 메시지, 브랜치 네이밍, 워크플로우, Pre-commit Hook 규칙을 정의합니다.

---

## 1. Commit Message Convention

### 형식

```
<type>(<scope>): <subject>

[body]

[footer]
```

### Type

| Type | 용도 | 예시 |
|------|------|------|
| `feat` | 새 기능 | `feat(shopping): add coupon redemption API` |
| `fix` | 버그 수정 | `fix(auth): resolve token refresh race condition` |
| `docs` | 문서 변경 | `docs(api): update shopping API reference` |
| `style` | 코드 스타일 (동작 변경 없음) | `style(gateway): apply checkstyle rules` |
| `refactor` | 리팩토링 (동작 변경 없음) | `refactor(blog): extract post validation logic` |
| `test` | 테스트 추가/수정 | `test(shopping): add order saga unit tests` |
| `chore` | 설정/빌드 변경 | `chore(gradle): upgrade Spring Boot to 3.5.5` |
| `perf` | 성능 개선 | `perf(shopping): add Redis cache to product query` |
| `ci` | CI/CD 변경 | `ci(github): add shopping-service build workflow` |

### Scope

서비스명 또는 영역을 사용합니다.

| Scope | 대상 |
|-------|------|
| `auth` | auth-service |
| `gateway` | api-gateway |
| `shopping` | shopping-service, shopping-frontend |
| `blog` | blog-service, blog-frontend |
| `notification` | notification-service |
| `prism` | prism-service, prism-frontend |
| `portal-shell` | portal-shell |
| `admin` | admin-frontend |
| `common` | common-library, 공통 설정 |
| `design-system` | design-system-vue, design-system-react |
| `infra` | Docker, K8s, CI/CD |

### 규칙

- **subject**: 50자 이내, 명령형(`add`, `fix`, `update`), 소문자 시작, 마침표 없음
- **body**: What & Why 중심, 72자 줄바꿈
- **footer**: `BREAKING CHANGE:`, `Closes #123`, `Refs #456`

### 예시

```
feat(shopping): add time-deal purchase with Redis Lua script

Redis Lua script를 사용하여 재고 차감과 구매 검증을 원자적으로 처리.
기존 @Transactional 방식은 동시 요청 시 overselling 발생.

Refs #87
```

```
fix(portal-shell): resolve dark mode flicker on initial load

theme 상태를 localStorage에서 동기적으로 읽어 FOUC 방지.
기존 useEffect 비동기 로딩이 흰색 flash를 유발했음.

Closes #102
```

---

## 2. Branch Naming Convention

### 형식

```
<type>/<short-description>          # issue 없는 경우
<type>/<issue-number>-<description> # issue 있는 경우
```

### Type

| Type | 용도 | Base Branch | 예시 |
|------|------|-------------|------|
| `feature` | 새 기능 | `dev` | `feature/admin-rbac-membership` |
| `fix` | 버그 수정 | `dev` | `fix/token-refresh-loop` |
| `hotfix` | 긴급 수정 | `main` | `hotfix/auth-session-crash` |
| `refactor` | 리팩토링 | `dev` | `refactor/bootstrap-cleanup` |
| `docs` | 문서 | `dev` | `docs/api-spec-update` |
| `chore` | 설정/빌드 | `dev` | `chore/gradle-upgrade` |
| `test` | 테스트 | `dev` | `test/e2e-prism` |

### 규칙

- 소문자, 하이픈(`-`) 구분, 영어만
- 설명 2~4단어
- `main`/`dev` 직접 push 금지

---

## 3. Branch Workflow

```
feature/xxx ──PR──> dev ──PR──> main
hotfix/xxx  ──PR──> main (+ cherry-pick to dev)
```

### 일반 작업 흐름

```bash
# 1. dev에서 feature 브랜치 생성
git checkout dev
git pull origin dev
git checkout -b feature/coupon-system

# 2. 작업 & 커밋
git add services/shopping-service/src/main/java/.../CouponController.java
git add docs/api/shopping-service/coupon-api.md
git commit -m "feat(shopping): add coupon issuance API"

# 3. PR 생성
git push -u origin feature/coupon-system
gh pr create --base dev --title "feat(shopping): add coupon issuance API"
```

### Hotfix 흐름

```bash
# 1. main에서 hotfix 브랜치 생성
git checkout main
git pull origin main
git checkout -b hotfix/auth-session-crash

# 2. 수정 & 커밋
git commit -m "fix(auth): resolve session crash on concurrent refresh"

# 3. main으로 PR → merge 후 dev에 cherry-pick
git push -u origin hotfix/auth-session-crash
gh pr create --base main --title "hotfix(auth): resolve session crash"
```

---

## 4. 커밋 분할 기준

하나의 커밋이 다루는 범위를 최소화합니다.

| 패턴 | 예시 |
|------|------|
| 서비스별 분리 | auth-service 변경과 shopping-service 변경은 별도 커밋 |
| 기능별 분리 | RBAC 재구조화 커밋 / Admin Frontend 커밋 분리 |
| docs + code 동봉 | 코드와 해당 문서를 **같은 커밋**에 포함 (doc-check 통과) |

---

## 5. Pre-commit Hooks

프로젝트에 2개의 pre-commit hook이 자동 실행됩니다.

### 5-1. Symlink Guard

민감 파일이 symlink로 커밋되는 것을 차단합니다.

**보호 대상**:
- `.claude/`, `certs/`
- `.env`, `.env.local`, `.env.dev`, `.env.docker`, `.env.k8s`
- `.mcp.json`
- `k8s/base/secret.yaml`, `k8s/base/jwt-secrets.yaml`

**차단 시 해결**:
```bash
git reset HEAD <file>  # staging에서 제거
```

### 5-2. Doc Check

서비스 로직 변경 시 `docs/` 변경 동반을 강제합니다.

**감지 대상**:
- `services/*/src/main/**` (설정, 테스트, 빌드 파일 제외)
- `frontend/*/src/**` (CSS, 이미지, 테스트 파일 제외)

**문서 불필요 (자동 통과)**:
- 설정 파일 (`.yml`, `.properties`, `.json`, `.gradle`)
- 테스트 코드 (`*Test.java`, `*.spec.*`, `*.test.*`)
- 빌드/인프라 (`Dockerfile`, `docker-compose`)
- 정적 리소스 (이미지, 폰트, CSS)
- `docs/` 자체, `scripts/`, `.claude/`

**변경 유형별 문서 위치**:

| 변경 유형 | 문서 위치 |
|----------|----------|
| Controller/API | `docs/api/{service}/` |
| 서비스 로직 | `docs/architecture/{service}/` |
| 이벤트 스키마 | `docs/architecture/{service}/` |
| DB 스키마 | `docs/architecture/database/` |
| 새 서비스 | `docs/api/` + `docs/architecture/` + `docs/adr/` |

**긴급 우회**:
```bash
git commit --no-verify -m "[skip-docs] fix(auth): hotfix for production crash"
```

> `[skip-docs]`를 사용한 경우 다음 커밋에서 문서 보완 필수

---

## 6. 금지 사항

- `main`/`dev` 직접 push (feature branch -> PR -> merge)
- `--force-push` to `main`/`dev`
- `.env`, `credentials`, 비밀키 파일 커밋
---

## 다음 단계

- [문서 읽기 순서 가이드](onboarding-path.md) - 프로젝트 온보딩
- [Contributing Guide](contributing.md) - Design System 기여 방법
- [문서화 규칙](../../../.claude/rules/documentation.md) - 문서 작성 상세 규칙

---

### 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-02-10 | 최초 작성 - 커밋/브랜치/워크플로우/Pre-commit Hook |
