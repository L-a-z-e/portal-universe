---
id: prism-service-security
title: Prism Service 보안 아키텍처
type: architecture
status: current
created: 2026-02-13
updated: 2026-02-13
author: Laze
tags: [prism-service, nestjs, security, validation, audit, xss, sql-injection]
related:
  - ADR-029-cross-cutting-security-layer
  - ADR-030-environment-security-profile
---

# Prism Service 보안 아키텍처

## 개요

Prism Service의 입력 검증, SQL Injection 방어, Audit 로깅 계층을 설명합니다. ADR-029의 Cross-cutting 보안 전략을 NestJS 환경에 적용한 구현입니다.

| 항목 | 내용 |
|------|------|
| **범위** | Component (Validators, Interceptors) |
| **주요 기술** | NestJS, class-validator, class-transformer |
| **배포 환경** | Local, Docker, Kubernetes |
| **관련 서비스** | api-gateway (CORS, Rate Limiting) |

---

## 보안 계층 구조

```
API Gateway (1차 방어)
    ↓ CORS, Rate Limiting, Security Headers
NestJS App (2차 방어)
    ↓
┌─────────────────────────────────────┐
│ Global Interceptor (AuditInterceptor)│ ← 모든 요청 로깅
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ DTO Validation (class-validator)    │ ← @NoXss, @NoSqlInjection
└─────────────────────────────────────┘
    ↓
Controller → Service → Repository
```

---

## 핵심 컴포넌트

### 1. NoXss Validator

**경로**: `src/common/validators/no-xss.validator.ts`

**역할**: 사용자 입력에서 XSS(Cross-Site Scripting) 공격 패턴을 감지합니다.

**사용법**:
```typescript
import { NoXss } from '@/common/validators/no-xss.validator';

export class CreateBoardDto {
  @NoXss({ message: 'Board name contains XSS pattern' })
  name: string;
}
```

**검증 패턴**:
| 패턴 | 설명 |
|------|------|
| `<script[^>]*>.*?</script>` | Script 태그 |
| `on\w+\s*=` | Event 핸들러 (`onclick`, `onerror` 등) |
| `javascript:` | JavaScript 프로토콜 |
| `<iframe[^>]*>` | Iframe 태그 |

**검증 규칙**:
- 대소문자 무시 (`i` flag)
- 하나라도 매칭되면 ValidationError 발생
- `@IsOptional()` 필드는 `undefined`/`null` 허용

### 2. NoSqlInjection Validator

**경로**: `src/common/validators/no-sql-injection.validator.ts`

**역할**: SQL Injection 공격 패턴을 감지합니다. (PostgreSQL 기반)

**사용법**:
```typescript
import { NoSqlInjection } from '@/common/validators/no-sql-injection.validator';

export class CreateAgentDto {
  @NoSqlInjection({ message: 'Description contains SQL injection pattern' })
  description: string;
}
```

**검증 패턴**:
| 패턴 | 설명 |
|------|------|
| `(\bUNION\b.*\bSELECT\b)` | UNION SELECT |
| `(\bDROP\b.*\bTABLE\b)` | DROP TABLE |
| `(\bINSERT\b.*\bINTO\b)` | INSERT INTO |
| `(\bDELETE\b.*\bFROM\b)` | DELETE FROM |
| `(--|#|\/\*|\*\/)` | SQL 주석 |
| `(\bEXEC\b|\bEXECUTE\b)` | 동적 SQL 실행 |

**검증 규칙**:
- 대소문자 무시 (`i` flag)
- TypeORM Parameterized Query 사용 시 추가 방어선
- `@IsOptional()` 필드는 `undefined`/`null` 허용

### 3. AuditInterceptor

**경로**: `src/common/interceptors/audit.interceptor.ts`

**역할**: 데이터 변경 요청(POST/PUT/PATCH/DELETE)을 로깅합니다.

**주요 기능**:
- 요청 정보 로깅: HTTP Method, URL Path, User ID
- 응답 정보 로깅: Status Code, Duration (ms)
- 성공/실패 모두 기록

**로그 형식**:
```typescript
{
  timestamp: '2026-02-13T10:00:00.000Z',
  userId: 'user-123',
  method: 'POST',
  path: '/api/v1/prism/boards',
  statusCode: 201,
  duration: 45
}
```

**등록 위치**: `main.ts`
```typescript
app.useGlobalInterceptors(new AuditInterceptor());
```

**대상 메서드**:
- POST: 생성
- PUT/PATCH: 수정
- DELETE: 삭제
- GET은 제외 (조회는 로깅하지 않음)

---

## DTO별 적용 현황

### Board DTOs
| DTO | @NoXss 필드 | @NoSqlInjection 필드 |
|-----|------------|---------------------|
| `CreateBoardDto` | `name`, `description` | `description` |
| `UpdateBoardDto` | `name`, `description` | `description` |

### Agent DTOs
| DTO | @NoXss 필드 | @NoSqlInjection 필드 |
|-----|------------|---------------------|
| `CreateAgentDto` | `name`, `description`, `systemPrompt` | `description`, `systemPrompt` |
| `UpdateAgentDto` | `name`, `description`, `systemPrompt` | `description`, `systemPrompt` |

