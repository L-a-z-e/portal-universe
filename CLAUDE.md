# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Portal Universe는 Spring Boot 3.5.5 백엔드, Vue 3/React 마이크로 프론트엔드(Module Federation), Kubernetes 인프라로 구축된 폴리글랏 마이크로서비스 플랫폼입니다. MSA(Microservices Architecture) + MFA(Micro Frontend Architecture) 패턴을 시연합니다.

## 프로젝트 구조

```
portal-universe/
├── services/                   # 백엔드 마이크로서비스 (Java 17, Spring Boot 3.5.5)
│   ├── common-library/         # 공유 라이브러리: ErrorCode, ApiResponse, GlobalExceptionHandler
│   ├── config-service/         # 외부 설정 서버 - 포트 8888
│   ├── api-gateway/            # Spring Cloud Gateway (WebFlux) - 포트 8080
│   ├── auth-service/           # OAuth2 인증 서버 (MySQL, Kafka) - 포트 8081
│   ├── blog-service/           # 블로그 CRUD (MongoDB, S3) - 포트 8082
│   ├── shopping-service/       # 전자상거래 (MySQL, Feign) - 포트 8083
│   └── notification-service/   # Kafka 컨슈머 - 포트 8084
├── frontend/                   # 마이크로 프론트엔드 (Vue 3, Vite 7.x)
│   ├── design-system/          # 공유 컴포넌트 (@portal/design-system) - 포트 30003
│   ├── portal-shell/           # Host 애플리케이션 (Vue) - 포트 30000
│   ├── blog-frontend/          # Remote 모듈 (Vue) - 포트 30001
│   └── shopping-frontend/      # Remote 모듈 (React, 미완성) - 포트 30002
├── k8s/                        # Kubernetes 매니페스트
├── monitoring/                 # Prometheus, Grafana 설정
└── docker-compose.yml          # 로컬 개발 환경
```

## 빌드 명령어

### 백엔드 (Gradle)
```bash
./gradlew build                              # 전체 서비스 빌드
./gradlew :services:auth-service:build       # 특정 서비스 빌드
./gradlew :services:auth-service:bootRun     # 특정 서비스 실행
./gradlew test                               # 전체 테스트 실행
./gradlew :services:auth-service:test        # 특정 서비스 테스트
./gradlew bootBuildImage                     # Docker 이미지 생성
```

### 프론트엔드 (npm workspaces)
```bash
cd frontend
npm install                    # 전체 워크스페이스 의존성 설치
npm run dev                    # 전체 앱 개발 모드 실행
npm run dev:portal             # portal-shell만 실행 (포트 30000)
npm run dev:blog               # blog-frontend만 실행 (포트 30001)
npm run build                  # 전체 앱 빌드
npm run test                   # Vitest 단위 테스트 실행
npm run test:e2e               # Playwright E2E 테스트 실행
npm run storybook              # Storybook 실행 (포트 6006)
```

### Docker Compose
```bash
docker-compose up -d           # 전체 스택 실행
docker-compose logs -f <service>  # 로그 확인
```

## 아키텍처

