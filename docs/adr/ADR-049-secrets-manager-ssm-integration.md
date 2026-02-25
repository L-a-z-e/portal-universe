# ADR-049: AWS Secrets Manager + SSM Parameter Store 설정 외부화

**Status**: Accepted
**Date**: 2026-02-25
**Author**: Laze

## Context

모든 마이크로서비스가 민감정보(DB 비밀번호, JWT 시크릿, OAuth 클라이언트 시크릿)를
`.env.local` 파일과 환경변수로 직접 관리하고 있었다.

### 기존 문제점

| 문제 | 상세 |
|------|------|
| 비밀번호 로테이션 불가 | 환경변수 변경 시 서비스 재시작 필수 |
| 중앙 관리 부재 | 서비스별 `.env.local`에 분산, 일관성 보장 어려움 |
| 감사 추적 불가 | 누가 언제 비밀번호를 변경했는지 기록 없음 |
| 프로덕션 전환 격차 | 로컬과 프로덕션의 설정 관리 방식 완전히 다름 |

### 검토한 대안

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A. SM/SSM 우선, 환경변수 폴백** | SM에서 로드 실패 시 기존 환경변수 사용 | 점진적 전환, 하위호환 | 이중 관리 구간 존재 |
| B. SM/SSM 완전 전환 | 모든 설정을 SM/SSM으로 이동 | 단일 소스 | LocalStack 필수, 기존 워크플로우 깨짐 |
| C. Vault (HashiCorp) | 자체 비밀 관리 솔루션 | 클라우드 독립 | 별도 인프라 운영 부담 |

## Decision

**옵션 A 채택**: Spring Cloud AWS를 사용하여 모든 Spring Boot 서비스에 SM/SSM을 통합하되,
`optional:` prefix + 환경변수 폴백으로 하위호환을 유지한다.

### 핵심 설계 원칙

1. **`.env.local`은 유지** — 로컬 개발 시 LocalStack 없이도 기존 방식으로 동작
2. **`optional:` prefix 필수** — SM/SSM 접근 실패 시 서비스 기동 차단하지 않음
3. **init-secrets.sh는 `.env.local` 참조** — 하드코딩된 비밀번호 절대 금지 (git-tracked 파일)
4. **서비스별 독립 경로** — `/portal-universe/local/{service-name}/db-credentials`

### 값 해석 우선순위

```
1. Secrets Manager (SM)     ← spring.config.import로 prefix 매핑
2. SSM Parameter Store      ← spring.config.import로 경로 매핑
3. 환경변수 (.env.local)     ← ${DB_PASSWORD} 폴백
4. application.yml 기본값    ← 개발용 기본값 (프로덕션에서는 crash)
```

### 적용 범위

| 서비스 | DB | SM Secret 경로 | 비고 |
|--------|-----|---------------|------|
| auth-service | PostgreSQL | `/auth-service/db-credentials`, `/auth-service/jwt-secret`, `/auth-service/oauth-credentials` | JWT + OAuth |
| api-gateway | N/A | auth-service의 jwt-secret 공유 | JWT 검증용 |
| blog-service | MongoDB | `/blog-service/db-credentials` | |
| shopping-service | PostgreSQL | `/shopping-service/db-credentials` | |
| shopping-seller-service | PostgreSQL | `/shopping-seller-service/db-credentials` | |
| shopping-settlement-service | PostgreSQL | `/shopping-settlement-service/db-credentials` | |
| notification-service | MySQL | `/notification-service/db-credentials` | |
| drive-service | PostgreSQL | `/drive-service/db-credentials` | |

### 폴백 패턴 (application-local.yml)

```yaml
spring:
  config:
    import:
      - optional:aws-secretsmanager:/portal-universe/local/{service}/db-credentials?prefix=db.
      - optional:aws-parameterstore:/portal-universe/local/{service}/
  cloud:
    aws:
      endpoint: http://localhost:4566  # LocalStack
  datasource:
    username: ${db.username:${DB_USER:laze}}    # SM → 환경변수 → 기본값
    password: ${db.password:${DB_PASSWORD}}      # SM → 환경변수
```

### 로컬 개발 워크플로우

```
1. docker compose up -d (LocalStack 포함)
2. ./localstack-init/init-secrets.sh (호스트에서 실행, .env.local 참조)
3. ./gradlew :services:{name}:bootRun --args='--spring.profiles.active=local'
```

LocalStack 미실행 시에도 `optional:` prefix로 폴백하여 기존 환경변수 방식 동작.

## Consequences

### 긍정적

- **프로덕션 전환 용이**: LocalStack → 실제 AWS SM/SSM으로 엔드포인트만 변경
- **자동 로테이션 대비**: AWS Lambda + SM rotation 연동 가능 (M5에서 구현 예정)
- **감사 추적**: SM API 호출 → CloudTrail 자동 기록
- **하위호환**: `optional:` prefix로 기존 워크플로우 깨지지 않음

### 부정적

- **의존성 증가**: `spring-cloud-aws` 3개 라이브러리 추가 (8개 서비스)
- **이중 관리 구간**: SM과 `.env.local` 양쪽에 값 존재 → 어떤 값이 적용됐는지 혼란 가능
- **NestJS/FastAPI 미적용**: prism-service, chatbot-service는 별도 SDK 연동 필요

### 미결 사항

- [ ] docker/k8s 프로필에도 SM/SSM 설정 추가 (현재 local만 적용)
- [ ] NestJS (prism-service) AWS SDK 연동
- [ ] FastAPI (chatbot-service) boto3 연동
- [ ] SM 키 로테이션 Lambda 구현 (M5 예정)

## References

- [Spring Cloud AWS - Secrets Manager](https://docs.awspring.io/spring-cloud-aws/docs/3.0/reference/html/index.html#secrets-manager)
- [Spring Cloud AWS - Parameter Store](https://docs.awspring.io/spring-cloud-aws/docs/3.0/reference/html/index.html#parameter-store)
- 커밋: `727d3970` (feat(infra): integrate AWS Secrets Manager and SSM Parameter Store)