### Task DTOs
| DTO | @NoXss 필드 | @NoSqlInjection 필드 |
|-----|------------|---------------------|
| `CreateTaskDto` | `subject`, `description`, `activeForm` | `description` |
| `UpdateTaskDto` | `subject`, `description`, `activeForm` | `description` |

### Provider DTOs
| DTO | @NoXss 필드 | @NoSqlInjection 필드 |
|-----|------------|---------------------|
| `CreateProviderDto` | `name`, `apiKey`, `baseUrl`, `model` | - |
| `UpdateProviderDto` | `name`, `apiKey`, `baseUrl`, `model` | - |

> Provider의 `apiKey`, `baseUrl`은 민감 정보이므로 XSS 검증만 적용. SQL Injection은 환경변수/암호화 저장으로 대응.

---

## 데이터 플로우

### 요청 처리 흐름

```
1. Client → API Gateway (JWT 검증, X-User-Id 추출)
2. Gateway → NestJS App (X-User-Id 헤더 전달)
3. AuditInterceptor (Before)
   - 요청 시작 시각 기록
   - Method가 POST/PUT/PATCH/DELETE인지 확인
4. DTO Validation Pipeline
   - class-transformer: JSON → DTO 변환
   - class-validator: @NoXss, @NoSqlInjection 검증
   - ValidationPipe: 검증 실패 시 400 BadRequest
5. Controller → Service → Repository
6. AuditInterceptor (After)
   - 응답 시간 계산
   - POST/PUT/PATCH/DELETE 요청만 로깅
   - Logger에 userId, method, path, statusCode, duration 기록
7. Response → Client
```

### Validation 실패 흐름

```
1. DTO에 XSS 패턴 포함된 요청
   → class-validator가 ValidationError 발생
   → ValidationPipe가 400 BadRequest 변환
   → HttpExceptionFilter가 표준 에러 응답 반환
   → AuditInterceptor는 statusCode: 400으로 로깅

2. SQL Injection 패턴 포함된 요청
   → 동일 흐름 (400 BadRequest)
```

---

## 기술적 결정

### class-validator 기반 Custom Decorator

**선택 이유**:
- NestJS 표준 Validation 파이프라인 활용
- DTO 레벨에서 선언적 검증 가능
- `@IsOptional()`과 자동 조합

**Trade-off**:
- ✅ 장점: 코드 가독성 향상, 재사용 용이
- ⚠️ 단점: Regex 패턴 복잡도 증가 시 유지보수 어려움

### Interceptor vs Middleware (Audit)

**Interceptor 선택 이유**:
- Exception 발생 후에도 `catch()` 블록에서 로깅 가능
- 응답 시간 측정 (Before/After)
- Dependency Injection 지원

**Middleware를 사용하지 않은 이유**:
- Exception 발생 시 After 로직 실행 안 됨
- Response 정보 접근 제한

### TypeORM Parameterized Query

**SQL Injection 이중 방어**:
1. `@NoSqlInjection()` 데코레이터 (사용자 입력 검증)
2. TypeORM Query Builder + Prepared Statement (실행 시점 방어)

```typescript
// 안전: 파라미터 바인딩
await this.boardRepository.findOne({ where: { name } });

// 위험: Raw Query (사용 금지)
await this.boardRepository.query(`SELECT * FROM boards WHERE name = '${name}'`);
```

---

## 설정

### ValidationPipe (main.ts)

```typescript
app.useGlobalPipes(
  new ValidationPipe({
    transform: true,
    whitelist: true,
    forbidNonWhitelisted: true,
  }),
);
```

| 옵션 | 설명 |
|------|------|
| `transform` | JSON → DTO 클래스 인스턴스 변환 |
| `whitelist` | DTO에 정의되지 않은 속성 제거 |
| `forbidNonWhitelisted` | 정의되지 않은 속성 포함 시 400 에러 |

### 로깅 설정

Audit 로그는 NestJS 기본 Logger를 사용하며, 프로덕션 환경에서는 구조화된 JSON 로그로 출력됩니다.

```typescript
private readonly logger = new Logger(AuditInterceptor.name);

this.logger.log({
  userId,
  method,
  path,
  statusCode,
  duration,
});
```

---

## 배포 및 확장

### 환경별 차이

| 환경 | Validation | Audit | 설정 |
|------|-----------|-------|------|
| Local | 활성 | 활성 (Console) | `.env.local` |
| Docker | 활성 | 활성 (JSON) | `.env.docker` |
| Kubernetes | 활성 | 활성 (JSON → Loki) | ConfigMap/Secret |

### 확장 고려사항

- **Audit 로그 저장소**: 현재는 Logger → Console/File. 향후 Elasticsearch/Loki로 중앙화 고려
- **Validation 패턴 업데이트**: `no-xss.validator.ts`, `no-sql-injection.validator.ts` 파일만 수정하면 전체 DTO에 즉시 반영
- **성능**: Regex 검증은 CPU Bound. 평균 응답 시간 영향 < 1ms (부하 테스트 검증 필요)

---

## 관련 문서

- [ADR-029: Cross-cutting 보안 처리 계층](../../adr/ADR-029-cross-cutting-security-layer.md)
- [ADR-030: 환경별 보안 프로파일](../../adr/ADR-030-environment-security-profile.md)
- [Chatbot Service 보안 아키텍처](../chatbot-service/security.md) (Python 구현)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-13 | 초안 작성 (NoXss, NoSqlInjection, AuditInterceptor) | Laze |
