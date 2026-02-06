# browser-test Plan

> **Feature**: browser-test
> **Date**: 2026-02-02
> **Author**: AI + Laze
> **Status**: Draft

---

## 1. Overview

### 1.1 목적

실제 Playwright 브라우저를 통해 Portal Universe의 **3개 프로필 환경**(local, docker, kubernetes)을 순차적으로 검증한다.
각 환경에서 서비스를 직접 실행한 상태로 브라우저 접속, 로그인, 주요 기능 동작을 점검하며,
실패 시 오류를 파악하고 수정 → 재시작 → 재검증을 통과할 때까지 반복한 후 다음 단계로 진행한다.

### 1.2 범위

| 환경 | 실행 방식 | Base URL | 검증 순서 |
|------|----------|----------|----------|
| **local** | `docker-compose-local.yml` (인프라) + `gradlew bootRun` (백엔드) + `npm run dev` (프론트) | `https://localhost:30000` | 1st |
| **docker** | `docker compose up -d` (전체) | `https://localhost:30000` | 2nd |
| **kubernetes** | Kind 클러스터 + k8s manifests | `https://localhost` (Ingress) | 3rd |

### 1.3 검증 항목 (환경별 공통)

| # | 카테고리 | 검증 항목 | 성공 기준 |
|:-:|---------|----------|----------|
| 1 | 접속 | Portal Shell 메인 페이지 로드 | `<body>` visible, 5초 이내 |
| 2 | 인증 | Login 버튼 → 모달 → 로그인 | `test@example.com` / `password123`로 로그인, Logout 버튼 표시 |
| 3 | 인증 | Admin 로그인 | `admin@example.com` / `admin123`로 로그인 |
| 4 | Blog | 블로그 페이지 접속 | `/blog` 경로, Module Federation 리모트 로딩 |
| 5 | Blog | 게시글 목록 표시 | 게시글 리스트 또는 empty state 표시 |
| 6 | Shopping | 쇼핑 페이지 접속 | `/shopping` 경로, React 리모트 로딩 |
| 7 | Shopping | 상품 목록 표시 | 상품 카드 또는 empty state 표시 |
| 8 | Prism | Prism 페이지 접속 | `/prism` 경로, React 리모트 로딩 |
| 9 | Prism | 보드 또는 에이전트 목록 | Prism 콘텐츠 로딩 |
| 10 | 인증 | 로그아웃 | Logout 클릭 → Login 버튼 복원 |
| 11 | Theme | 다크모드 토글 | 테마 전환 동작 |

---

## 2. AS-IS 분석

### 2.1 기존 E2E 테스트 현황

```
e2e-tests/                     # 54개 테스트 파일
├── playwright.config.ts       # baseURL: https://localhost:30000
├── tests/
│   ├── auth.setup.ts          # 일반 사용자 인증
│   ├── auth-admin.setup.ts    # 관리자 인증
│   ├── data-seed.setup.ts     # 테스트 데이터 시딩
│   ├── helpers/
│   │   ├── auth.ts            # gotoServicePage(), waitForAuthReady()
│   │   ├── test-fixtures.ts
│   │   └── test-fixtures-admin.ts
│   ├── shopping/ (10 files)
│   ├── blog/ (11 files)
│   ├── prism/ (6 files)
│   └── admin/ (6 files)
```

- 기존 테스트는 **하나의 환경**(주로 docker)을 전제로 작성
- `BASE_URL` 환경변수로 URL 변경 가능하나, 환경별 순차 검증 프로세스 없음
- 실패 시 자동 수정 → 재검증 워크플로우 없음

### 2.2 문제점

| 문제 | 설명 |
|------|------|
| 환경 전환 수동 | local → docker → k8s 전환이 수동이며 검증 기준 없음 |
| 환경 기동 검증 없음 | 서비스 health check 없이 바로 테스트 실행 |
| K8s 테스트 미경험 | Kind 클러스터 기반 E2E 테스트 프로세스 미정립 |
| 실패 복구 전략 없음 | 테스트 실패 시 원인 분석 → 수정 → 재시도 자동화 없음 |

---

## 3. TO-BE 전략

### 3.1 핵심 원칙

