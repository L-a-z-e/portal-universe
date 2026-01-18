# Shopping Service

MySQL 기반 이커머스 서비스입니다.

## 개요

상품, 장바구니, 주문, 결제, 배송, 재고 관리 기능을 제공합니다.

## 포트

- 서비스: `8083`
- Swagger: `http://localhost:8083/swagger-ui.html`

## 주요 도메인

| 도메인 | 기능 |
|--------|------|
| Product | 상품 관리 (CRUD, 검색) |
| Cart | 장바구니 (아이템 추가/수정/삭제) |
| Order | 주문 (생성, 취소) |
| Payment | 결제 (PG 연동) |
| Delivery | 배송 추적 |
| Inventory | 재고 관리 |

## 기술 스택

- **Database**: MySQL
- **Message Broker**: Kafka
- **Security**: OAuth2 Resource Server (JWT)
- **Service Communication**: Feign Client

## API 엔드포인트

### Product API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/products` | 상품 목록 |
| GET | `/products/{id}` | 상품 상세 |
| POST | `/admin/products` | 상품 등록 (Admin) |
| PUT | `/admin/products/{id}` | 상품 수정 (Admin) |
| DELETE | `/admin/products/{id}` | 상품 삭제 (Admin) |

### Cart API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/shopping/cart` | 장바구니 조회 |
| POST | `/api/shopping/cart/items` | 아이템 추가 |
| PUT | `/api/shopping/cart/items/{id}` | 수량 변경 |
| DELETE | `/api/shopping/cart/items/{id}` | 아이템 삭제 |
| POST | `/api/shopping/cart/checkout` | 체크아웃 |

### Order API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/shopping/orders` | 주문 생성 |
| GET | `/api/shopping/orders` | 주문 목록 |
| GET | `/api/shopping/orders/{orderNumber}` | 주문 상세 |
| POST | `/api/shopping/orders/{orderNumber}/cancel` | 주문 취소 |

### Payment API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/shopping/payments` | 결제 처리 |
| GET | `/api/shopping/payments/{id}` | 결제 상세 |

### Delivery API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/shopping/deliveries/{orderNumber}` | 배송 조회 |
| PUT | `/api/shopping/deliveries/{id}/status` | 상태 변경 (Admin) |

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `MYSQL_HOST` | MySQL 호스트 | localhost |
| `MYSQL_PORT` | MySQL 포트 | 3306 |
| `MYSQL_DATABASE` | 데이터베이스 | shopping_db |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 서버 | localhost:9092 |

## 실행

```bash
./gradlew :services:shopping-service:bootRun
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
- [API.md](./API.md) - API 명세
