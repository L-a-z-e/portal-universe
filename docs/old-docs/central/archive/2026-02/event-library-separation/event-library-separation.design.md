# Feature Design: event-library-separation

> **Feature**: 이벤트 라이브러리 분리
> **Version**: 1.0
> **Author**: Claude
> **Created**: 2026-02-03
> **Plan Reference**: `docs/pdca/01-plan/features/event-library-separation.plan.md`
> **Status**: Draft

---

## 1. 개요

### 1.1 설계 목표

common-library에 있는 도메인 이벤트들을 도메인별 독립 라이브러리로 분리하여:
- 불필요한 의존성 결합 제거
- 빌드 시간 최적화
- 단일 책임 원칙(SRP) 준수

### 1.2 현재 vs 목표 구조

```
현재 (Before)                           목표 (After)
─────────────────────────────────────────────────────────────────
common-library/                         common-library/
└── event/                              ├── exception/  (유지)
    ├── UserSignedUpEvent.java          ├── response/   (유지)
    ├── shopping/  (9개)                └── event/
    ├── blog/      (4개)                    └── UserSignedUpEvent.java (유지)
    └── prism/     (2개)
                                        shopping-events/  (신규)
                                        └── event/shopping/ (9개)

                                        blog-events/      (신규)
                                        └── event/blog/ (4개)

                                        prism-events/     (신규)
                                        └── event/prism/ (2개)
```

---

## 2. 모듈 설계

### 2.1 shopping-events 모듈

#### 2.1.1 디렉토리 구조

```
services/shopping-events/
├── build.gradle
└── src/main/java/com/portal/universe/event/shopping/
    ├── OrderCreatedEvent.java
    ├── OrderConfirmedEvent.java
    ├── OrderCancelledEvent.java
    ├── PaymentCompletedEvent.java
    ├── PaymentFailedEvent.java
    ├── DeliveryShippedEvent.java
    ├── InventoryReservedEvent.java
    ├── CouponIssuedEvent.java
    └── TimeDealStartedEvent.java
```

#### 2.1.2 build.gradle

```groovy
plugins {
    id 'java-library'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
description = 'Shopping Domain Events'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 이벤트는 순수 Java record - 외부 의존성 최소화
}
```

### 2.2 blog-events 모듈

#### 2.2.1 디렉토리 구조

```
services/blog-events/
├── build.gradle
└── src/main/java/com/portal/universe/event/blog/
    ├── PostLikedEvent.java
    ├── CommentCreatedEvent.java
    ├── CommentRepliedEvent.java
    └── UserFollowedEvent.java
```

#### 2.2.2 build.gradle

```groovy
plugins {
    id 'java-library'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
description = 'Blog Domain Events'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 이벤트는 순수 Java record - 외부 의존성 최소화
}
```

### 2.3 prism-events 모듈

#### 2.3.1 디렉토리 구조

```
services/prism-events/
├── build.gradle
└── src/main/java/com/portal/universe/event/prism/
    ├── PrismTaskCompletedEvent.java
    └── PrismTaskFailedEvent.java
```

#### 2.3.2 build.gradle

```groovy
plugins {
    id 'java-library'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
description = 'Prism Domain Events'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 이벤트는 순수 Java record - 외부 의존성 최소화
}
```

---

## 3. 패키지 경로 변경

### 3.1 패키지 매핑

| 현재 패키지 | 새 패키지 |
|------------|----------|
| `com.portal.universe.common.event.shopping.*` | `com.portal.universe.event.shopping.*` |
| `com.portal.universe.common.event.blog.*` | `com.portal.universe.event.blog.*` |
| `com.portal.universe.common.event.prism.*` | `com.portal.universe.event.prism.*` |
| `com.portal.universe.common.event.UserSignedUpEvent` | (변경 없음 - common-library에 유지) |

### 3.2 Import 변경 예시

