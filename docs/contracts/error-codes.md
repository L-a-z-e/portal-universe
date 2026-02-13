# Error Code Namespace Allocation

> 서비스별 에러 코드 prefix 할당표.
> 에러 코드 형식: `<PREFIX><DIGITS>` (예: `C001`, `S201`, `P006`, `CH01`)

## 할당표

| Prefix | 서비스 | 범위 | 설명 |
|--------|--------|------|------|
| `C` | Common (전체 공통) | C001-C099 | 서비스 독립적인 공통 에러 |
| `A` | auth-service | A001-A099 | 인증/인가 관련 에러 |
| `S` | shopping-service | S001-S9999 | 쇼핑 도메인 에러 (세부 namespace 있음) |
| `B` | blog-service | B001-B099 | 블로그 도메인 에러 |
| `N` | notification-service | N001-N099 | 알림 서비스 에러 |
| `D` | drive-service | D001-D099 | 드라이브 서비스 에러 |
| `P` | prism-service (NestJS) | P001-P099 | 프로젝트 관리 에러 |
| `CH` | chatbot-service (Python) | CH01-CH99 | AI/챗봇 서비스 에러 |

## Common Error Codes (전체 서비스 공유)

모든 서비스에서 공통으로 사용하는 에러 코드. Java는 `CommonErrorCode` enum으로 정의되어 있으며, NestJS/Python도 동일한 코드를 사용해야 한다.

| Code | HTTP Status | Message | 사용 시점 |
|------|-------------|---------|----------|
| C001 | 500 | Internal Server Error | 예상치 못한 서버 에러 |
| C002 | 400 | Invalid Input Value | Bean Validation / Pydantic / class-validator 실패 |
| C003 | 404 | Not Found | 리소스를 찾을 수 없음 |
| C004 | 403 | Forbidden | 권한 없음 |
| C005 | 401 | Unauthorized | 인증 필요 |
| C006 | 400 | XSS Detected | XSS 공격 탐지 |
| C007 | 400 | SQL Injection Detected | SQL Injection 탐지 |
| C008 | 400 | Invalid HTML Content | HTML sanitization 실패 |

## Shopping Service 세부 Namespace

Shopping은 도메인이 크므로 세부 namespace를 사용한다.

| 범위 | 도메인 | 예시 |
|------|--------|------|
| S001-S010 | Product | S001: Product Not Found |
| S101-S110 | Cart | S101: Cart Item Not Found |
| S201-S220 | Order | S201: Invalid Order State |
| S301-S315 | Payment | S301: Payment Failed |
| S401-S410 | Inventory | S401: Insufficient Stock |
| S501-S510 | Delivery | S501: Delivery Not Found |
| S601-S611 | Coupon | S601: Invalid Coupon |
| S701-S708 | TimeDeal | S701: TimeDeal Expired |
| S801-S807 | Queue | S801: Queue Full |
| S901-S905 | Saga/System | S901: Saga Compensation Failed |
| S1001-S1004 | Search | S1001: Search Index Error |

## Prism Service Error Codes

| Code | HTTP Status | Message |
|------|-------------|---------|
| P001 | 404 | Board not found |
| P002 | 404 | Task not found |
| P003 | 403 | Unauthorized access |
| P004 | 400 | Invalid state transition |
| P005 | 400 | Agent not assigned |
| P006 | 400 | Invalid request |
| P007 | 500 | Agent execution failed |
| P008 | 500 | Provider configuration error |
| P009 | 500 | AI execution failed |

## Chatbot Service Error Codes (신규)

| Code | HTTP Status | Message | 사용 시점 |
|------|-------------|---------|----------|
| CH01 | 500 | RAG Engine Error | RAG 엔진 초기화/쿼리 실패 |
| CH02 | 400 | Invalid Document | 지원하지 않는 파일 형식 |
| CH03 | 500 | AI Provider Error | LLM provider 호출 실패 |
| CH04 | 404 | Conversation Not Found | 대화 이력 조회 실패 |
| CH05 | 429 | Rate Limit Exceeded | AI 호출 제한 초과 |

## 새 서비스 추가 시

1. 이 문서에 새 prefix를 등록한다
2. 2글자 이내의 대문자 prefix를 선택한다 (기존과 충돌 불가)
3. Common codes (C0XX)는 재정의하지 않고 그대로 사용한다
4. `error-response.schema.json`의 code 패턴 `^[A-Z]{1,2}[0-9]{2,4}$`을 준수한다

## 관련 문서

- [Error Response Schema](error-response.schema.json)
- [API Response Schema](api-response.schema.json)

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-13 | 최초 작성 - 전체 서비스 에러 코드 namespace 정리, chatbot-service(CH) 신규 할당 |
