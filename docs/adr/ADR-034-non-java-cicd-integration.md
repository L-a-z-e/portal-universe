# ADR-034: 비Java 서비스 CI/CD 파이프라인 통합

**Status**: Accepted
**Date**: 2026-02-13
**Author**: Laze
**Supersedes**: -

## Context

Portal Universe는 Polyglot 아키텍처(Java/Spring, NestJS, Python)를 채택했지만, 현재 CI/CD 파이프라인은 Java 중심으로 구성되어 prism-service(NestJS), chatbot-service(Python)가 빌드/테스트/Docker 이미지 생성 파이프라인에서 완전히 누락되었다. 이로 인해:

- prism-service, chatbot-service의 코드 품질 회귀를 사전 감지할 수 없음
- 수동 빌드/배포에 의존하여 운영 리스크 증가
- Java 서비스와의 일관성 없는 품질 보증 프로세스

현재 `.github/workflows/ci.yml`은 backend job(Java 5개), frontend job(Vue/React 5개), e2e job, integration-tests job으로 구성되어 있으며, `.github/workflows/docker.yml`의 matrix는 Java 5개 서비스와 portal-shell/blog-frontend만 포함한다.

## Decision

**서비스별 독립 CI 워크플로우 추가**: `prism-ci.yml`, `chatbot-ci.yml`을 신규 생성하고, `docker.yml`의 build matrix에 두 서비스를 추가하여 언어별 독립 파이프라인으로 통합한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| **① 서비스별 독립 워크플로우 (채택)** | path filter로 불필요한 실행 방지, 언어별 환경 독립 관리, 실패 격리 | 워크플로우 파일 수 증가 (2개 추가) |
| ② 기존 ci.yml에 job 추가 | 단일 파일 관리, 전체 CI 상태 한눈에 파악 | 워크플로우 파일 비대화(300줄 → 400줄 예상), 모든 push에 전체 실행 |
| ③ Matrix 전략 확장 (언어별 matrix) | DRY 원칙 준수, 단일 job으로 통합 | 언어별 빌드 환경이 완전히 다름(JDK/Node/Python), 조건부 step 복잡도 증가 |

## Rationale

- prism-service는 Node 22, NestJS CLI, Jest가 필요하며, chatbot-service는 Python 3.12, pip, pytest, ruff가 필요하므로 Java 중심 backend job에 통합하기 어렵다
- path filter(`services/prism-service/**`, `services/chatbot-service/**`)를 사용하면 해당 서비스 변경 시에만 실행되어 CI 시간 낭비를 방지한다
- prism-service, chatbot-service의 Dockerfile이 이미 존재하므로 docker.yml matrix 추가만으로 이미지 자동 배포가 가능하다
- 서비스별 독립 워크플로우는 실패 격리가 가능하여, prism-service 테스트 실패가 Java 서비스 배포를 차단하지 않는다
- 향후 Go, Rust 등 추가 언어 서비스가 생길 경우 동일 패턴으로 확장 가능하다

## Trade-offs

✅ **장점**:
- prism-service, chatbot-service의 코드 품질 회귀를 PR 단계에서 사전 차단
- 언어별 독립적인 빌드 환경 관리로 의존성 충돌 방지
- path filter로 불필요한 CI 실행 최소화 (타 서비스 변경 시 실행 안 함)
- Docker 이미지 자동 빌드/배포로 수동 작업 제거

⚠️ **단점 및 완화**:
- 워크플로우 파일 수 증가 (3개 → 5개) → (완화: 파일명 규칙 `{service}-ci.yml`로 통일하여 관리 부담 최소화)
- prism-service는 PostgreSQL 컨테이너 필요, chatbot-service는 외부 API 의존성 있어 통합 테스트 구성 복잡 → (완화: 1단계는 unit test + lint + build만 포함, E2E는 기존 e2e-tests에 위임)
- NestJS와 Python의 커버리지 리포트 형식이 Java(JaCoCo)와 다름 → (완화: codecov는 lcov.info, coverage.xml 모두 지원하므로 추가 설정 불필요)

## Implementation

### 1. prism-ci.yml (신규)

```yaml
name: Prism Service CI

on:
  pull_request:
    branches: [main, dev]
    paths:
      - 'services/prism-service/**'
      - '.github/workflows/prism-ci.yml'
  push:
    branches: [main, dev]
    paths:
      - 'services/prism-service/**'
  workflow_dispatch:

env:
  NODE_VERSION: '22'

jobs:
  prism-service:
    name: Prism Service Build & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: services/prism-service

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_USER: prism
          POSTGRES_PASSWORD: password
          POSTGRES_DB: prism_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd="pg_isready -U prism"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: services/prism-service/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Run tests
        run: npm run test:cov
        env:
          DATABASE_URL: postgresql://prism:password@localhost:5432/prism_test
          REDIS_HOST: localhost
          REDIS_PORT: 6379

      - name: Build
        run: npm run build

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: services/prism-service/coverage/lcov.info
          flags: prism-service
          fail_ci_if_error: false
```

### 2. chatbot-ci.yml (신규)

```yaml
name: Chatbot Service CI

on:
  pull_request:
    branches: [main, dev]
    paths:
      - 'services/chatbot-service/**'
      - '.github/workflows/chatbot-ci.yml'
  push:
    branches: [main, dev]
    paths:
      - 'services/chatbot-service/**'
  workflow_dispatch:

env:
  PYTHON_VERSION: '3.12'

jobs:
  chatbot-service:
    name: Chatbot Service Build & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: services/chatbot-service

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}
          cache: 'pip'

      - name: Install dependencies
        run: |
          python -m pip install --upgrade pip
          pip install -r requirements.txt
          pip install pytest pytest-asyncio ruff

      - name: Lint (ruff)
        run: ruff check .

      - name: Run tests
        run: pytest --cov=app --cov-report=xml --cov-report=term

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: services/chatbot-service/coverage.xml
          flags: chatbot-service
          fail_ci_if_error: false
```

### 3. docker.yml 수정

```yaml
# build-backend job의 matrix.service에 추가:
strategy:
  fail-fast: false
  matrix:
    service:
      - api-gateway
      - auth-service
      - blog-service
      - shopping-service
      - notification-service
      - prism-service    # ← 추가
      - chatbot-service  # ← 추가
```

각 서비스의 Dockerfile은 다음 경로에 이미 존재:
- `services/prism-service/Dockerfile`
- `services/chatbot-service/Dockerfile`

### 4. 영향받는 파일

- `.github/workflows/prism-ci.yml` (신규)
- `.github/workflows/chatbot-ci.yml` (신규)
- `.github/workflows/docker.yml` (matrix 수정)

### 5. 검증 방법

PR 생성 후 다음을 확인:
- prism-service 코드 변경 → prism-ci.yml만 실행, ci.yml은 skip
- chatbot-service 코드 변경 → chatbot-ci.yml만 실행
- Java 서비스 변경 → ci.yml만 실행, prism/chatbot은 skip
- main branch merge 후 docker.yml 실행 → 7개 서비스 모두 이미지 생성

## References

- [Execution Guide](../../.claude/rules/execution.md) - prism-service, chatbot-service 실행 명령어
- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md) - Polyglot 환경에서의 표준화 선례
- [GitHub Actions: Path Filters](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#onpushpull_requestpaths) - path 기반 트리거 공식 문서

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
| 2026-02-13 | 구현 완료: prism-ci.yml, chatbot-ci.yml, contract-check.yml 생성, docker.yml 확장. Status → Accepted | Laze |