1. **순차 검증**: local 통과 → docker 통과 → kubernetes 통과
2. **Gate 방식**: 이전 환경 통과 없이 다음 환경 진행 불가
3. **실패 시 Fix-Restart-Retest 루프**: 오류 파악 → 코드/설정 수정 → 환경 재시작 → 재검증
4. **기존 테스트 활용**: 새 테스트 작성 최소화, 기존 54개 테스트 재사용
5. **MCP Playwright 활용**: Claude Code의 Playwright MCP 서버로 실시간 브라우저 조작

### 3.2 환경별 실행 계획

#### Phase 1: Local 환경 검증

```bash
# 1. 인프라 기동
docker compose -f docker-compose-local.yml up -d

# 2. 백엔드 서비스 기동 (병렬)
./gradlew :services:auth-service:bootRun &
./gradlew :services:api-gateway:bootRun &
./gradlew :services:blog-service:bootRun &
./gradlew :services:shopping-service:bootRun &
./gradlew :services:notification-service:bootRun &
./gradlew :services:prism-service:bootRun &

# 3. 프론트엔드 기동
cd frontend && npm run dev

# 4. Health check (모든 서비스 UP 확인)
# 5. Playwright 브라우저 테스트 실행
```

**검증 방법**: MCP Playwright로 직접 브라우저 조작 + 기존 E2E 스크립트 선별 실행

#### Phase 2: Docker 환경 검증

```bash
# 1. Local 환경 정리
# 2. Docker 전체 기동
docker compose up -d --build

# 3. Health check (모든 컨테이너 healthy)
# 4. Playwright 브라우저 테스트 실행
```

**차이점**: HTTPS (자체 서명 인증서), 컨테이너 간 네트워크

#### Phase 3: Kubernetes 환경 검증

```bash
# 1. Docker 환경 정리
# 2. Kind 클러스터 생성
kind create cluster --config k8s/base/kind-config.yaml

# 3. 인프라 배포
kubectl apply -f k8s/base/ -f k8s/infrastructure/

# 4. 서비스 빌드 + 이미지 로드 + 배포
# 5. Ingress 설정 완료 대기
# 6. Playwright 브라우저 테스트 실행
```

**차이점**: Ingress 라우팅 (`https://localhost`), Kind 클러스터 환경

### 3.3 검증 프로세스 (각 Phase 공통)

```
환경 기동
  ↓
Health Check (서비스 응답 확인)
  ↓
MCP Playwright 브라우저 접속
  ↓
검증 항목 순차 실행 (11개)
  ↓
├── 전체 통과 → 다음 Phase로
└── 실패 발생
     ↓
    오류 분석 (로그, 스크린샷, 네트워크)
     ↓
    코드/설정 수정
     ↓
    환경 재시작
     ↓
    재검증 (실패 항목부터)
     ↓
    통과 → 다음 Phase로
```

---

## 4. 기술 결정

### 4.1 테스트 실행 방식

| 선택지 | 장점 | 단점 | **결정** |
|--------|------|------|---------|
| MCP Playwright 직접 조작 | 실시간 디버깅, 즉시 수정 가능 | 반복 어려움 | **Primary** |
| `npx playwright test` 스크립트 | 자동화, 재현성 | 실패 시 컨텍스트 부족 | Secondary |
| 혼합 (MCP + Script) | 유연성 + 자동화 | 복잡도 | **채택** |

**결정**: MCP Playwright로 수동 검증 먼저 수행 → 통과 후 기존 E2E 스크립트로 회귀 테스트

### 4.2 Health Check 방식

```bash
# 백엔드 서비스 health check
curl -f http://localhost:8080/actuator/health  # API Gateway
curl -f http://localhost:8081/actuator/health  # Auth Service
curl -f http://localhost:8082/actuator/health  # Blog Service
curl -f http://localhost:8083/actuator/health  # Shopping Service
curl -f http://localhost:8084/actuator/health  # Notification Service

# 프론트엔드 접속 확인
curl -fk https://localhost:30000              # Portal Shell
```

### 4.3 실패 복구 전략

