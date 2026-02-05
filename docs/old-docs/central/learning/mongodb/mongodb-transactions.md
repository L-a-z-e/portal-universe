# MongoDB Transactions

## 학습 목표
- Multi-Document Transactions 이해
- Write Concern과 Read Concern 설정
- ACID 보장과 성능 트레이드오프
- Portal Universe에서의 트랜잭션 활용

---

## 1. MongoDB 트랜잭션 개요

### 1.1 단일 문서 vs 다중 문서

| 유형 | 트랜잭션 | 설명 |
|------|---------|------|
| **Single Document** | 항상 원자적 | 기본 제공 |
| **Multi-Document** | 명시적 트랜잭션 필요 | MongoDB 4.0+ |

```javascript
// 단일 문서 연산 - 항상 원자적
db.posts.updateOne(
  { _id: "post123" },
  {
    $set: { title: "새 제목" },
    $inc: { viewCount: 1 },
    $push: { tags: "new-tag" }
  }
)
// 위 모든 변경이 원자적으로 적용됨
```

### 1.2 Multi-Document Transaction이 필요한 경우

```
1. 여러 컬렉션에 걸친 일관성 필요
2. 관련 문서들의 동시 업데이트
3. 금융, 재고 등 정합성이 중요한 도메인
```

---

## 2. Multi-Document Transactions

### 2.1 기본 사용법 (MongoDB Shell)

```javascript
// 세션 시작
const session = db.getMongo().startSession();

try {
  // 트랜잭션 시작
  session.startTransaction({
    readConcern: { level: "snapshot" },
    writeConcern: { w: "majority" }
  });

  const postsCollection = session.getDatabase("blog_db").posts;
  const likesCollection = session.getDatabase("blog_db").likes;

  // 좋아요 추가
  likesCollection.insertOne({
    postId: "post123",
    userId: "user456",
    createdAt: new Date()
  }, { session });

  // 게시물 좋아요 수 증가
  postsCollection.updateOne(
    { _id: "post123" },
    { $inc: { likeCount: 1 } },
    { session }
  );

  // 커밋
  session.commitTransaction();
  print("Transaction committed!");

} catch (error) {
  // 롤백
  session.abortTransaction();
  print("Transaction aborted: " + error.message);

} finally {
  session.endSession();
}
```

### 2.2 Spring Data MongoDB Transaction

```java
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * @Transactional 어노테이션으로 트랜잭션 관리
     * MongoDB 4.0+ 및 Replica Set 필요
     */
    @Transactional
    public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
        // 1. Post 존재 확인
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 2. 기존 좋아요 확인
        Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        boolean liked;
        if (existingLike.isPresent()) {
            // 좋아요 취소
            likeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            liked = false;
        } else {
            // 좋아요 추가
            Like newLike = Like.builder()
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .build();
            likeRepository.save(newLike);
            post.incrementLikeCount();
            liked = true;
        }

        // 3. Post 저장
        postRepository.save(post);

        // 모든 연산이 성공하면 커밋, 실패하면 롤백
        return LikeToggleResponse.of(liked, post.getLikeCount());
    }
}
```

### 2.3 TransactionTemplate 사용

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MongoTransactionManager transactionManager;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public Order createOrder(OrderRequest request) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {
            try {
                // 재고 확인 및 차감
                Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

                if (inventory.getQuantity() < request.getQuantity()) {
                    throw new RuntimeException("Insufficient inventory");
                }

                inventory.decreaseQuantity(request.getQuantity());
                inventoryRepository.save(inventory);

                // 주문 생성
                Order order = Order.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .userId(request.getUserId())
                    .status(OrderStatus.CREATED)
                    .build();

                return orderRepository.save(order);

            } catch (Exception e) {
                status.setRollbackOnly();  // 명시적 롤백
                throw e;
            }
        });
    }
}
```

### 2.4 MongoDB Transaction Configuration

```java
@Configuration
public class MongoTransactionConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/blog_db?replicaSet=rs0
      # Replica Set 필수!
```

---

## 3. Write Concern

### 3.1 개념

Write Concern은 **쓰기 연산의 확인 수준**을 정의합니다.

```
Client → Primary → Secondary 1
                → Secondary 2
```

### 3.2 옵션

| Write Concern | 설명 | 내구성 | 성능 |
|---------------|------|--------|------|
| `w: 0` | 확인 안 함 (Fire and Forget) | 낮음 | 높음 |
| `w: 1` | Primary 확인 (기본값) | 중간 | 중간 |
| `w: "majority"` | 과반수 노드 확인 | 높음 | 낮음 |
| `w: <number>` | 특정 수의 노드 확인 | 설정 따름 | 설정 따름 |

### 3.3 사용법

```javascript
// MongoDB Shell
db.posts.insertOne(
  { title: "중요한 게시물" },
  { writeConcern: { w: "majority", wtimeout: 5000 } }
)

