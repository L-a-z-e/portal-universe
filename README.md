# Portal Universe - 마이크로서비스 플랫폼

**Spring Boot 3.x**, **Vue 3**, **Kubernetes**로 구축한 포괄적인 **풀스택 마이크로서비스 플랫폼**으로, 현대적인 분산 시스템 아키텍처와 클라우드 네이티브 기술을 시연합니다.

## 🎯 프로젝트 개요

**Portal Universe**는 마이크로서비스 아키텍처의 완전한 구현을 시연하는 엔터프라이즈급 마이크로서비스 플랫폼입니다. 이 플랫폼은 여러 개의 독립적인 백엔드 서비스와 정교한 마이크로 프론트엔드 아키텍처를 결합하며, 클라우드 인프라 자동화, 포괄적인 모니터링, 분산 추적 기능을 갖추고 있습니다.

이 프로젝트는 다음을 시연합니다:
- **마이크로서비스 아키텍처**: 8개의 독립적으로 배포 가능한 서비스
- **클라우드 네이티브 설계**: Docker 컨테이너화 & Kubernetes 오케스트레이션
- **분산 시스템 패턴**: 서비스 디스커버리, API 게이트웨이, Circuit Breaker, Config Server
- **보안**: OAuth2/JWT 인증, Spring Security 통합
- **관찰성**: Prometheus 메트릭, Grafana 대시보드, Zipkin 분산 추적
- **메시지 기반 아키텍처**: Kafka 기반 비동기 통신
- **마이크로 프론트엔드 아키텍처**: Module Federation을 통한 독립적인 프론트엔드 애플리케이션
- **데이터베이스 다양성**: MySQL, MongoDB, Redis를 통한 다양한 데이터 저장소

---

## 📊 아키텍처 개요

### 시스템 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                     프론트엔드 레이어                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Portal Shell │  │ Blog Frontend │  │ Shopping FE  │          │
│  │  (Vue 3)     │  │   (Vue 3)     │  │   (Vue 3)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP/REST
┌──────────────────────────▼──────────────────────────────────────┐
│                     API 게이트웨이                                │
│        (Spring Cloud Gateway + OAuth2)                          │
│      • 라우팅 • 인증 • 로드 밸런싱                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┬──────────────────┐
        │                  │                  │                  │
┌───────▼──────┐   ┌──────▼──────┐  ┌──────▼──────┐   ┌────────▼─┐
│인증 서비스    │   │블로그 서비스  │  │쇼핑 서비스   │   │알림 서비스│
│  (MySQL)     │   │ (MongoDB)   │  │  (MySQL)    │   │ (Kafka)  │
└───────┬──────┘   └──────┬──────┘  └──────┬──────┘   └────────┬─┘
        │                  │                  │                  │
└───────┴──────────────────┴──────────────────┴──────────────────┘
                           │
        ┌──────────────────┼──────────────────────┐
        │                  │                      │
┌───────▼──────┐  ┌───────▼────────┐  ┌─────────▼──┐
│ 서비스 디스커버리│ 설정 서버        │  │공용 라이브러리│
│  (Eureka)    │  │ (Git 기반)     │  │  (공유)     │
└──────────────┘  └────────────────┘  └────────────┘

데이터 레이어:
MySQL ← 인증/쇼핑 서비스
MongoDB ← 블로그 서비스
Kafka ← 비동기 메시지 큐
Redis ← 캐싱 (선택사항)

