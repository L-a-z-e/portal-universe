---
id: notification-service-docs-index
title: Notification Service 문서 인덱스
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter-agent
tags: [notification-service, documentation, index, kafka, consumer]
related:
  - notification-service-architecture-system-overview
---

# Notification Service 문서

Portal Universe 플랫폼의 Notification Service 문서입니다.

## 개요

Notification Service는 Kafka 컨슈머 기반 알림 서비스로, 이벤트 기반 메시지 처리를 통해 이메일, SMS, 푸시 알림을 발송합니다.

### 핵심 기능

| 기능 | 설명 |
|------|------|
| **이벤트 수신** | Kafka 토픽 구독 및 이벤트 소비 |
| **이메일 발송** | Spring Mail(SMTP) 기반 이메일 전송 |
| **SMS 발송** | SMS 알림 전송 |
| **푸시 알림** | 푸시 알림 발송 |
| **알림 이력 관리** | 발송 이력 및 상태 추적 |
| **재시도 처리** | 실패 시 자동 재시도 및 DLQ 처리 |

### 기술 스택

- **Framework**: Spring Boot 3.5.5
- **Message Broker**: Apache Kafka 4.1.0 (KRaft)
- **Email**: Spring Mail (SMTP), Thymeleaf Template
- **Observability**: Micrometer, Prometheus
- **Consumer Group**: `notification-group`

### 포트

- 개발 환경: `8084`
- Docker: `notification-service:8084`

---

## 문서 목록

### Architecture (아키텍처)

| 문서 | 설명 |
|------|------|
| [시스템 아키텍처](./architecture/system-overview.md) | Kafka Consumer 구조, 이벤트 처리 흐름, 재시도 전략 |

### Guides (가이드)

| 문서 | 설명 |
|------|------|
| [로컬 개발 가이드](./guides/local-development.md) | 로컬 환경 설정, Kafka 설정, 테스트 방법 |

---

## 빠른 시작

### 빌드 및 실행

```bash
# 빌드
./gradlew :services:notification-service:build

# 실행
./gradlew :services:notification-service:bootRun

# 테스트
./gradlew :services:notification-service:test
```

### 헬스 체크

```bash
curl http://localhost:8084/actuator/health
```

### 구독 토픽

| 토픽 | 이벤트 | 처리 내용 | Producer |
|------|--------|----------|----------|
| `user-signup` | 회원가입 | 환영 이메일 발송 | auth-service |
| `order-created` | 주문 생성 | 주문 확인 이메일/SMS 발송 | shopping-service |
| `order-cancelled` | 주문 취소 | 주문 취소 알림 발송 | shopping-service |
| `delivery-status` | 배송 상태 변경 | 배송 진행 상황 알림 | shopping-service |

### 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | `localhost:9092` |
| `KAFKA_GROUP_ID` | 컨슈머 그룹 ID | `notification-group` |
| `MAIL_HOST` | SMTP 서버 호스트 | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP 서버 포트 | `587` |
| `MAIL_USERNAME` | SMTP 사용자명 | - |
| `MAIL_PASSWORD` | SMTP 비밀번호 | - |

---

## 이벤트 DTO

### UserSignedUpEvent

```java
public record UserSignedUpEvent(
    String userId,
    String email,
    String name,
    LocalDateTime signedUpAt
) {}
```

### OrderCreatedEvent

```java
public record OrderCreatedEvent(
    String orderId,
    String orderNumber,
    String userId,
    String userEmail,
    String userPhone,
    BigDecimal totalAmount,
    List<OrderItemEvent> items,
    LocalDateTime createdAt
) {}
```

---

## 관련 링크

- [Portal Universe CLAUDE.md](../../../CLAUDE.md) - 프로젝트 전체 개요
- [Auth Service](../../auth-service/docs/) - 인증 서비스 문서 (user-signup 이벤트 Producer)
- [Shopping Service](../../shopping-service/docs/) - 쇼핑 서비스 문서 (order 이벤트 Producer)
- [Kafka 설정](../../../docker-compose.yml) - 로컬 Kafka 환경 설정

---

## 백업 문서

이전 버전의 문서는 `backup/` 디렉토리에 보관되어 있습니다.

- [README.md](./backup/README.md)
- [ARCHITECTURE.md](./backup/ARCHITECTURE.md)