// wtimeout: 최대 대기 시간 (ms)
```

```java
// Spring Data MongoDB
@Document(collection = "posts")
public class Post {
    // ...
}

// Repository 레벨 설정
public interface PostRepository extends MongoRepository<Post, String> {

    @Meta(comment = "Important write")
    @Override
    <S extends Post> S save(S entity);
}

// 또는 MongoTemplate 사용
mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);
mongoTemplate.save(post);
```

### 3.4 Journal (j 옵션)

```javascript
// journal에 기록까지 완료 확인
db.posts.insertOne(
  { title: "매우 중요한 데이터" },
  { writeConcern: { w: 1, j: true } }
)
```

| j 옵션 | 설명 |
|--------|------|
| `j: false` | 메모리에만 기록 (빠름, 위험) |
| `j: true` | 디스크 journal에 기록 확인 |

---

## 4. Read Concern

### 4.1 개념

Read Concern은 **읽기 연산의 일관성 수준**을 정의합니다.

### 4.2 옵션

| Read Concern | 설명 | 사용 사례 |
|--------------|------|----------|
| `local` | 로컬 데이터 반환 (기본값) | 일반 조회 |
| `available` | 가장 빠른 응답 | 샤딩 환경 |
| `majority` | 과반수 복제 확인된 데이터 | 일관성 중요 |
| `linearizable` | 가장 최신 데이터 보장 | 강한 일관성 필요 |
| `snapshot` | 트랜잭션 시작 시점 스냅샷 | 트랜잭션 내 |

### 4.3 사용법

```javascript
// MongoDB Shell
db.posts.find({ status: "PUBLISHED" })
  .readConcern("majority")

// 트랜잭션 내
session.startTransaction({
  readConcern: { level: "snapshot" },
  writeConcern: { w: "majority" }
});
```

```java
// Spring Data MongoDB
Query query = new Query(Criteria.where("status").is("PUBLISHED"));
query.withReadConcern(ReadConcern.MAJORITY);
List<Post> posts = mongoTemplate.find(query, Post.class);
```

### 4.4 Read Concern vs Write Concern 조합

| 조합 | 일관성 | 내구성 | 용도 |
|------|--------|--------|------|
| `local` + `w:1` | 낮음 | 낮음 | 로그, 임시 데이터 |
| `majority` + `w:majority` | 높음 | 높음 | 중요 비즈니스 데이터 |
| `snapshot` + `w:majority` | 트랜잭션 | 높음 | Multi-Doc Transaction |

---

## 5. Read Preference

### 5.1 개념

Read Preference는 **어느 노드에서 읽을지** 결정합니다.

### 5.2 옵션

| Read Preference | 설명 | 사용 사례 |
|-----------------|------|----------|
| `primary` | Primary에서만 읽기 (기본값) | 최신 데이터 필요 |
| `primaryPreferred` | Primary 우선, 불가능 시 Secondary | 약간의 지연 허용 |
| `secondary` | Secondary에서만 읽기 | 읽기 부하 분산 |
| `secondaryPreferred` | Secondary 우선, 불가능 시 Primary | 분석/리포팅 |
| `nearest` | 지연 시간 가장 짧은 노드 | 지리적 분산 |

### 5.3 사용법

```javascript
// MongoDB Shell
db.posts.find({ status: "PUBLISHED" })
  .readPref("secondaryPreferred")
```

```java
// Spring Data MongoDB
Query query = new Query(Criteria.where("status").is("PUBLISHED"));
query.withReadPreference(ReadPreference.secondaryPreferred());
List<Post> posts = mongoTemplate.find(query, Post.class);
```

---

## 6. Portal Universe 트랜잭션 전략

### 6.1 현재 구현 분석

```java
// LikeService.java - @Transactional 사용
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    @Transactional  // 쓰기 트랜잭션
    public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

        if (existingLike != null) {
            likeRepository.delete(existingLike);       // likes 컬렉션
            post.decrementLikeCount();
        } else {
            likeRepository.save(newLike);              // likes 컬렉션
            post.incrementLikeCount();
        }

        postRepository.save(post);                      // posts 컬렉션
        // 두 컬렉션 연산이 하나의 트랜잭션으로 처리
        return LikeToggleResponse.of(liked, post.getLikeCount());
    }
}
```

### 6.2 트랜잭션이 필요한 시나리오

| 시나리오 | 컬렉션 | 트랜잭션 필요 |
|----------|--------|--------------|
| 좋아요 토글 | likes + posts | O (카운트 동기화) |
| 댓글 작성 | comments + posts | O (카운트 동기화) |
| 게시물 발행 | posts + tags | O (태그 카운트 동기화) |
| 게시물 조회 | posts | X (단일 문서) |
| 조회수 증가 | posts | X (단일 문서) |

### 6.3 Eventual Consistency 대안

트랜잭션 대신 **최종 일관성**으로 처리할 수도 있습니다.

```java
// 방법 1: 트랜잭션 (강한 일관성)
@Transactional
public void toggleLike() {
    // likes, posts 동시 업데이트
}