관찰성 스택:
Prometheus → Zipkin → Grafana 대시보드
Actuator 헬스 체크
```

---

## 🏗️ 프로젝트 구조

```
portal-universe/
├── services/                      # 백엔드 마이크로서비스 (Java/Spring Boot 3.x)
│   ├── api-gateway/              # API 게이트웨이 & 요청 라우팅
│   │   └── Spring Cloud Gateway + OAuth2 Resource Server
│   ├── auth-service/             # 인증 & 인가 서비스
│   │   └── 사용자 관리, JWT 토큰 생성
│   ├── blog-service/             # 블로그 콘텐츠 관리 서비스
│   │   └── 포스트, 댓글 (MongoDB)
│   ├── shopping-service/         # 전자상거래 쇼핑 서비스
│   │   └── 상품, 주문, 재고 (MySQL)
│   ├── notification-service/     # 비동기 알림 서비스
│   │   └── Kafka 기반 이벤트 드리븐
│   ├── discovery-service/        # 서비스 레지스트리 (Netflix Eureka)
│   │   └── 서비스 디스커버리 & 헬스 체크
│   ├── config-service/           # 분산 설정 서버
│   │   └── 중앙화된 설정 관리
│   └── common-library/           # 공유 컴포넌트 & 유틸리티
│
├── frontend/                      # 프론트엔드 애플리케이션 (Vue 3)
│   ├── portal-shell/             # 셸 애플리케이션 (Module Federation Host)
│   │   └── 메인 진입점, 네비게이션, 레이아웃
│   ├── blog-frontend/            # 블로그 원격 모듈
│   │   └── 블로그 특화 UI 컴포넌트
│   ├── shopping-frontend/        # 쇼핑 원격 모듈
│   │   └── 쇼핑 특화 UI 컴포넌트
│   └── design-system/            # 공유 컴포넌트 라이브러리
│       └── UI 컴포넌트, 스타일, 디자인 토큰
│
├── k8s/                          # Kubernetes 설정
│   ├── base/                     # 기본 Kubernetes 매니페스트
│   ├── infrastructure/           # 인프라 설정 (MySQL, MongoDB, Kafka 등)
│   ├── services/                 # 서비스별 K8s 설정
│   └── scripts/                  # K8s 배포 자동화 스크립트
│
├── monitoring/                   # 관찰성 스택 설정
│   ├── prometheus.yml            # Prometheus 메트릭 스크래핑 설정
│   └── grafana/                  # Grafana 대시보드 & 프로비저닝
│
├── scripts/                      # 유틸리티 스크립트
│
├── docker-compose.yml            # 로컬 개발 오케스트레이션
├── build.gradle                  # 루트 Gradle 설정 (멀티 모듈)
├── settings.gradle               # 모듈 정의
├── gradlew                        # Gradle wrapper
└── .github/                       # GitHub 워크플로우 & 설정
```

---

## 🛠️ 기술 스택

### 백엔드
| 분류 | 기술 | 목적 |
|------|------|------|
| **런타임** | Java 17 | JDK 버전 |
| **프레임워크** | Spring Boot 3.5.5 | 애플리케이션 프레임워크 |
| **클라우드** | Spring Cloud 2025.0.0 | 분산 시스템 지원 |
| **API 게이트웨이** | Spring Cloud Gateway | 요청 라우팅 & 로드 밸런싱 |
| **서비스 디스커버리** | Netflix Eureka | 서비스 레지스트리 |
| **설정 관리** | Spring Cloud Config | 중앙화된 설정 |
| **보안** | Spring Security + OAuth2 | 인증 & 인가 |
| **Circuit Breaker** | Resilience4j | 장애 허용성 |
| **빌드 도구** | Gradle 7+ | 프로젝트 빌드 & 의존성 관리 |

### 프론트엔드
| 분류 | 기술 | 목적 |
|------|------|------|
| **프레임워크** | Vue 3 | Progressive JavaScript 프레임워크 |
| **빌드 도구** | Vite 7.x | 초고속 빌드 도구 |
| **모듈 시스템** | Module Federation | 마이크로 프론트엔드 오케스트레이션 |
| **테스팅** | Vitest + Playwright | 단위 & E2E 테스팅 |
| **타입 안전성** | TypeScript 5.9 | 정적 타입 검사 |
| **컴포넌트** | Storybook | 컴포넌트 문서화 |

### 데이터 & 메시징
| 기술 | 용도 |
|------|------|
| **MySQL 8.0** | 인증 & 쇼핑 서비스 (관계형 데이터) |
| **MongoDB** | 블로그 서비스 (문서 저장소) |
| **Kafka 4.1.0** | 비동기 메시징 & 이벤트 스트림 |
| **Redis** | 캐싱 레이어 (선택사항) |

### 인프라 & 배포
| 기술 | 목적 |
|------|------|
| **Docker** | 컨테이너 오케스트레이션 |
| **Kubernetes** | 컨테이너 오케스트레이션 & 자동 확장 |
| **Kind** | 로컬 K8s 개발 클러스터 |
| **LocalStack** | 로컬 개발용 AWS S3 시뮬레이션 |

### 관찰성 & 모니터링
| 기술 | 목적 |
|------|------|
| **Prometheus** | 메트릭 수집 & 저장 |
| **Grafana** | 시각화 & 대시보드 |
| **Zipkin** | 분산 추적 |
| **Micrometer** | 메트릭 계측 |
| **Spring Boot Actuator** | 헬스 체크 & 메트릭 엔드포인트 |

---

## 🚀 빠른 시작 가이드

### 사전 요구사항
- **Java 17+**
- **Docker & Docker Compose**
- **Node.js 18+** 및 **npm 8+**
- **kubectl** (Kubernetes 배포용)
- **Git**

### 1. Docker Compose를 이용한 로컬 개발

#### 저장소 클론
```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe
```

#### 인프라 시작
```bash
# Docker Compose로 모든 서비스 시작
docker-compose up -d

