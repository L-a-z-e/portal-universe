# Notification Service API Documentation

Notification Service의 API 명세 문서입니다.

## API 문서 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [notification-api.md](./notification-api.md) | 알림 CRUD REST API (6개 엔드포인트) | current |
| [notification-events.md](./notification-events.md) | Kafka 이벤트, WebSocket, Redis Pub/Sub 실시간 알림 | current |

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8084` |
| **API Prefix** | `/api/v1/notifications` |
| **인증** | API Gateway -> X-User-Id 헤더 |
| **실시간** | WebSocket (STOMP over SockJS) |
| **이벤트** | Kafka Consumer (14개 토픽) |
| **Multi-instance** | Redis Pub/Sub |

## REST API Endpoints

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/notifications` | 알림 목록 조회 |
| `GET` | `/api/v1/notifications/unread` | 읽지 않은 알림 조회 |
| `GET` | `/api/v1/notifications/unread/count` | 읽지 않은 알림 수 |
| `PUT` | `/api/v1/notifications/{id}/read` | 읽음 처리 |
| `PUT` | `/api/v1/notifications/read-all` | 전체 읽음 처리 |
| `DELETE` | `/api/v1/notifications/{id}` | 알림 삭제 |

## Kafka Topics

| 도메인 | Topic 수 | 주요 이벤트 |
|--------|----------|-------------|
| Auth | 1 | 회원가입 환영 알림 |
| Shopping | 7 | 주문, 결제, 배송, 쿠폰, 타임딜 |
| Blog | 4 | 좋아요, 댓글, 답글, 팔로우 |
| Prism | 2 | AI 태스크 완료/실패 |

## 관련 문서

- [Notification Service Architecture](../../architecture/notification-service/system-overview.md)

---

**최종 업데이트**: 2026-02-06