### 백엔드 마이크로서비스 (services/)
- **config-service** (8888): Spring Cloud Config, Git 기반 설정 관리 (https://github.com/L-a-z-e/portal-universe-config-repo.git)
- **api-gateway** (8080): Spring Cloud Gateway (WebFlux) + OAuth2 Resource Server
- **auth-service** (8081): OAuth2 Authorization Server, JWT 토큰 발급 → MySQL, Kafka
- **blog-service** (8082): 블로그 CRUD → MongoDB, S3
- **shopping-service** (8083): 이커머스 → MySQL, Feign Client
- **notification-service** (8084): Kafka 이벤트 컨슈머
- **common-library**: 공유 DTO, 예외 처리, 유틸리티

### Discovery Service (Eureka) 제거 사유
이 프로젝트에서는 Eureka 기반 서비스 디스커버리를 제거했습니다:
- Docker Compose와 Kubernetes는 자체 DNS를 통한 서비스 디스커버리를 제공함
- 로컬 개발 환경에서 서비스 스케일아웃이 필요하지 않음
- 서비스 간 통신은 Docker/K8s의 서비스명 기반 DNS로 충분히 해결됨

### 프론트엔드 마이크로 프론트엔드 (frontend/)
- **portal-shell** (30000): Module Federation Host, 메인 앱 셸 (Vue 3)
- **blog-frontend** (30001): Remote 모듈 (Vue 3), 듀얼 모드 지원
- **shopping-frontend** (30002): Remote 모듈 (React 18, 미완성)
- **design-system** (30003): 공유 Vue 컴포넌트 + TailwindCSS

Module Federation을 통해 런타임에 remote 모듈이 portal-shell에 동적으로 통합됩니다.

### 핵심 아키텍처 패턴
- **이벤트 기반**: 서비스 간 비동기 통신은 Kafka를 통해 처리 (직접 호출 지양)
- **폴리글랏 퍼시스턴스**: auth/shopping은 MySQL, blog는 MongoDB 사용
- **API Gateway**: JWT 검증, 라우팅, CORS 처리를 담당하는 단일 진입점
- **마이크로 프론트엔드**: Module Federation을 통한 프론트엔드 모듈 독립 배포
- **서비스 간 통신**: Feign Client (동기), Kafka (비동기)

### Spring 프로필
- `local`: 로컬 개발 (기본값)
- `docker`: Docker Compose 환경
- `k8s`: Kubernetes 환경

## 에러 처리 아키텍처

### 패턴: ErrorCode 인터페이스 → Enum → CustomBusinessException → GlobalExceptionHandler

**서비스별 에러코드 접두사:**
| 서비스 | 접두사 | 예시 |
|--------|--------|------|
| Common | C | C001, C002, C003 |
| Auth | A | A001 |
| Blog | B | B001, B002, B003 |
| Shopping | S | S001 |

### 에러 처리 핵심 파일
```
services/common-library/.../exception/
├── ErrorCode.java              # 에러코드 인터페이스
├── CommonErrorCode.java        # 공통 에러코드 Enum
├── CustomBusinessException.java # 커스텀 비즈니스 예외
└── GlobalExceptionHandler.java  # 전역 예외 핸들러

services/common-library/.../response/
└── ApiResponse.java            # 통일된 응답 래퍼
```

## 백엔드 코드 패턴

### Controller - ApiResponse.success() 래퍼 사용
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable String id) {
    return ResponseEntity.ok(ApiResponse.success(postService.getPost(id)));
}
```

### Service - @Transactional, CustomBusinessException 발생
```java
@Transactional
public PostResponse createPost(PostRequest request) {
    if (isDuplicate(request.getTitle())) {
        throw new CustomBusinessException(BlogErrorCode.DUPLICATE_TITLE);
    }
    // ...
}
```

### 서비스 간 통신
- **동기 (Feign Client)**: shopping-service → auth-service 사용자 조회
- **비동기 (Kafka)**: auth-service → notification-service 이벤트 발행

## 프론트엔드 코드 패턴

### 디자인 토큰 (3계층)
| Layer | 명칭 | 예시 | 용도 |
|-------|------|------|------|
| 1 | Base | `green-600`, `spacing-4` | 원시 값 |
| 2 | Semantic | `brand-primary`, `text-body` | 역할 기반 |
| 3 | Component | 컴포넌트 클래스에서 적용 | 실제 사용 |

**서비스별 테마:** `data-service="blog"` 또는 `data-service="shopping"` 속성으로 전환

### Module Federation 구조
- **Host (portal-shell)**: `apiClient`, `authStore` 노출
- **Remote (blog-frontend)**: `bootstrap` 함수 노출, 독립 실행/통합 모드 듀얼 지원

### design-system 컴포넌트 (Vue only, 8개)
Button, Card, Badge, Input, Modal, Tag, Avatar, SearchBar

## 기술 스택

### 백엔드
- Java 17, Spring Boot 3.5.5, Spring Cloud 2025.0.0
- Spring Security + OAuth2 Authorization Server
- Resilience4j (Circuit Breaker)
- Micrometer + Prometheus (메트릭)
- Zipkin (분산 추적)

### 프론트엔드
- Vue 3 (Composition API + `<script setup>`)
- Vite 7.x + @originjs/vite-plugin-federation
- TypeScript 5.9, Pinia, Vue Router 4
- oidc-client-ts (인증)

### 인프라
- Kafka 4.1.0 (KRaft 모드, Zookeeper 없음)
- MySQL 8.0, MongoDB
- Docker, Kubernetes (로컬은 Kind 사용)
- Prometheus, Grafana, Zipkin

## 데이터베이스 스키마

### auth-service (MySQL)
- `users`: 사용자 계정 정보
- `user_profiles`: 사용자 프로필 상세
- `social_accounts`: 소셜 로그인 연동

### blog-service (MongoDB)
- `posts`: 블로그 게시물
- `comments`: 댓글
- `tags`: 태그
- `series`: 시리즈

### shopping-service (MySQL)
- `products`: 상품 정보

## 서비스 URL (로컬 개발)

| 서비스 | URL |
|--------|-----|
| API Gateway | http://localhost:8080 |
| Config Server | http://localhost:8888 |
| Portal Shell | http://localhost:30000 |
| Blog Frontend | http://localhost:30001 |
| Shopping Frontend | http://localhost:30002 |
| Design System | http://localhost:30003 |
| Storybook | http://localhost:6006 |
| Grafana | http://localhost:3000 (admin/password) |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |

## API 라우팅 (Gateway 경유)

```
/api/v1/auth/**          → auth-service
/api/v1/blog/**          → blog-service
/api/v1/shopping/**      → shopping-service
/api/v1/notifications/** → notification-service
```

## 개발 참고사항

### 인증 흐름
1. 프론트엔드에서 oidc-client-ts로 auth-service에 인증 요청
2. auth-service가 Spring Authorization Server를 통해 JWT 토큰 발급
3. API Gateway가 들어오는 요청의 JWT 검증
4. 토큰은 프론트엔드에 저장되고 axios interceptor를 통해 API 호출 시 자동 첨부

### 새 백엔드 서비스 추가 시
1. `services/` 하위에 모듈 생성
2. `settings.gradle`에 추가
3. `docker-compose.yml` 및 k8s 매니페스트에 설정
4. api-gateway에 라우팅 규칙 추가

### 새 프론트엔드 모듈 추가 시
1. `frontend/` 하위에 앱 생성
2. `frontend/package.json` workspaces에 추가
3. vite.config.ts에 Module Federation 설정 (remote expose)
4. portal-shell의 Module Federation 설정에 등록 (remote consume)

## 현재 미완성 항목

### shopping-frontend (React)
- 부트스트랩 함수만 구현됨
- design-system이 React 미지원 (Vue 컴포넌트만 존재)

### design-system
- Vue 컴포넌트만 존재 (8개)
- React 래퍼 또는 별도 React 컴포넌트 필요

## Agent 시스템

이 프로젝트는 16개의 전문화된 Claude Code Agent를 통해 개발 작업을 지원합니다.

### 사용법
```
/[agent명] [작업 내용]
```

### Agent 목록

#### Core Development
| Agent | 명령어 | 역할 | Model |
|-------|--------|------|-------|
| Backend | `/backend` | Java/Spring API, 비즈니스 로직 | sonnet |
| Frontend | `/frontend` | Vue/React, UI 컴포넌트, 상태관리 | sonnet |
| Database | `/database` | 스키마 설계, 쿼리 최적화, 마이그레이션 | sonnet |
| DevOps | `/devops` | Docker, K8s, CI/CD, 모니터링 | sonnet |

#### Architecture & Design
| Agent | 명령어 | 역할 | Model |
|-------|--------|------|-------|
| Architect | `/architect` | 시스템 설계, 서비스 경계, 패턴 선택 | opus |
| Security | `/security` | 인증/인가, 취약점 분석, 암호화 | sonnet |
| API Designer | `/api-designer` | REST/GraphQL 설계, 버저닝, 계약 | sonnet |
| Designer | `/designer` | 디자인 시스템, 컴포넌트 스타일링 | sonnet |

#### Quality & Testing
| Agent | 명령어 | 역할 | Model |
|-------|--------|------|-------|
| Tester | `/tester` | Unit, Integration, E2E 테스트 작성 | sonnet |
| Reviewer | `/reviewer` | 코드 리뷰, 베스트 프랙티스, 리팩토링 | sonnet |
| Performance | `/performance` | 프로파일링, 병목 분석, 최적화 | sonnet |

#### Knowledge & Learning
| Agent | 명령어 | 역할 | Model |
|-------|--------|------|-------|
| Tutor | `/tutor` | 개념 설명, 코드 해석, 온보딩 | haiku |
| Analyst | `/analyst` | 기술 Trade-off, 장단점 비교 분석 | sonnet |
| Documenter | `/documenter` | ADR, API 명세, README 작성 | haiku |

#### Management
| Agent | 명령어 | 역할 | Model |
|-------|--------|------|-------|
| PM | `/pm` | 요구사항 정리, 태스크 분해, 우선순위 | sonnet |
| Orchestrator | `/orchestrator` | Agent 간 협업 조율, 워크플로우 관리 | sonnet |

### 사용 예시

#### 단순 작업 (단일 Agent)
```
/backend auth-service에 비밀번호 변경 API 추가해줘
/frontend 로그인 폼에 유효성 검사 추가해줘
/database 주문 테이블 인덱스 최적화해줘
```

#### 복합 작업 (Orchestrator 활용)
```
/orchestrator 새로운 결제 서비스를 추가하고 싶어
```
→ Orchestrator가 실행 계획을 제시하고, 사용자 승인 후 순차적으로 Agent 호출

### Model 선택 기준
| Model | 용도 | 사용 Agent |
|-------|------|-----------|
| haiku | 빠른 응답, 단순 설명/문서화 | tutor, documenter |
| sonnet | 균형잡힌 성능, 대부분의 개발 작업 | 대부분의 agent |
| opus | 복잡한 의사결정, 시스템 설계 | architect |

### 토큰 최적화 규칙
- 같은 파일 반복 읽기 금지
- 불필요한 전체 탐색 금지
- 이전 대화에서 확인한 정보 재활용