# 모든 서비스가 실행 중인지 확인
docker-compose ps

# 특정 서비스의 로그 확인
docker-compose logs -f api-gateway
```

#### 서비스 접근
- **API 게이트웨이**: http://localhost:8080
- **서비스 디스커버리 (Eureka)**: http://localhost:8761
- **설정 서버**: http://localhost:8888
- **인증 서비스**: http://localhost:8081
- **블로그 서비스**: http://localhost:8082
- **쇼핑 서비스**: http://localhost:8083
- **알림 서비스**: http://localhost:8084
- **포탈 셸 (프론트엔드)**: http://localhost:30000
- **블로그 프론트엔드**: http://localhost:30001
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/password)
- **Zipkin**: http://localhost:9411

### 2. 백엔드 개발

#### 모든 서비스 빌드
```bash
# Gradle wrapper 사용
./gradlew build

# 특정 서비스 빌드
./gradlew :services:auth-service:build
```

#### 개별 서비스 실행 (Docker 없이)
```bash
# 터미널 1: 서비스 디스커버리 시작
./gradlew :services:discovery-service:bootRun

# 터미널 2: 설정 서버 시작
SPRING_PROFILES_ACTIVE=local ./gradlew :services:config-service:bootRun

# 터미널 3: API 게이트웨이 시작
SPRING_PROFILES_ACTIVE=local ./gradlew :services:api-gateway:bootRun

# 터미널 4: 인증 서비스 시작
SPRING_PROFILES_ACTIVE=local ./gradlew :services:auth-service:bootRun
```

#### 애플리케이션 프로필
- **local**: 로컬 개발 (기본값)
- **docker**: Docker Compose 환경
- **k8s**: Kubernetes 환경

### 3. 프론트엔드 개발

#### 프론트엔드 설정
```bash
cd frontend

# 의존성 설치
npm install

# 모든 애플리케이션 개발 서버 시작
npm run dev

# 특정 애플리케이션 시작
npm run dev:portal          # 포탈 셸
npm run dev:blog           # 블로그 프론트엔드
npm run dev:shopping       # 쇼핑 프론트엔드
npm run dev:design         # 디자인 시스템
```

#### 프론트엔드 빌드
```bash
# 모든 애플리케이션 빌드
npm run build

# Docker용 빌드
npm run build:blog:docker
npm run build:shopping:docker