// 방법 2: 비동기 동기화 (최종 일관성)
public void toggleLike() {
    likeRepository.save(like);  // 즉시 저장

    // 카운트는 비동기로 동기화 (Kafka 이벤트)
    eventPublisher.publish(new LikeCreatedEvent(postId));
}

// 이벤트 컨슈머
@KafkaListener(topics = "like-events")
public void handleLikeEvent(LikeCreatedEvent event) {
    postRepository.incrementLikeCount(event.getPostId());
}
```

### 6.4 트랜잭션 없이 원자성 보장

단일 문서 연산으로 설계하면 트랜잭션 없이도 원자성 보장됩니다.

```javascript
// 좋아요를 Post에 Embedding (비권장 - 배열 커질 수 있음)
db.posts.updateOne(
  { _id: "post123" },
  {
    $addToSet: { likedBy: "user456" },  // 중복 없이 추가
    $inc: { likeCount: 1 }
  }
)

// 단일 문서 연산이므로 항상 원자적
```

---

## 7. 트랜잭션 Best Practices

### 7.1 트랜잭션 사용 권장사항

| 권장 | 비권장 |
|------|--------|
| 짧은 트랜잭션 유지 | 긴 실행 시간 |
| 필요한 경우에만 사용 | 단일 문서에 트랜잭션 |
| 적절한 인덱스 설정 | 전체 컬렉션 스캔 |
| 재시도 로직 구현 | 무한 재시도 |

### 7.2 트랜잭션 제약사항

| 제약 | 설명 |
|------|------|
| **Replica Set 필수** | Standalone에서 사용 불가 |
| **시간 제한** | 기본 60초, 최대 24시간 |
| **WiredTiger 필요** | MMAPv1 미지원 |
| **크기 제한** | 16MB 문서 크기 제한 |
| **성능 오버헤드** | 락, 로그 기록 등 |

### 7.3 TransientTransactionError 처리

```java
@Retryable(
    value = { TransientTransactionError.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
@Transactional
public void criticalOperation() {
    // 재시도 가능한 트랜잭션 에러 자동 재시도
}
```

---

## 8. 실습: 트랜잭션 테스트

### 8.1 트랜잭션 성공 케이스

```java
@Test
@Transactional
void toggleLike_ShouldUpdateBothCollections() {
    // Given
    Post post = createPost();
    String userId = "user123";

    // When
    LikeToggleResponse response = likeService.toggleLike(
        post.getId(), userId, "User Name");

    // Then
    assertThat(response.isLiked()).isTrue();
    assertThat(response.getLikeCount()).isEqualTo(1);

    Post updatedPost = postRepository.findById(post.getId()).get();
    assertThat(updatedPost.getLikeCount()).isEqualTo(1);

    Like like = likeRepository.findByPostIdAndUserId(post.getId(), userId).get();
    assertThat(like).isNotNull();
}
```

### 8.2 트랜잭션 롤백 케이스

```java
@Test
void toggleLike_ShouldRollbackOnError() {
    // Given
    String invalidPostId = "non-existent";
    String userId = "user123";

    // When & Then
    assertThrows(CustomBusinessException.class, () ->
        likeService.toggleLike(invalidPostId, userId, "User Name")
    );

    // 좋아요가 생성되지 않았는지 확인
    assertThat(likeRepository.existsByPostIdAndUserId(invalidPostId, userId))
        .isFalse();
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Single Document** | 항상 원자적, 트랜잭션 불필요 |
| **Multi-Document Transaction** | 여러 문서/컬렉션 원자적 처리, 4.0+ |
| **Write Concern** | 쓰기 확인 수준 (w: majority 권장) |
| **Read Concern** | 읽기 일관성 수준 (majority, snapshot) |
| **Read Preference** | 읽기 노드 선택 (primary, secondary) |

| Portal Universe 전략 | 설명 |
|---------------------|------|
| `@Transactional` | 다중 컬렉션 동기화에 사용 |
| 카운터 역정규화 | Post.likeCount, Post.commentCount |
| 동기화 시점 | 좋아요/댓글 생성/삭제 시 |

---

## 다음 학습

- [MongoDB 데이터 모델링](./mongodb-data-modeling.md)
- [MongoDB Portal Universe 분석](./mongodb-portal-universe.md)

---

## 참고 자료

- [MongoDB Transactions](https://www.mongodb.com/docs/manual/core/transactions/)
- [Read Concern](https://www.mongodb.com/docs/manual/reference/read-concern/)
- [Write Concern](https://www.mongodb.com/docs/manual/reference/write-concern/)
- [Spring Data MongoDB Transactions](https://docs.spring.io/spring-data/mongodb/reference/mongodb/client-session-transactions.html)
