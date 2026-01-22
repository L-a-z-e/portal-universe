# Portal Universe 학습 자료

Portal Universe 프로젝트의 기술 스택과 아키텍처를 이해하기 위한 포괄적인 학습 자료입니다.

---

## 권장 학습 순서

```
1. 마이크로서비스 기초 → Kafka → Redis → MongoDB → Elasticsearch
2. Shopping 도메인 → Blog 도메인
3. Module Federation → Vue/React 패턴
4. API Gateway → 보안
5. 아키텍처 패턴 → Clean Code & 트레이드오프
6. AWS 로컬 개발 → LocalStack → Kubernetes → AWS 배포
```

---

## PART 1: 전체 시스템 학습 자료

### 📚 Apache Kafka

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [kafka-introduction.md](./kafka/kafka-introduction.md) | Kafka 아키텍처, Topic, Partition, Consumer Group | ⭐⭐ |
| [kafka-spring-integration.md](./kafka/kafka-spring-integration.md) | @KafkaListener, KafkaTemplate, DLQ, 에러 처리 | ⭐⭐⭐ |

### 📚 Redis

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [redis-introduction.md](./redis/redis-introduction.md) | 데이터 구조, TTL, 캐싱 패턴 | ⭐⭐ |
| [redis-distributed-lock.md](./redis/redis-distributed-lock.md) | Redisson, @DistributedLock, Lua Script | ⭐⭐⭐⭐ |

### 📚 MongoDB

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [mongodb-introduction.md](./mongodb/mongodb-introduction.md) | Document 모델, Embedding vs Reference | ⭐⭐ |
| [mongodb-spring-integration.md](./mongodb/mongodb-spring-integration.md) | @Document, Repository, Aggregation | ⭐⭐⭐ |

### 📚 Elasticsearch

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [es-introduction.md](./elasticsearch/es-introduction.md) | 역인덱스, Nori 분석기, Query DSL | ⭐⭐⭐ |
| [es-portal-universe.md](./elasticsearch/es-portal-universe.md) | 상품 검색 구현, 자동완성, Faceted Search | ⭐⭐⭐⭐ |

---

## PART 2: Shopping Service

위치: `services/shopping-service/docs/learning/`

### 📦 도메인 설계

| 문서 | 주제 |
|------|------|
| [shopping-domain-overview.md](../../services/shopping-service/docs/learning/domain/shopping-domain-overview.md) | 전체 도메인 맵, Aggregate Root |
| [order-domain.md](../../services/shopping-service/docs/learning/domain/order-domain.md) | Order 상태 머신, 스냅샷 패턴 |
| [inventory-domain.md](../../services/shopping-service/docs/learning/domain/inventory-domain.md) | 3단계 재고 모델, StockMovement |

### 🗄️ 데이터베이스

| 문서 | 주제 |
|------|------|
| [shopping-erd.md](../../services/shopping-service/docs/learning/database/shopping-erd.md) | ERD, 테이블 관계, 인덱스 전략 |

### 💼 비즈니스 로직

| 문서 | 주제 |
|------|------|
| [order-flow.md](../../services/shopping-service/docs/learning/business/order-flow.md) | 주문 전체 흐름, Saga 단계 |
| [inventory-concurrency.md](../../services/shopping-service/docs/learning/business/inventory-concurrency.md) | 재고 동시성 제어, 분산 락 |

---

## PART 3: Blog Service

위치: `services/blog-service/docs/learning/`

### 📦 도메인 설계

| 문서 | 주제 |
|------|------|
| [blog-domain-overview.md](../../services/blog-service/docs/learning/domain/blog-domain-overview.md) | Document 구조, 역정규화 전략 |

---

## PART 4: API Gateway

위치: `services/api-gateway/docs/learning/`

### 🌐 Gateway & 보안

| 문서 | 주제 |
|------|------|
| [spring-cloud-gateway.md](../../services/api-gateway/docs/learning/gateway/spring-cloud-gateway.md) | Route, Predicate, Filter, StripPrefix |
| [circuit-breaker.md](../../services/api-gateway/docs/learning/gateway/circuit-breaker.md) | Resilience4j, 상태 전이, Fallback |
| [jwt-validation.md](../../services/api-gateway/docs/learning/gateway/jwt-validation.md) | JWT 검증, 접근 제어, CORS |

---

## PART 5: Frontend

### 🎨 Module Federation