```java
// Before
import com.portal.universe.common.event.shopping.OrderCreatedEvent;
import com.portal.universe.common.event.blog.PostLikedEvent;
import com.portal.universe.common.event.prism.PrismTaskCompletedEvent;

// After
import com.portal.universe.event.shopping.OrderCreatedEvent;
import com.portal.universe.event.blog.PostLikedEvent;
import com.portal.universe.event.prism.PrismTaskCompletedEvent;
```

---

## 4. 서비스 의존성 수정

### 4.1 settings.gradle 수정

```groovy
// 기존
include 'services:common-library'
include 'services:shopping-service'
include 'services:blog-service'
include 'services:notification-service'

// 추가
include 'services:shopping-events'
include 'services:blog-events'
include 'services:prism-events'
```

### 4.2 shopping-service/build.gradle

```groovy
dependencies {
    // Before
    implementation project(':services:common-library')

    // After
    implementation project(':services:common-library')
    implementation project(':services:shopping-events')  // 추가
}
```

### 4.3 blog-service/build.gradle

```groovy
dependencies {
    // Before
    implementation project(':services:common-library')

    // After
    implementation project(':services:common-library')
    implementation project(':services:blog-events')  // 추가
}
```

### 4.4 notification-service/build.gradle

```groovy
dependencies {
    // Before
    implementation project(':services:common-library')

    // After
    implementation project(':services:common-library')
    implementation project(':services:shopping-events')  // 추가
    implementation project(':services:blog-events')      // 추가
    implementation project(':services:prism-events')     // 추가
}
```

### 4.5 의존성 다이어그램

```
                    ┌─────────────────────┐
                    │   common-library    │
                    │  (예외, 응답, 유틸)  │
                    └─────────────────────┘
                              ▲
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────┴─────────┐ ┌───────┴───────┐ ┌────────┴────────┐
│ shopping-events   │ │ blog-events   │ │ prism-events    │
│ (9 events)        │ │ (4 events)    │ │ (2 events)      │
└─────────┬─────────┘ └───────┬───────┘ └────────┬────────┘
          │                   │                   │
          ▼                   ▼                   │
┌─────────────────────────────────────────────────┼──────┐
│              notification-service               │      │
│         (모든 이벤트 라이브러리 의존)              │      │
└─────────────────────────────────────────────────┼──────┘
                                                  │
          ┌───────────────────┼───────────────────┘
          │                   │
          ▼                   ▼
┌─────────────────┐   ┌───────────────┐
│ shopping-service│   │ blog-service  │
│ (shopping-events│   │ (blog-events  │
│  만 의존)        │   │  만 의존)     │
└─────────────────┘   └───────────────┘
```

---

## 5. 이벤트 파일 상세

### 5.1 Shopping Events (9개)

| 파일명 | 용도 | 필드 |
|--------|------|------|
| `OrderCreatedEvent.java` | 주문 생성 | orderNumber, userId, totalAmount, itemCount, items, createdAt |
| `OrderConfirmedEvent.java` | 주문 확정 | orderNumber, userId, confirmedAt |
| `OrderCancelledEvent.java` | 주문 취소 | orderNumber, userId, totalAmount, cancelReason, cancelledAt |
| `PaymentCompletedEvent.java` | 결제 완료 | paymentNumber, orderNumber, userId, amount, paymentMethod, pgTransactionId, completedAt |
| `PaymentFailedEvent.java` | 결제 실패 | paymentNumber, orderNumber, userId, amount, failureReason, failedAt |
| `DeliveryShippedEvent.java` | 배송 시작 | deliveryNumber, orderNumber, userId, trackingNumber, carrier, shippedAt |
| `InventoryReservedEvent.java` | 재고 예약 | inventoryId, productId, quantity, orderId, reservedAt |
| `CouponIssuedEvent.java` | 쿠폰 발급 | userId, couponCode, couponName, discountValue, discountType, issuedAt |
| `TimeDealStartedEvent.java` | 타임딜 시작 | timeDealId, productId, productName, originalPrice, dealPrice, stockQuantity, startAt, endAt |