# Kubernetes용 빌드
npm run build:blog:k8s
npm run build:shopping:k8s
```

#### Storybook (컴포넌트 문서화)
```bash
npm run storybook    # Storybook 개발 서버 시작 (포트 6006)
npm run build-storybook    # 정적 Storybook 빌드
```

---

## 📋 서비스 문서

### 핵심 서비스

#### 🔐 **인증 서비스** (포트: 8081)
**목적**: 사용자 인증 및 인가

**주요 기능**:
- 사용자 등록 & 로그인
- JWT 토큰 생성
- OAuth2 통합
- 역할 기반 접근 제어 (RBAC)
- 비밀번호 암호화

**데이터베이스**: MySQL
**API 엔드포인트**:
- `POST /auth/api/v1/users/register` - 사용자 등록
- `POST /auth/api/v1/users/login` - 사용자 로그인
- `POST /auth/api/v1/users/logout` - 사용자 로그아웃
- `GET /auth/api/v1/users/me` - 현재 사용자 정보 조회

---

#### 📰 **블로그 서비스** (포트: 8082)
**목적**: 블로그 콘텐츠 관리

**주요 기능**:
- 블로그 포스트 CRUD
- 댓글 시스템
- 태그 관리
- 검색 기능
- 콘텐츠 중재

**데이터베이스**: MongoDB
**API 엔드포인트**:
- `GET /blog/api/v1/posts` - 모든 포스트 목록 조회
- `POST /blog/api/v1/posts` - 새 포스트 작성
- `GET /blog/api/v1/posts/{id}` - 포스트 상세 조회
- `PUT /blog/api/v1/posts/{id}` - 포스트 수정
- `DELETE /blog/api/v1/posts/{id}` - 포스트 삭제

---

#### 🛍️ **쇼핑 서비스** (포트: 8083)
**목적**: 전자상거래 및 상품 관리

**주요 기능**:
- 상품 카탈로그 관리
- 장바구니 기능
- 주문 처리
- 재고 관리
- 결제 통합

**데이터베이스**: MySQL
**API 엔드포인트**:
- `GET /shopping/api/v1/products` - 상품 목록 조회
- `POST /shopping/api/v1/orders` - 주문 생성
- `GET /shopping/api/v1/orders/{id}` - 주문 상세 조회
- `PUT /shopping/api/v1/cart` - 장바구니 업데이트

---

#### 🔔 **알림 서비스** (포트: 8084)
**목적**: 비동기 이벤트 기반 알림

**주요 기능**:
- 이메일 알림
- 인앱 알림
- Kafka 기반 이벤트 메시징
- 알림 템플릿
- 배송 추적

**메시지 큐**: Kafka
**소비 이벤트**:
- `user.registered` - 환영 이메일 발송
- `order.created` - 주문 확인 메시지
- `post.published` - 팔로워 알림

---

### 인프라 서비스

#### 🔍 **API 게이트웨이** (포트: 8080)
**Spring Cloud Gateway** with OAuth2 Resource Server

**책임사항**:
- 적절한 마이크로서비스로의 요청 라우팅
- 요청/응답 필터링
- JWT 토큰을 통한 인증
- 레이트 리미팅
- 로드 밸런싱
- CORS 처리

**주요 설정**:
```yaml
# 경로 패턴에 따른 트래픽 라우팅
/api/v1/auth/** → 인증 서비스
/api/v1/blog/** → 블로그 서비스
/api/v1/shopping/** → 쇼핑 서비스
/api/v1/notifications/** → 알림 서비스
```

---

#### 📡 **서비스 디스커버리 (Eureka)** (포트: 8761)
**Netflix Eureka** 서비스 레지스트리

**책임사항**:
- 서비스 등록 & 해제
- 서비스 인스턴스 디스커버리
- 헬스 체크 모니터링
- 로드 밸런싱 메타데이터

**기능**:
- 자동 서비스 등록
- 자가 치유 기능
- 하트비트 기반 헬스 모니터링

---

#### ⚙️ **설정 서버** (포트: 8888)
**Spring Cloud Config Server**

**책임사항**:
- 중앙화된 설정 관리
- Git 기반 설정 저장소
- 동적 설정 업데이트
- 환경별 프로필 관리

**설정 저장소**: https://github.com/L-a-z-e/portal-universe-config-repo.git

**사용 방법**:
```bash
# 설정 조회
curl http://localhost:8888/auth-service/docker

# 설정 새로고침 (actuator 엔드포인트 필요)
curl -X POST http://localhost:8080/actuator/refresh
```

---

#### 📚 **공용 라이브러리**
**공유 컴포넌트 & 유틸리티**

**포함 내용**:
- 공통 DTO (Data Transfer Objects)
- 커스텀 예외 처리
- 유틸리티 함수
- 횡단 관심사 (Cross-cutting Concerns)
- 공유 설정

---

## 📊 관찰성 & 모니터링

### Prometheus 메트릭
모든 서비스는 `micrometer-registry-prometheus`를 통해 메트릭을 노출합니다:
```
GET http://localhost:{service-port}/actuator/prometheus
```

**주요 메트릭**:
- HTTP 요청 속도, 지연 시간, 에러
- 데이터베이스 연결 풀 메트릭
- JVM 메트릭 (메모리, GC, 스레드)
- 커스텀 비즈니스 메트릭

### Grafana 대시보드
Grafana 접근: http://localhost:3000 (admin/password)

**사전 구성된 대시보드**:
- 서비스 헬스 개요
- API 요청 메트릭
- 데이터베이스 성능
- JVM 모니터링
- 에러율 분석

### Zipkin 분산 추적
Zipkin 접근: http://localhost:9411

**기능**:
- 서비스 전체 요청 추적
- 서비스 의존성 맵핑
- 지연 시간 분석
- 에러 추적

**통합**: Micrometer Tracing Bridge + Brave

---

## 🐳 Docker & 컨테이너 배포

### Docker Compose 서비스
`docker-compose.yml`은 다음을 오케스트레이션합니다:
- **8개 마이크로서비스**: 모든 백엔드 서비스
- **2개 프론트엔드 애플리케이션**: 포탈 셸 + 블로그 프론트엔드
- **3개 데이터 저장소**: MySQL, MongoDB, Kafka
- **3개 모니터링 도구**: Prometheus, Grafana, Zipkin
- **인프라**: LocalStack (AWS S3 시뮬레이션)

### 커스텀 Docker 이미지 빌드
```bash
# 모든 서비스 빌드
./gradlew bootBuildImage

# 특정 서비스 빌드
./gradlew :services:auth-service:bootBuildImage

# 프론트엔드 빌드
cd frontend && npm run build:shopping:docker
```

### Docker 네트워크
모든 서비스는 `portal-universe-net` 브릿지 네트워크로 통신합니다

---

## ☸️ Kubernetes 배포

### Kubernetes 설정 구조
```
k8s/
├── base/                    # 공통 기본 설정
├── infrastructure/          # 데이터베이스, Kafka, 모니터링 서비스
├── services/                # 서비스별 매니페스트
└── scripts/                 # 자동화 스크립트
```

### 배포 단계

#### 1. Kind 클러스터 생성 (로컬 Kubernetes)
```bash
# 클러스터 생성
kind create cluster --name portal-universe

# 클러스터 확인
kubectl cluster-info
kubectl get nodes
```

#### 2. 인프라 배포
```bash
# MySQL, MongoDB, Kafka 등
kubectl apply -k k8s/infrastructure/
```

#### 3. 마이크로서비스 배포
```bash
# 모든 서비스 배포
kubectl apply -k k8s/services/

# 배포 확인
kubectl get deployments
kubectl get pods
kubectl get svc
```

#### 4. 서비스 접근
```bash
# API 게이트웨이로 포트 포워딩
kubectl port-forward svc/api-gateway 8080:8080

# Grafana로 포트 포워딩
kubectl port-forward svc/grafana 3000:3000

# Prometheus로 포트 포워딩
kubectl port-forward svc/prometheus 9090:9090
```

### Kubernetes 기능
- **Deployment**: 레플리카, 롤링 업데이트
- **Service**: ClusterIP, NodePort, LoadBalancer
- **ConfigMap**: 설정 관리
- **Secret**: 민감한 데이터 (자격증명, 키)
- **PersistentVolume**: 데이터 지속성
- **StatefulSet**: 상태저장 서비스 (데이터베이스)
- **Ingress**: 외부 트래픽 라우팅

---

## 🔐 보안 기능

### OAuth2 & JWT
- **토큰 기반 인증**: 무상태 JWT 토큰
- **리소스 서버**: API 게이트웨이 토큰 검증
- **토큰 갱신**: 자동 토큰 갱신
- **스코프 기반 인가**: 세분화된 권한

### Spring Security
- CORS 정책 설정
- CSRF 보호
- 보안 헤더 (HSTS, X-Frame-Options 등)
- 메서드 레벨 보안 주석

### 네트워크 보안
- 서비스 간 HTTP 통신
- API 게이트웨이 단일 진입점
- Kubernetes 네트워크 정책 (선택사항)

---

## 🧪 테스팅 전략

### 백엔드 테스팅
```bash
# 모든 테스트 실행
./gradlew test

# 특정 서비스 테스트 실행
./gradlew :services:auth-service:test

# 테스트 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

**테스트 유형**:
- 단위 테스트: 서비스, 컨트롤러, 저장소 레이어
- 통합 테스트: 데이터베이스, 메시지 큐 상호작용
- 엔드투엔드 테스트: API 엔드포인트 테스팅

### 프론트엔드 테스팅
```bash
# 단위 테스트
npm run test

# E2E 테스트 (Playwright)
npm run test:e2e

# 커버리지 리포트
npm run test:coverage
```

---

## 📝 설정 관리

### 애플리케이션 프로퍼티
설정 파일은 외부 저장소에서 관리됩니다:
https://github.com/L-a-z-e/portal-universe-config-repo.git

**설정 프로필**:
```
application-local.yml      # 로컬 개발
application-docker.yml     # Docker Compose 환경
application-k8s.yml        # Kubernetes 환경
```

### 환경 변수
```bash
# Spring 프로필 설정
SPRING_PROFILES_ACTIVE=docker

# 데이터베이스 자격증명
MYSQL_USER=laze
MYSQL_PASSWORD=password

# Kafka 설정
KAFKA_BROKERS=kafka:29092
```

---

## 🤝 기여하기

### 개발 워크플로우
1. **저장소 포크**
2. **기능 브랜치 생성**: `git checkout -b feature/your-feature`
3. **변경사항 커밋**: `git commit -am 'Add new feature'`
4. **브랜치 푸시**: `git push origin feature/your-feature`
5. **풀 리퀘스트 제출**

### 코드 스타일
- Java: Spring Boot 컨벤션 준수
- 프론트엔드: Vue 3 스타일 가이드
- 포매팅: Gradle format 태스크, JS는 Prettier

### 테스팅 요구사항
- 모든 새로운 기능에는 테스트 필수
- 70% 이상의 코드 커버리지 유지
- PR 제출 전 `./gradlew build` 실행

---

## 🎓 학습 자료

### 마이크로서비스 아키텍처
- [Spring Cloud 공식 문서](https://spring.io/projects/spring-cloud)
- [마이크로서비스 패턴](https://microservices.io)
- [Building Microservices by Sam Newman](https://samnewman.io/books/building_microservices/)

### 프론트엔드 아키텍처
- [Vue 3 공식 문서](https://vuejs.org/)
- [Webpack Module Federation](https://webpack.js.org/concepts/module-federation/)
- [Vite 공식 문서](https://vitejs.dev/)

### 클라우드 & DevOps
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Docker 공식 문서](https://docs.docker.com/)
- [12 Factor App](https://12factor.net/)

---

## 📞 지원 및 연락처

- **GitHub Issues**: [Portal Universe Issues](https://github.com/L-a-z-e/portal-universe/issues)
- **이메일**: [yysi8771@gmail.com]

---

## 🎯 향후 로드맵

- [ ] GraphQL API 구현
- [ ] 고급 캐싱 전략 (Redis)
- [ ] API 버전 관리 전략
- [ ] ELK 스택 고급 로깅
- [ ] 머신러닝 서비스 통합
- [ ] 모바일 애플리케이션 (React Native)
- [ ] 이벤트 소싱 구현
- [ ] CQRS 패턴 도입
- [ ] 향상된 보안 (mTLS, 서비스 메시)
- [ ] 프로덕션 준비 CI/CD 파이프라인

---

**마지막 업데이트**: 2025년 12월  
**현재 버전**: 0.0.1-SNAPSHOT  
**Java 버전**: 17+  
**Spring Boot 버전**: 3.5.5  
**Spring Cloud 버전**: 2025.0.0