| 실패 유형 | 분석 방법 | 수정 대상 |
|----------|----------|----------|
| 서비스 미기동 | `docker compose logs`, `gradlew` 콘솔 | 설정 파일, 의존성 |
| DB 연결 실패 | 서비스 로그, MySQL/Mongo/PostgreSQL 상태 | application-*.yml |
| 프론트엔드 로드 실패 | 브라우저 콘솔, 네트워크 탭 | vite.config.ts, env |
| Module Federation 실패 | remoteEntry.js 404/500 | shared 설정, CORS |
| 로그인 실패 | Auth Service 로그, API 응답 | JWT 설정, CORS |
| K8s Pod CrashLoop | `kubectl logs`, `kubectl describe` | manifest, resources |

---

## 5. 작업 계획

### Phase 1: Local 환경 (예상 작업량: 중)

| # | 작업 | 설명 |
|:-:|------|------|
| 1.1 | 인프라 기동 | `docker-compose-local.yml` |
| 1.2 | 백엔드 서비스 기동 | 6개 서비스 `bootRun` |
| 1.3 | 프론트엔드 기동 | `npm run dev` (4개 앱) |
| 1.4 | Health check | 모든 서비스 UP 확인 |
| 1.5 | 브라우저 검증 | 11개 항목 MCP Playwright |
| 1.6 | 실패 수정 | 발견된 문제 수정 + 재검증 |
| 1.7 | E2E 스크립트 | 기존 테스트 선별 실행 |

### Phase 2: Docker 환경 (예상 작업량: 중)

| # | 작업 | 설명 |
|:-:|------|------|
| 2.1 | Local 정리 | 프로세스 종료, 포트 해제 |
| 2.2 | Docker 기동 | `docker compose up -d` |
| 2.3 | Health check | 컨테이너 상태 + 서비스 응답 |
| 2.4 | 브라우저 검증 | 11개 항목 |
| 2.5 | 실패 수정 | Docker 특이 문제 수정 |
| 2.6 | E2E 스크립트 | 전체 테스트 실행 |

### Phase 3: Kubernetes 환경 (예상 작업량: 대)

| # | 작업 | 설명 |
|:-:|------|------|
| 3.1 | Docker 정리 | `docker compose down` |
| 3.2 | Kind 클러스터 생성 | `kind create cluster` |
| 3.3 | 인프라 배포 | DB, Kafka, Redis, Elasticsearch |
| 3.4 | 이미지 빌드 + 로드 | 서비스 Docker 이미지 → Kind 로드 |
| 3.5 | 서비스 배포 | K8s manifests apply |
| 3.6 | Ingress 설정 | NGINX Ingress Controller |
| 3.7 | Health check | Pod Ready + 서비스 응답 |
| 3.8 | 브라우저 검증 | 11개 항목 |
| 3.9 | 실패 수정 | K8s 특이 문제 수정 |
| 3.10 | E2E 스크립트 | 전체 테스트 실행 (BASE_URL 변경) |

---

## 6. 성공 기준

| 기준 | 목표 |
|------|------|
| Local 검증 | 11개 항목 전체 통과 |
| Docker 검증 | 11개 항목 전체 통과 |
| Kubernetes 검증 | 11개 항목 전체 통과 |
| E2E 스크립트 | 주요 smoke 테스트 통과 (shopping, blog noauth) |
| 발견 이슈 | 모두 수정 완료, 코드 커밋 |

---

## 7. 리스크

| 리스크 | 영향 | 대응 |
|--------|------|------|
| Local에서 6개 백엔드 동시 실행 메모리 부족 | 서비스 기동 실패 | JVM 메모리 제한 (`-Xmx256m`), 필수 서비스만 우선 기동 |
| Docker 빌드 시간 | 지연 | 캐시 활용, 변경 서비스만 재빌드 |
| Kind 클러스터 리소스 부족 | Pod CrashLoopBackOff | 리소스 요청/제한 조정, 불필요 서비스 제외 |
| 자체 서명 인증서 | 브라우저 차단 | `ignoreHTTPSErrors: true` |
| Kafka/Elasticsearch 기동 지연 | 서비스 연결 실패 | 충분한 대기 시간, retry 설정 |

---

## 8. 스코프 제외

- 성능 테스트 (부하, 응답 시간 측정)
- 크로스 브라우저 테스트 (Chromium만 사용)
- 모바일 반응형 테스트
- CI/CD 파이프라인 통합
- 새로운 E2E 테스트 파일 작성 (기존 활용)