### 5.2 Blog Events (4개)

| 파일명 | 용도 | 필드 |
|--------|------|------|
| `PostLikedEvent.java` | 좋아요 | likeId, postId, postTitle, authorId, likerId, likerName, likedAt |
| `CommentCreatedEvent.java` | 댓글 생성 | commentId, postId, postTitle, authorId, commenterId, commenterName, content, createdAt |
| `CommentRepliedEvent.java` | 답글 | replyId, postId, parentCommentId, parentCommentAuthorId, replierId, replierName, content, createdAt |
| `UserFollowedEvent.java` | 팔로우 | followId, followeeId, followerId, followerName, followedAt |

### 5.3 Prism Events (2개)

| 파일명 | 용도 | 필드 |
|--------|------|------|
| `PrismTaskCompletedEvent.java` | 태스크 완료 | taskId, boardId, userId, title, status, agentName, executionId, timestamp |
| `PrismTaskFailedEvent.java` | 태스크 실패 | taskId, boardId, userId, title, status, agentName, executionId, errorMessage, timestamp |

---

## 6. Import 수정 대상 파일

### 6.1 shopping-service (shopping-events 사용)

| 파일 | 변경 내용 |
|------|----------|
| `ShoppingEventPublisher.java` | import 경로 변경 |
| `OrderServiceImpl.java` | import 경로 변경 |
| `PaymentServiceImpl.java` | import 경로 변경 |
| `DeliveryServiceImpl.java` | import 경로 변경 |
| `CouponServiceImpl.java` | import 경로 변경 |
| `KafkaConfig.java` | (변경 없음) |

### 6.2 blog-service (blog-events 사용)

| 파일 | 변경 내용 |
|------|----------|
| `BlogEventPublisher.java` | import 경로 변경 |
| `LikeService.java` | import 경로 변경 |
| `CommentService.java` | import 경로 변경 |

### 6.3 notification-service (전체 이벤트 사용)

| 파일 | 변경 내용 |
|------|----------|
| `NotificationEventConverter.java` | shopping/blog/prism import 경로 변경 |
| `NotificationConsumer.java` | shopping/blog/prism import 경로 변경 |

---

## 7. Kafka 직렬화 호환성

### 7.1 문제점

Kafka의 기본 JSON 직렬화는 클래스 FQCN(Fully Qualified Class Name)을 사용합니다. 패키지 경로가 변경되면 기존 메시지 역직렬화 실패 가능성이 있습니다.

### 7.2 해결 방안

**방안 1: JsonTypeInfo 없이 단순 JSON 사용** (현재 프로젝트 방식)
- 현재 프로젝트는 `JsonSerializer`를 사용하지만 타입 정보를 메시지에 포함하지 않음
- Consumer에서 명시적으로 타입을 지정하므로 패키지 변경에 영향 없음
- ✅ **추가 작업 불필요**

**방안 2: (필요시) TYPE_ID 헤더 사용**
```java
// Producer 설정
configProps.put(JsonSerializer.TYPE_MAPPINGS,
    "orderCreated:com.portal.universe.event.shopping.OrderCreatedEvent");
```

### 7.3 검증 방법

```bash
# 1. Producer에서 이벤트 발행
# 2. kafka-console-consumer로 메시지 확인
kafka-console-consumer --topic shopping.order.created --from-beginning

# 3. Consumer에서 정상 수신 확인 (로그)
```

---

## 8. 구현 순서

### Phase 1: 이벤트 라이브러리 생성

```
□ 1.1 services/shopping-events/ 디렉토리 생성
□ 1.2 services/blog-events/ 디렉토리 생성
□ 1.3 services/prism-events/ 디렉토리 생성
□ 1.4 각 모듈의 build.gradle 생성
□ 1.5 settings.gradle에 모듈 등록
□ 1.6 ./gradlew projects 로 모듈 인식 확인
```