| 문서 | 주제 | 위치 |
|------|------|------|
| [module-federation-host.md](../../frontend/portal-shell/docs/learning/mfe/module-federation-host.md) | Host 설정, Remote 로딩, 공유 리소스 | portal-shell |
| [module-federation-remote.md](../../frontend/shopping-frontend/docs/learning/mfe/module-federation-remote.md) | Bootstrap 패턴, Keep-Alive, 스타일 격리 | shopping-frontend |

### ⚛️ React 패턴

| 문서 | 주제 | 위치 |
|------|------|------|
| [zustand-state.md](../../frontend/shopping-frontend/docs/learning/react/zustand-state.md) | Zustand Store, 미들웨어, 최적화 | shopping-frontend |

---

## PART 6: 아키텍처 패턴

위치: `docs/learning/patterns/`

### 🏗️ 핵심 패턴

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [saga-pattern-deep-dive.md](./patterns/saga-pattern-deep-dive.md) | Orchestration vs Choreography, 보상 로직 | ⭐⭐⭐⭐ |
| [state-machine-pattern.md](./patterns/state-machine-pattern.md) | Order/Payment 상태 전이, Guard 조건 | ⭐⭐⭐ |
| [portal-universe-patterns.md](./patterns/portal-universe-patterns.md) | 프로젝트 전체 패턴 총정리 | ⭐⭐⭐⭐ |

---

## PART 7: Clean Code & 아키텍처

위치: `docs/learning/clean-code/`

### 📐 설계 원칙

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [solid-principles.md](./clean-code/principles/solid-principles.md) | SOLID 5원칙 (SRP, OCP, LSP, ISP, DIP) | ⭐⭐⭐ |
| [dry-kiss-yagni.md](./clean-code/principles/dry-kiss-yagni.md) | DRY, KISS, YAGNI 실용적 설계 원칙 | ⭐⭐ |
| [clean-code-naming.md](./clean-code/principles/clean-code-naming.md) | 의미 있는 이름 짓기 | ⭐⭐ |
| [clean-code-functions.md](./clean-code/principles/clean-code-functions.md) | 함수 설계 원칙 (크기, 인자, CQS) | ⭐⭐⭐ |
| [clean-code-comments.md](./clean-code/principles/clean-code-comments.md) | 주석 작성 가이드, JavaDoc | ⭐⭐ |
| [error-handling-patterns.md](./clean-code/principles/error-handling-patterns.md) | 에러 처리 패턴, ErrorCode Enum | ⭐⭐⭐ |
| [trade-offs.md](./clean-code/trade-offs.md) | 아키텍처 트레이드오프 분석 | ⭐⭐⭐⭐ |

### 🔧 리팩토링

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [refactoring-techniques.md](./clean-code/refactoring/refactoring-techniques.md) | 5가지 핵심 리팩토링 기법 (Extract Method, Rename, Magic Number, Parameter Object, Polymorphism) | ⭐⭐⭐ |
| [code-review-checklist.md](./clean-code/refactoring/code-review-checklist.md) | 코드 리뷰 체크리스트 (가독성, 성능, 보안, 테스트, 아키텍처) | ⭐⭐⭐ |

---

## PART 8: AWS 로컬 개발

위치: `docs/learning/aws/`

AWS 클라우드 서비스와 LocalStack을 활용한 로컬 개발 환경 가이드입니다.

### ☁️ AWS 기초

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [aws-overview.md](./aws/fundamentals/aws-overview.md) | AWS 개요, 핵심 서비스 카테고리 | ⭐ |
| [region-az.md](./aws/fundamentals/region-az.md) | 리전, 가용영역, 서울 리전 | ⭐ |
| [aws-cli-setup.md](./aws/fundamentals/aws-cli-setup.md) | AWS CLI 설치, 프로필, awslocal | ⭐⭐ |

### 🔐 IAM (Identity & Access Management)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [iam-introduction.md](./aws/iam/iam-introduction.md) | User, Role, Policy 개념 | ⭐⭐ |
| [iam-policies.md](./aws/iam/iam-policies.md) | Policy 문법, 최소 권한 원칙 | ⭐⭐⭐ |
| [iam-best-practices.md](./aws/iam/iam-best-practices.md) | Credentials 관리, MFA | ⭐⭐ |

