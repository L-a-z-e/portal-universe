# Embedding vs Reference - 트레이드오프 분석

## 개요

MongoDB에서 관계 데이터를 모델링하는 두 가지 핵심 패턴인 Embedding과 Reference의 트레이드오프를 Blog Service 사례로 학습합니다.

## 기본 개념

### Embedding (내장)

```javascript
// Post 문서에 작성자 정보 내장
{
    "_id": "post123",
    "title": "Vue.js 시작하기",
    "authorId": "user456",
    "authorName": "홍길동",    // 내장된 정보
    "tags": ["vue", "javascript"],  // 배열로 내장
    "likeCount": 42           // 역정규화된 카운터
}
```

### Reference (참조)

```javascript
// Post 문서에서 다른 컬렉션 참조
{
    "_id": "post123",
    "title": "Vue.js 시작하기",
    "authorId": "user456"     // User 컬렉션 참조
}

// 별도 Like 컬렉션
{
    "_id": "like789",
    "postId": "post123",      // Post 참조
    "userId": "user456"
}
```

## Blog Service의 결정들

### 1. 작성자 정보: Embedding 선택

```java
// Post.java
private String authorId;    // Reference (조회/갱신용)
private String authorName;  // Embedded (표시용)
```

**분석:**

| 기준 | Embedding | Reference |
|------|-----------|-----------|
| 조회 빈도 | 높음 (목록 표시) | - |
| 변경 빈도 | 낮음 (이름 변경 드묾) | - |
| 일관성 요구 | 낮음 (약간 지연 허용) | - |
| N+1 문제 | 방지 | 발생 가능 |

**결정:** 이름 변경 빈도 < 조회 빈도이므로 Embedding

### 2. 태그: 이중 저장 (Hybrid)

```java
// Post.java - 빠른 조회용
private Set<String> tags = new HashSet<>();

// Tag.java - 통계/관리용
@Document(collection = "tags")
public class Tag {
    private String name;
    private Long postCount;
}
```

**분석:**
- Post 조회 시 태그 즉시 표시 필요 → Embedding
- 인기 태그 통계 필요 → 별도 컬렉션

### 3. 좋아요: Reference + 역정규화

```java
// Like.java - Reference
@Document(collection = "likes")
public class Like {
    private String postId;
    private String userId;
}

// Post.java - 역정규화된 카운터
private Long likeCount = 0L;
```

**분석:**

| 패턴 | 장점 | 단점 |
|------|------|------|
| Like만 사용 | 정확한 데이터 | 매번 count 필요 |
| Post.likeCount만 | 빠른 조회 | 중복 좋아요 방지 어려움 |
| 혼합 | 둘 다 해결 | 동기화 필요 |

**결정:** 혼합 패턴 (중복 방지 + 빠른 카운트)

### 4. 댓글: Reference 선택

```java
// Comment.java - 별도 컬렉션
@Document(collection = "comments")
public class Comment {
    private String postId;
    private String parentCommentId;  // 대댓글
}
```

**Embedding을 선택하지 않은 이유:**
- 댓글 개수 무제한 → 문서 크기 초과 가능 (16MB 제한)
- 댓글 개별 수정/삭제 필요
- 대댓글 구조 복잡

### 5. 시리즈: Reference 선택

```java
// Series.java
private List<String> postIds;  // Post ID만 저장
```

**Embedding을 선택하지 않은 이유:**
- Post 수정 시 Series 업데이트 불필요
- Post 정보는 자주 변경됨
- 순서만 관리하면 되므로 ID 목록으로 충분

## 결정 가이드

### Embedding 적합 조건

1. **One-to-Few 관계** (1:소수)
2. **함께 조회되는 데이터**
3. **자주 변경되지 않는 데이터**
4. **독립적으로 접근할 필요 없는 데이터**

```java
// 예: Post에 내장된 authorName
private String authorName;  // 변경 드묾, 항상 함께 표시
```

### Reference 적합 조건

1. **One-to-Many, Many-to-Many 관계**
2. **독립적으로 접근하는 데이터**
3. **자주 변경되는 데이터**
4. **무제한 증가 가능한 데이터**

```java
// 예: 댓글 (무제한 증가)
@Document(collection = "comments")
public class Comment { ... }
```

## 역정규화 패턴

### 카운터 역정규화

```java
// Post.java
private Long viewCount = 0L;
private Long likeCount = 0L;
private Long commentCount = 0L;
```

**장점:**
- 정렬/필터링에 즉시 사용
- Aggregation 없이 표시

**단점:**
- 동기화 필요
- 약간의 비일관성 가능

### 동기화 전략

```java
// 실시간 동기화
public void incrementLikeCount() {
    this.likeCount++;
}

// 배치 동기화 (정기 검증)
@Scheduled(cron = "0 0 3 * * ?")
public void syncLikeCounts() {
    // 실제 Like count와 Post.likeCount 비교/수정
}
```

## 문서 크기 고려

```
MongoDB 문서 크기 제한: 16MB
```

### 안전한 Embedding

```java
// 제한된 크기의 배열
private Set<String> tags;        // 최대 수십 개
private List<String> images;     // 최대 수십 개
```

### 위험한 Embedding

```java
// 무제한 증가 가능 - Reference 사용
private List<Comment> comments;  // 수천 개 가능 → 별도 컬렉션
private List<Like> likes;        // 수만 개 가능 → 별도 컬렉션
```

## 성능 비교

| 패턴 | 읽기 성능 | 쓰기 성능 | 일관성 |
|------|----------|----------|--------|
| Embedding | 우수 | 보통 | 높음 |
| Reference | 보통 | 우수 | 높음 |
| 역정규화 | 우수 | 추가 작업 필요 | 최종 일관성 |

## Blog Service 요약

| 데이터 | 패턴 | 이유 |
|--------|------|------|
| authorName | Embedding | 조회 빈도 높음, 변경 드묾 |
| tags (Post) | Embedding | 함께 표시, 개수 제한적 |
| tags (통계) | Reference | 집계/관리 필요 |
| likes | Reference + 역정규화 | 중복 방지 + 빠른 카운트 |
| comments | Reference | 무제한 증가, 개별 관리 |
| series.postIds | Reference | Post 변경과 독립 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/domain/Like.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/domain/Comment.java`
