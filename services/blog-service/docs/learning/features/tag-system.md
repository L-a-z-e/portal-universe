# Tag System

## 개요

Blog Service의 태그 시스템 구현을 학습합니다. 태그 정규화, 인기 태그, 자동완성 등을 포함합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tags` | 태그 생성 |
| GET | `/api/v1/tags` | 전체 태그 목록 |
| GET | `/api/v1/tags/{name}` | 태그 상세 |
| GET | `/api/v1/tags/popular` | 인기 태그 목록 |
| GET | `/api/v1/tags/recent` | 최근 사용 태그 |
| GET | `/api/v1/tags/search` | 태그 검색 (자동완성) |

## 태그 정규화

### 정규화 규칙

```java
// Tag.java
public static String normalizeName(String name) {
    if (name == null) return null;
    return name.trim().toLowerCase();
}
```

**예시:**
- `"Vue.js"` → `"vue.js"`
- `" Spring Boot "` → `"spring boot"`
- `"MongoDB"` → `"mongodb"`

### 태그 생성/조회

```java
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
            log.info("Auto-created tag: {}", normalizedName);
            return newTag;
        });
}
```

## 태그 카운터 관리

### Post 생성 시

```java
public void incrementTagPostCounts(List<String> tagNames) {
    tagNames.forEach(tagName -> {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
            .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        tag.incrementPostCount();
        tagRepository.save(tag);
    });
}
```

### Post 삭제 시

```java
public void decrementTagPostCounts(List<String> tagNames) {
    tagNames.forEach(tagName -> {
        String normalizedName = Tag.normalizeName(tagName);
        tagRepository.findByNameIgnoreCase(normalizedName)
            .ifPresent(tag -> {
                tag.decrementPostCount();
                tagRepository.save(tag);
            });
    });
}
```

### Domain 메서드

```java
// Tag.java
public void incrementPostCount() {
    this.postCount++;
    this.lastUsedAt = LocalDateTime.now();
}

public void decrementPostCount() {
    if (this.postCount > 0) {
        this.postCount--;
    }
}
```

## 인기 태그 조회

### Service 구현

```java
@Transactional(readOnly = true)
public List<TagStatsResponse> getPopularTags(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByPostCountDesc(0L, pageable);

    return tags.stream()
        .map(tag -> new TagStatsResponse(tag.getName(), tag.getPostCount()))
        .toList();
}
```

### Response DTO

```java
public record TagStatsResponse(
    String name,
    Long postCount
) {}
```

## 태그 검색 (자동완성)

### Service 구현

```java
@Transactional(readOnly = true)
public List<TagResponse> searchTags(String keyword, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByNameContainingIgnoreCaseOrderByPostCountDesc(
        keyword, pageable);

    return tags.stream()
        .map(this::toResponse)
        .toList();
}
```

### Controller

```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<List<TagResponse>>> searchTags(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "10") int limit) {

    return ResponseEntity.ok(
        ApiResponse.success(tagService.searchTags(keyword, limit))
    );
}
```

## 최근 사용 태그

```java
@Transactional(readOnly = true)
public List<TagResponse> getRecentlyUsedTags(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByLastUsedAtDesc(0L, pageable);

    return tags.stream()
        .map(this::toResponse)
        .toList();
}
```

## 미사용 태그 정리

### Service 구현

```java
// 관리자용: 사용되지 않는 태그 삭제
public void deleteUnusedTags() {
    List<Tag> unusedTags = tagRepository.findByPostCount(0L);
    tagRepository.deleteAll(unusedTags);
    log.info("Deleted {} unused tags", unusedTags.size());
}
```

### Scheduled Job (선택적)

```java
@Scheduled(cron = "0 0 4 * * SUN")  // 매주 일요일 새벽 4시
public void cleanupUnusedTags() {
    tagService.deleteUnusedTags();
}
```

## Repository

```java
public interface TagRepository extends MongoRepository<Tag, String> {

    // 정규화된 이름으로 조회 (대소문자 무시)
    Optional<Tag> findByNameIgnoreCase(String name);