### 📦 S3 (Simple Storage Service)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [s3-introduction.md](./aws/s3/s3-introduction.md) | 객체 스토리지, 버킷 구조 | ⭐⭐ |
| [s3-operations.md](./aws/s3/s3-operations.md) | CRUD, Pre-signed URL, Multipart | ⭐⭐ |
| [s3-sdk-integration.md](./aws/s3/s3-sdk-integration.md) | AWS SDK v2, Spring Boot 통합 | ⭐⭐⭐ |
| [s3-permissions.md](./aws/s3/s3-permissions.md) | 버킷 정책, ACL, CORS | ⭐⭐⭐ |
| [s3-best-practices.md](./aws/s3/s3-best-practices.md) | 키 네이밍, 스토리지 클래스 | ⭐⭐ |

### 💻 EC2 (Elastic Compute Cloud)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [ec2-introduction.md](./aws/ec2/ec2-introduction.md) | 가상 서버, 인스턴스 타입 | ⭐⭐ |
| [ec2-vs-kubernetes.md](./aws/ec2/ec2-vs-kubernetes.md) | EC2 vs ECS vs EKS 비교 | ⭐⭐⭐ |

### 🧪 LocalStack (핵심)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [localstack-setup.md](./aws/localstack/localstack-setup.md) | Docker Compose 설정, 환경 변수 | ⭐⭐ |
| [localstack-persistence.md](./aws/localstack/localstack-persistence.md) | **데이터 영속성 문제 해결** ⭐핵심⭐ | ⭐⭐⭐ |
| [localstack-services.md](./aws/localstack/localstack-services.md) | S3, SQS, SNS, DynamoDB 설정 | ⭐⭐ |
| [localstack-troubleshooting.md](./aws/localstack/localstack-troubleshooting.md) | 자주 발생하는 문제 및 해결책 | ⭐⭐ |

### 🚀 배포 파이프라인

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [environment-profiles.md](./aws/deployment/environment-profiles.md) | local/docker/k8s 환경별 설정 | ⭐⭐⭐ |
| [local-to-kubernetes.md](./aws/deployment/local-to-kubernetes.md) | 로컬 → Docker → K8s 전환 | ⭐⭐⭐ |
| [kubernetes-to-aws.md](./aws/deployment/kubernetes-to-aws.md) | K8s → AWS 마이그레이션 | ⭐⭐⭐⭐ |

### ✅ 모범 사례

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [aws-migration-checklist.md](./aws/best-practices/aws-migration-checklist.md) | LocalStack → AWS 체크리스트 | ⭐⭐⭐ |

---

## 기존 학습 노트

### 구현 패턴

| 파일명 | 주제 | 관련 서비스 |
|--------|------|-------------|
| [admin-implementation-patterns.md](./admin-implementation-patterns.md) | Admin 기능 구현 패턴 | shopping-service |

### 학습 노트

| 번호 | 파일명 | 주제 | 핵심 기술 |
|----|--------|------|-----------|
| 01 | [01-domain-model.md](./notes/01-domain-model.md) | 도메인 모델 설계 | DDD, Entity 설계 |
| 02 | [02-saga-pattern.md](./notes/02-saga-pattern.md) | Saga 패턴 | Orchestration, 분산 트랜잭션 |
| 03 | [03-concurrency-control.md](./notes/03-concurrency-control.md) | 동시성 제어 | Pessimistic/Optimistic Lock |
| 04 | [04-snapshot-pattern.md](./notes/04-snapshot-pattern.md) | 스냅샷 패턴 | 가격 스냅샷, 이력 관리 |
| 05 | [05-react-fundamentals.md](./notes/05-react-fundamentals.md) | React 기초 | Hooks, Context API |
| 06 | [06-shopping-frontend-implementation.md](./notes/06-shopping-frontend-implementation.md) | Shopping Frontend 구현 | Module Federation, React Router |
| 07 | [07-security-cryptography.md](./notes/07-security-cryptography.md) | 암호화 개념 | AES, RSA, BCrypt, JWT, PKCE, OAuth2 |
| 08 | [08-redis-lua-script-atomicity.md](./notes/08-redis-lua-script-atomicity.md) | Redis Lua 원자성 | Lua Script, 동시성 제어, 선착순 처리 |

---

## 주제별 인덱스

### Backend Infrastructure
- **Kafka**: [소개](./kafka/kafka-introduction.md) | [Spring 통합](./kafka/kafka-spring-integration.md)
- **Redis**: [소개](./redis/redis-introduction.md) | [분산 락](./redis/redis-distributed-lock.md)
- **MongoDB**: [소개](./mongodb/mongodb-introduction.md) | [Spring Data](./mongodb/mongodb-spring-integration.md)
- **Elasticsearch**: [소개](./elasticsearch/es-introduction.md) | [상품 검색](./elasticsearch/es-portal-universe.md)

