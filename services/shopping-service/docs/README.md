# Shopping Service Documentation

> Shopping Service의 모든 문서를 한 곳에서 확인할 수 있습니다.

---

## 📋 개요

Shopping Service는 Portal Universe 프로젝트의 이커머스 마이크로서비스입니다. 상품, 장바구니, 주문, 결제, 배송 기능을 제공합니다.

| 항목 | 내용 |
|------|------|
| **포트** | 8083 |
| **데이터베이스** | MySQL |
| **메시지 브로커** | Kafka |
| **인증** | OAuth2 Resource Server (JWT) |

---

## 📚 문서 카테고리

### 📐 Architecture
시스템 구조, 컴포넌트 설계, 데이터 흐름

| 문서 | 설명 |
|------|------|
| [Architecture Overview](./architecture/README.md) | 아키텍처 문서 인덱스 |
| [System Overview](./architecture/system-overview.md) | 전체 시스템 구조 및 컴포넌트 |
| [Data Flow](./architecture/data-flow.md) | 주문, 결제, Saga 패턴 데이터 흐름 |

---

### 📡 API
REST API 명세서

| 문서 | 설명 |
|------|------|
| [API Overview](./api/README.md) | API 문서 인덱스 |
| [Product API](./api/product-api.md) | 상품 CRUD, 리뷰 조회 |
| [Cart API](./api/cart-api.md) | 장바구니 조회, 아이템 추가/수정/삭제, 체크아웃 |
| [Order API](./api/order-api.md) | 주문 생성, 조회, 취소 |
| [Payment API](./api/payment-api.md) | 결제 처리, 조회, 취소, 환불 |
| [Delivery API](./api/delivery-api.md) | 배송 조회, 상태 변경 |

---

### 📖 Guides
개발자 가이드 및 사용 가이드

| 문서 | 설명 |
|------|------|
| [Guides Overview](./guides/README.md) | 가이드 문서 인덱스 |
| [Getting Started](./guides/getting-started.md) | 로컬 개발 환경 설정 |

---

### 🔧 Runbooks
운영 절차서

| 문서 | 설명 |
|------|------|
| [Runbooks Overview](./runbooks/README.md) | 운영 절차서 인덱스 |
| [Deployment](./runbooks/deployment.md) | 배포 절차 (로컬/Docker/K8s) |
| [Rollback](./runbooks/rollback.md) | 롤백 절차 및 비상 대응 |

---

## 🔗 관련 서비스

- [API Gateway](../../api-gateway/docs/README.md)
- [Auth Service](../../auth-service/docs/README.md)
- [Blog Service](../../blog-service/docs/README.md)

---

## 🚀 빠른 시작

### 로컬 실행

```bash
# Gradle 빌드
./gradlew :services:shopping-service:build

# 실행
./gradlew :services:shopping-service:bootRun
```

### Docker Compose

```bash
# 전체 스택 실행
docker-compose up -d shopping-service

# 로그 확인
docker-compose logs -f shopping-service
```

---

## 📞 문의

이슈가 발생하거나 문의사항이 있으면 GitHub Issues를 통해 문의해 주세요.

---

**최종 업데이트**: 2026-01-18