### Phase 2: 이벤트 파일 이동

```
□ 2.1 common-library/event/shopping/* → shopping-events/로 복사
□ 2.2 common-library/event/blog/* → blog-events/로 복사
□ 2.3 common-library/event/prism/* → prism-events/로 복사
□ 2.4 각 파일의 package 선언 수정
□ 2.5 각 이벤트 라이브러리 빌드 확인
```

### Phase 3: 서비스 의존성 및 Import 수정

```
□ 3.1 shopping-service build.gradle 수정 + import 변경
□ 3.2 blog-service build.gradle 수정 + import 변경
□ 3.3 notification-service build.gradle 수정 + import 변경
□ 3.4 각 서비스 개별 빌드 확인
```

### Phase 4: 정리 및 최종 검증

```
□ 4.1 common-library에서 event/shopping, event/blog, event/prism 삭제
□ 4.2 전체 빌드 검증 (./gradlew build)
□ 4.3 Kafka 이벤트 발행/수신 통합 테스트
```

---

## 9. 변경 파일 목록

### 9.1 신규 생성

| 파일 | 설명 |
|------|------|
| `services/shopping-events/build.gradle` | Shopping 이벤트 모듈 설정 |
| `services/shopping-events/src/main/java/.../event/shopping/*.java` | 9개 이벤트 |
| `services/blog-events/build.gradle` | Blog 이벤트 모듈 설정 |
| `services/blog-events/src/main/java/.../event/blog/*.java` | 4개 이벤트 |
| `services/prism-events/build.gradle` | Prism 이벤트 모듈 설정 |
| `services/prism-events/src/main/java/.../event/prism/*.java` | 2개 이벤트 |

### 9.2 수정

| 파일 | 변경 내용 |
|------|----------|
| `settings.gradle` | 3개 모듈 추가 |
| `services/shopping-service/build.gradle` | shopping-events 의존성 추가 |
| `services/blog-service/build.gradle` | blog-events 의존성 추가 |
| `services/notification-service/build.gradle` | 3개 이벤트 라이브러리 의존성 추가 |
| `ShoppingEventPublisher.java` | import 경로 변경 |
| `OrderServiceImpl.java` | import 경로 변경 |
| `PaymentServiceImpl.java` | import 경로 변경 |
| `DeliveryServiceImpl.java` | import 경로 변경 |
| `CouponServiceImpl.java` | import 경로 변경 |
| `BlogEventPublisher.java` | import 경로 변경 |
| `LikeService.java` | import 경로 변경 |
| `CommentService.java` | import 경로 변경 |
| `NotificationEventConverter.java` | import 경로 변경 |
| `NotificationConsumer.java` | import 경로 변경 |

### 9.3 삭제

| 파일/디렉토리 | 설명 |
|--------------|------|
| `common-library/src/.../event/shopping/` | 디렉토리 전체 |
| `common-library/src/.../event/blog/` | 디렉토리 전체 |
| `common-library/src/.../event/prism/` | 디렉토리 전체 |

---

## 10. 테스트 계획

### 10.1 빌드 테스트

```bash
# 1. 개별 모듈 빌드
./gradlew :services:shopping-events:build
./gradlew :services:blog-events:build
./gradlew :services:prism-events:build

# 2. 서비스 빌드
./gradlew :services:shopping-service:compileJava
./gradlew :services:blog-service:compileJava
./gradlew :services:notification-service:compileJava

# 3. 전체 빌드
./gradlew build
```

### 10.2 통합 테스트

```bash
# Docker 환경 실행 후
# 1. 주문 생성 → notification 수신 확인
# 2. 좋아요 → notification 수신 확인
```

---

## 11. 롤백 계획

문제 발생 시:
1. Git에서 이전 커밋으로 복구
2. settings.gradle에서 새 모듈 제거
3. 서비스 build.gradle 복원

---

## Changelog

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-03 | 최초 작성 |