### Domain Design
- **Shopping**: [개요](../../services/shopping-service/docs/learning/domain/shopping-domain-overview.md) | [Order](../../services/shopping-service/docs/learning/domain/order-domain.md) | [Inventory](../../services/shopping-service/docs/learning/domain/inventory-domain.md)
- **Blog**: [개요](../../services/blog-service/docs/learning/domain/blog-domain-overview.md)

### API Gateway & Security
- **Gateway**: [Spring Cloud Gateway](../../services/api-gateway/docs/learning/gateway/spring-cloud-gateway.md) | [Circuit Breaker](../../services/api-gateway/docs/learning/gateway/circuit-breaker.md)
- **Security**: [JWT 검증](../../services/api-gateway/docs/learning/gateway/jwt-validation.md)

### Frontend
- **MFE**: [Host (Vue)](../../frontend/portal-shell/docs/learning/mfe/module-federation-host.md) | [Remote (React)](../../frontend/shopping-frontend/docs/learning/mfe/module-federation-remote.md)
- **State**: [Zustand](../../frontend/shopping-frontend/docs/learning/react/zustand-state.md)

### Architecture Patterns
- **Patterns**: [Saga 심화](./patterns/saga-pattern-deep-dive.md) | [State Machine](./patterns/state-machine-pattern.md) | [전체 정리](./patterns/portal-universe-patterns.md)
- **Clean Code**: [SOLID](./clean-code/principles/solid-principles.md) | [DRY/KISS/YAGNI](./clean-code/principles/dry-kiss-yagni.md) | [네이밍](./clean-code/principles/clean-code-naming.md) | [함수](./clean-code/principles/clean-code-functions.md) | [주석](./clean-code/principles/clean-code-comments.md) | [에러 처리](./clean-code/principles/error-handling-patterns.md) | [트레이드오프](./clean-code/trade-offs.md) | [리팩토링 기법](./clean-code/refactoring/refactoring-techniques.md) | [코드 리뷰 체크리스트](./clean-code/refactoring/code-review-checklist.md)

### AWS & LocalStack
- **AWS 기초**: [개요](./aws/fundamentals/aws-overview.md) | [리전/AZ](./aws/fundamentals/region-az.md) | [CLI 설정](./aws/fundamentals/aws-cli-setup.md)
- **IAM**: [소개](./aws/iam/iam-introduction.md) | [정책](./aws/iam/iam-policies.md) | [모범 사례](./aws/iam/iam-best-practices.md)
- **S3**: [소개](./aws/s3/s3-introduction.md) | [연산](./aws/s3/s3-operations.md) | [SDK 통합](./aws/s3/s3-sdk-integration.md) | [권한](./aws/s3/s3-permissions.md) | [모범 사례](./aws/s3/s3-best-practices.md)
- **EC2**: [소개](./aws/ec2/ec2-introduction.md) | [EC2 vs K8s](./aws/ec2/ec2-vs-kubernetes.md)
- **LocalStack**: [설정](./aws/localstack/localstack-setup.md) | [영속성 ⭐](./aws/localstack/localstack-persistence.md) | [서비스별](./aws/localstack/localstack-services.md) | [트러블슈팅](./aws/localstack/localstack-troubleshooting.md)
- **배포**: [환경 프로필](./aws/deployment/environment-profiles.md) | [로컬→K8s](./aws/deployment/local-to-kubernetes.md) | [K8s→AWS](./aws/deployment/kubernetes-to-aws.md) | [마이그레이션 체크리스트](./aws/best-practices/aws-migration-checklist.md)

---

## 문서 통계

| 카테고리 | 문서 수 |
|----------|---------|
| 인프라 (Kafka, Redis, MongoDB, ES) | 8개 |
| Shopping Service | 5개 |
| Blog Service | 1개 |
| API Gateway | 3개 |
| Frontend (MFE, React) | 3개 |
| 아키텍처 패턴 | 3개 |
| Clean Code & 리팩토링 | 9개 |
| AWS & LocalStack | 22개 |
| 학습 노트 | 8개 |
| **총계** | **62개** |

---

## 관련 문서

- [Scenarios 목록](../scenarios/README.md) - 업무 시나리오 문서
- [ADR 목록](../adr/README.md) - 아키텍처 결정 기록
- [Architecture](../architecture/) - 아키텍처 설계 문서
- [PRD](../prd/) - 제품 요구사항 문서
