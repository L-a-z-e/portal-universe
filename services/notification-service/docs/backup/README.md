# Notification Service

Kafka 컨슈머 기반 알림 서비스입니다.

## 개요

Kafka 토픽을 구독하여 이메일, SMS, 푸시 알림 등을 처리합니다.

## 포트

- 서비스: `8084`

## 주요 기능

| 기능 | 설명 |
|------|------|
| 이벤트 수신 | Kafka 토픽 구독 |
| 알림 발송 | 이메일, SMS, 푸시 |
| 알림 이력 | 발송 이력 관리 |

## 구독 토픽

| 토픽 | 이벤트 | 처리 |
|------|--------|------|
| `user-signup` | 회원가입 | 환영 이메일 발송 |
| `order-created` | 주문 생성 | 주문 확인 알림 |
| `order-cancelled` | 주문 취소 | 취소 알림 |
| `delivery-status` | 배송 상태 변경 | 배송 알림 |

## 기술 스택

- **Message Broker**: Kafka
- **Email**: Spring Mail (SMTP)
- **Template**: Thymeleaf

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 서버 | localhost:9092 |
| `KAFKA_GROUP_ID` | 컨슈머 그룹 | notification-group |
| `MAIL_HOST` | SMTP 서버 | smtp.gmail.com |
| `MAIL_USERNAME` | SMTP 사용자 | - |
| `MAIL_PASSWORD` | SMTP 비밀번호 | - |

## 실행

```bash
./gradlew :services:notification-service:bootRun
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
