# Tag Document - 태그 정규화

## 개요

블로그 포스트 분류 및 검색을 위한 태그 시스템의 정규화 전략을 학습합니다.

## Document Schema

```java
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;

    @TextIndexed
    @Indexed(unique = true)
    @NotBlank(message = "태그 이름은 필수입니다")
    @Size(max = 50)
    private String name;            // 정규화된 태그 이름

    @Builder.Default
    private Long postCount = 0L;    // 역정규화된 사용 횟수

    @Size(max = 200)
    private String description;     // 태그 설명 (선택적)

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt; // 최근 사용 시간
}
```

## 이중 저장 전략

### 1. Tag 컬렉션 (정규화)
- 태그 메타데이터 관리
- 통계 집계용

### 2. Post.tags 필드 (비정규화)
- 빠른 조회용
- 태그 이름 직접 저장

```java
// Post에서의 태그 저장
@Indexed
private Set<String> tags = new HashSet<>();
```

## 태그 정규화

```java
/**
 * 태그 이름 정규화 (소문자 변환, 공백 제거)
 */
public static String normalizeName(String name) {
    if (name == null) return null;
    return name.trim().toLowerCase();
}
```

**정규화 예시:**
- `"Vue.js"` → `"vue.js"`
- `" Spring Boot "` → `"spring boot"`
- `"MongoDB"` → `"mongodb"`

## 포스트 카운트 관리

### 증가/감소 메서드

```java
// 포스트 개수 증가
public void incrementPostCount() {
    this.postCount++;
    this.lastUsedAt = LocalDateTime.now();
}

// 포스트 개수 감소
public void decrementPostCount() {
    if (this.postCount > 0) {
        this.postCount--;
    }
}
```

### Service에서 동기화

```java
@Service
public class TagService {

    // 태그 자동 생성 또는 기존 태그 반환
    public Tag getOrCreateTag(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);

        return tagRepository.findByNameIgnoreCase(normalizedName)
            .orElseGet(() -> {
                Tag newTag = Tag.builder()
                    .name(normalizedName)
                    .createdAt(LocalDateTime.now())
                    .lastUsedAt(LocalDateTime.now())
                    .build();
                tagRepository.save(newTag);
                return newTag;
            });
    }

    // 여러 태그의 포스트 카운트 일괄 증가
    public void incrementTagPostCounts(List<String> tagNames) {
        tagNames.forEach(this::incrementTagPostCount);
    }

    // 여러 태그의 포스트 카운트 일괄 감소
    public void decrementTagPostCounts(List<String> tagNames) {
        tagNames.forEach(this::decrementTagPostCount);
    }
}
```

## 태그 조회 기능

### 인기 태그

```java
// postCount 기준 인기 태그 조회
public List<TagStatsResponse> getPopularTags(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByPostCountDesc(0L, pageable);

    return tags.stream()
        .map(tag -> new TagStatsResponse(tag.getName(), tag.getPostCount()))
        .toList();
}
```

### 최근 사용 태그

```java
public List<TagResponse> getRecentlyUsedTags(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByLastUsedAtDesc(0L, pageable);
    return tags.stream().map(this::toResponse).toList();
}
```

### 태그 검색 (자동완성)

```java
public List<TagResponse> searchTags(String keyword, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByNameContainingIgnoreCaseOrderByPostCountDesc(
        keyword, pageable);
    return tags.stream().map(this::toResponse).toList();
}
```

## Repository

```java
public interface TagRepository extends MongoRepository<Tag, String> {

    // 정규화된 이름으로 조회
    Optional<Tag> findByNameIgnoreCase(String name);

    // 중복 체크
    boolean existsByNameIgnoreCase(String name);

    // 인기 태그 (postCount > 0)
    List<Tag> findByPostCountGreaterThanOrderByPostCountDesc(Long minCount, Pageable pageable);

    // 최근 사용 태그
    List<Tag> findByPostCountGreaterThanOrderByLastUsedAtDesc(Long minCount, Pageable pageable);

    // 자동완성 검색
    List<Tag> findByNameContainingIgnoreCaseOrderByPostCountDesc(String keyword, Pageable pageable);

    // 미사용 태그 조회
    List<Tag> findByPostCount(Long count);
}
```

## 미사용 태그 정리

```java
// 관리자용: 사용되지 않는 태그 삭제
public void deleteUnusedTags() {
    List<Tag> unusedTags = tagRepository.findByPostCount(0L);
    tagRepository.deleteAll(unusedTags);
    log.info("Deleted {} unused tags", unusedTags.size());
}

// 태그가 사용되지 않는지 확인
public boolean isUnused() {
    return this.postCount == 0;
}
```

## 태그 클라우드 데이터

```java
// 태그 클라우드용 응답
public record TagStatsResponse(
    String name,
    Long postCount
) {}

// 크기 계산 (프론트엔드)
function getTagSize(postCount, maxCount) {
    const minSize = 12;
    const maxSize = 32;
    return minSize + (postCount / maxCount) * (maxSize - minSize);
}
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 정규화 | 소문자 변환, 공백 제거 |
| 저장 전략 | Tag 컬렉션 + Post.tags 이중 저장 |
| 카운터 | postCount 역정규화 |
| 정리 | 미사용 태그 주기적 삭제 |

## 동기화 타이밍

| 이벤트 | 액션 |
|--------|------|
| Post 생성 | 태그 생성/조회 + postCount 증가 |
| Post 삭제 | postCount 감소 |
| Post 태그 변경 | 추가된 태그 증가, 제거된 태그 감소 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/domain/Tag.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/service/TagService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/repository/TagRepository.java`