    // 중복 체크
    boolean existsByNameIgnoreCase(String name);

    // 인기 태그 (postCount 내림차순)
    List<Tag> findByPostCountGreaterThanOrderByPostCountDesc(Long minCount, Pageable pageable);

    // 최근 사용 태그
    List<Tag> findByPostCountGreaterThanOrderByLastUsedAtDesc(Long minCount, Pageable pageable);

    // 자동완성 검색
    List<Tag> findByNameContainingIgnoreCaseOrderByPostCountDesc(String keyword, Pageable pageable);

    // 미사용 태그 조회
    List<Tag> findByPostCount(Long count);
}
```

## Response DTO

```java
public record TagResponse(
    String id,
    String name,
    Long postCount,
    String description,
    LocalDateTime createdAt,
    LocalDateTime lastUsedAt
) {}
```

## 프론트엔드: 태그 입력

### 태그 자동완성 컴포넌트 (Vue)

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import { searchTags } from '@/api/tags'
import { useDebounceFn } from '@vueuse/core'

const model = defineModel<string[]>({ default: () => [] })

const input = ref('')
const suggestions = ref<string[]>([])

const search = useDebounceFn(async (keyword: string) => {
  if (keyword.length < 2) {
    suggestions.value = []
    return
  }
  const tags = await searchTags(keyword)
  suggestions.value = tags.map(t => t.name)
}, 300)

watch(input, (value) => search(value))

function addTag(tag: string) {
  const normalized = tag.trim().toLowerCase()
  if (normalized && !model.value.includes(normalized)) {
    model.value = [...model.value, normalized]
  }
  input.value = ''
  suggestions.value = []
}

function removeTag(tag: string) {
  model.value = model.value.filter(t => t !== tag)
}
</script>

<template>
  <div class="tag-input">
    <!-- 선택된 태그 -->
    <div class="selected-tags">
      <span v-for="tag in model" :key="tag" class="tag-chip">
        {{ tag }}
        <button @click="removeTag(tag)">×</button>
      </span>
    </div>

    <!-- 입력 -->
    <input
      v-model="input"
      @keydown.enter.prevent="addTag(input)"
      placeholder="태그 입력..."
    />

    <!-- 자동완성 목록 -->
    <ul v-if="suggestions.length" class="suggestions">
      <li
        v-for="suggestion in suggestions"
        :key="suggestion"
        @click="addTag(suggestion)"
      >
        {{ suggestion }}
      </li>
    </ul>
  </div>
</template>
```

## 태그 클라우드

### 프론트엔드 구현

```vue
<script setup lang="ts">
import { computed } from 'vue'

interface TagStat {
  name: string
  postCount: number
}

const props = defineProps<{
  tags: TagStat[]
}>()

const maxCount = computed(() =>
  Math.max(...props.tags.map(t => t.postCount))
)

function getTagSize(postCount: number): number {
  const minSize = 12
  const maxSize = 32
  return minSize + (postCount / maxCount.value) * (maxSize - minSize)
}
</script>

<template>
  <div class="tag-cloud">
    <router-link
      v-for="tag in tags"
      :key="tag.name"
      :to="`/posts?tag=${tag.name}`"
      :style="{ fontSize: `${getTagSize(tag.postCount)}px` }"
      class="tag-link"
    >
      {{ tag.name }}
    </router-link>
  </div>
</template>
```

## 에러 코드

```java
TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "B050", "Tag not found"),
TAG_ALREADY_EXISTS(HttpStatus.CONFLICT, "B051", "Tag already exists");
```

## 핵심 포인트

| 기능 | 핵심 사항 |
|------|----------|
| 정규화 | 소문자 변환, 공백 제거 |
| 저장 | Tag 컬렉션 + Post.tags 이중 |
| 카운터 | postCount 역정규화 |
| 자동완성 | 부분 일치 검색 |
| 정리 | 미사용 태그 주기적 삭제 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/domain/Tag.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/service/TagService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/tag/controller/TagController.java`
