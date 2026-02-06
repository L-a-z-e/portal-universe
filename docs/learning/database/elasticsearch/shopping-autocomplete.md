# 자동완성 (Autocomplete) 구현

## 개요

Shopping Service는 Elasticsearch의 Completion Suggester와 Redis를 조합하여 빠르고 효율적인 자동완성 기능을 제공합니다. 이 문서에서는 자동완성 구현 패턴과 최적화 전략을 설명합니다.

## 자동완성 아키텍처

```
┌─────────────┐
│   Client    │
│ (사용자 입력)│
└──────┬──────┘
       │ "무선 이"
       ▼
┌──────────────┐
│  SuggestSvc  │
└──────┬───────┘
       │
       ├─────────────────────────────────┐
       │                                 │
       ▼                                 ▼
┌─────────────────┐           ┌─────────────────┐
│  Elasticsearch  │           │     Redis       │
│  (Completion    │           │ (Popular/Recent │
│   Suggester)    │           │   Keywords)     │
└─────────────────┘           └─────────────────┘
       │                                 │
       └─────────────┬───────────────────┘
                     │
                     ▼
              ┌─────────────┐
              │  Merged     │
              │  Results    │
              └─────────────┘
```

## Elasticsearch Completion Suggester

### 인덱스 매핑

```json
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "suggest": {
            "type": "completion",
            "analyzer": "korean"
          }
        }
      }
    }
  }
}
```

### Completion 필드 특징

- **빠른 검색**: FST(Finite State Transducer) 기반으로 O(1)에 가까운 조회
- **Fuzzy 지원**: 오타 허용 검색 가능
- **가중치**: 특정 결과에 우선순위 부여 가능

## SuggestService 구현

### 전체 코드

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestService {

    private static final String INDEX_NAME = "products";
    private static final String POPULAR_KEY = "search:popular";
    private static final String RECENT_KEY_PREFIX = "search:recent:";

    private final ElasticsearchClient esClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 자동완성 제안을 반환합니다.
     */
    public List<String> suggest(String keyword, int size) {
        if (keyword == null || keyword.length() < 2) {
            return List.of();
        }

        try {
            SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .suggest(su -> su
                    .suggesters("product-suggest", sg -> sg
                        .prefix(keyword)
                        .completion(c -> c
                            .field("name.suggest")
                            .size(size)
                            .skipDuplicates(true)
                            .fuzzy(f -> f.fuzziness("AUTO"))
                        )
                    )
                )
            );

            var response = esClient.search(request, Void.class);

            List<String> suggestions = new ArrayList<>();
            if (response.suggest() != null &&
                response.suggest().get("product-suggest") != null) {

                for (var suggestion : response.suggest().get("product-suggest")) {
                    if (suggestion.isCompletion()) {
                        for (var option : suggestion.completion().options()) {
                            suggestions.add(option.text());
                        }
                    }
                }
            }

            return suggestions;

        } catch (IOException e) {
            log.error("Failed to get suggestions for: {}", keyword, e);
            return List.of();
        }
    }

    /**
     * 인기 검색어 목록을 반환합니다.
     */
    public List<String> getPopularKeywords(int size) {
        Set<Object> keywords = redisTemplate.opsForZSet()
            .reverseRange(POPULAR_KEY, 0, size - 1);

        if (keywords == null) {
            return List.of();
        }

        return keywords.stream()
            .map(Object::toString)
            .toList();
    }

    /**
     * 검색어 검색 횟수를 증가시킵니다.
     */
    public void incrementSearchCount(String keyword) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, keyword, 1);
    }

    /**
     * 사용자의 최근 검색어를 반환합니다.
     */
    public List<String> getRecentKeywords(Long userId, int size) {
        String key = RECENT_KEY_PREFIX + userId;
        List<Object> keywords = redisTemplate.opsForList().range(key, 0, size - 1);

        if (keywords == null) {
            return List.of();
        }

        return keywords.stream()
            .map(Object::toString)
            .toList();
    }

    /**
     * 최근 검색어를 추가합니다.
     */
    public void addRecentKeyword(Long userId, String keyword) {
        String key = RECENT_KEY_PREFIX + userId;

        // 기존 중복 제거
        redisTemplate.opsForList().remove(key, 0, keyword);

        // 맨 앞에 추가
        redisTemplate.opsForList().leftPush(key, keyword);

        // 최근 20개만 유지
        redisTemplate.opsForList().trim(key, 0, 19);

        // 인기 검색어 통계에도 반영
        incrementSearchCount(keyword);
    }

    /**
     * 특정 최근 검색어를 삭제합니다.
     */
    public void deleteRecentKeyword(Long userId, String keyword) {
        String key = RECENT_KEY_PREFIX + userId;
        redisTemplate.opsForList().remove(key, 0, keyword);
    }

    /**
     * 사용자의 최근 검색어를 모두 삭제합니다.
     */
    public void clearRecentKeywords(Long userId) {
        String key = RECENT_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
```

## Fuzzy 검색

오타를 허용하는 자동완성:

```java
.completion(c -> c
    .field("name.suggest")
    .size(size)
    .fuzzy(f -> f
        .fuzziness("AUTO")    // 자동 오타 허용
        .transpositions(true)  // 문자 순서 바꿈 허용
        .minLength(3)          // 3자 이상부터 fuzzy 적용
        .prefixLength(1)       // 첫 1자는 정확히 매칭
    )
)
```

### Fuzziness 옵션

| 값 | 설명 |
|-----|------|
| `AUTO` | 입력 길이에 따라 자동 결정 (0-2: 0, 3-5: 1, >5: 2) |
| `0` | 정확한 매칭만 |
| `1` | 1글자 오타 허용 |
| `2` | 2글자 오타 허용 |

## 가중치 기반 자동완성

특정 상품이 우선 표시되도록 가중치 부여:

### 인덱스 매핑 확장

```json
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "fields": {
          "suggest": {
            "type": "completion",
            "contexts": [
              {
                "name": "category",
                "type": "category"
              }
            ]
          }
        }
      },
      "popularity": { "type": "integer" }
    }
  }
}
```

### 가중치 적용 인덱싱

```java
public void indexProductWithWeight(Product product, int popularity) {
    Map<String, Object> doc = new HashMap<>();
    doc.put("id", product.getId());
    doc.put("name", Map.of(
        "input", product.getName(),
        "weight", popularity  // 인기도를 가중치로
    ));

    esClient.index(IndexRequest.of(i -> i
        .index(INDEX_NAME)
        .id(String.valueOf(product.getId()))
        .document(doc)
    ));
}
```

## Redis 기반 인기/최근 검색어

### 데이터 구조

```
# 인기 검색어 (Sorted Set)
search:popular
├── "무선 이어폰": 1500
├── "블루투스 스피커": 1200
├── "충전 케이블": 800
└── ...

# 최근 검색어 (List per User)
search:recent:{userId}
├── "무선 이어폰"
├── "블루투스 스피커"
└── ...
```

### 인기 검색어 관리

```java
// 검색 시 카운트 증가
public void incrementSearchCount(String keyword) {
    redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, keyword, 1);
}

// 상위 N개 조회
public List<String> getPopularKeywords(int size) {
    Set<Object> keywords = redisTemplate.opsForZSet()
        .reverseRange(POPULAR_KEY, 0, size - 1);
    return keywords.stream().map(Object::toString).toList();
}

// 주기적 정리 (30일 이상 된 데이터)
@Scheduled(cron = "0 0 4 * * *")
public void cleanupOldKeywords() {
    // 점수가 낮은 키워드 삭제
    redisTemplate.opsForZSet().removeRangeByScore(POPULAR_KEY, 0, 10);
}
```

## REST API

### Controller

```java
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SuggestController {

    private final SuggestService suggestService;

    /**
     * 자동완성 제안
     * GET /api/v1/search/suggest?keyword=무선&size=10
     */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(suggestService.suggest(keyword, size))
        );
    }

    /**
     * 인기 검색어
     * GET /api/v1/search/popular?size=10
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<String>>> popular(
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(suggestService.getPopularKeywords(size))
        );
    }

    /**
     * 최근 검색어
     * GET /api/v1/search/recent?size=10
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> recent(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(suggestService.getRecentKeywords(user.getId(), size))
        );
    }

    /**
     * 최근 검색어 삭제
     * DELETE /api/v1/search/recent?keyword=무선
     */
    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> deleteRecent(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam String keyword
    ) {
        suggestService.deleteRecentKeyword(user.getId(), keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 최근 검색어 전체 삭제
     * DELETE /api/v1/search/recent/all
     */
    @DeleteMapping("/recent/all")
    public ResponseEntity<ApiResponse<Void>> clearRecent(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        suggestService.clearRecentKeywords(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

## 응답 예시

### 자동완성 응답

```json
{
  "success": true,
  "data": [
    "무선 이어폰",
    "무선 충전기",
    "무선 마우스",
    "무선 키보드"
  ]
}
```

### 인기 검색어 응답

```json
{
  "success": true,
  "data": [
    "무선 이어폰",
    "블루투스 스피커",
    "충전 케이블",
    "스마트 워치",
    "노트북 가방"
  ]
}
```

## 성능 최적화

### 1. 응답 시간 목표

| 기능 | 목표 | 현재 |
|------|------|------|
| 자동완성 | < 50ms | ~30ms |
| 인기 검색어 | < 10ms | ~5ms |
| 최근 검색어 | < 10ms | ~5ms |

### 2. 캐싱 전략

```java
@Cacheable(value = "suggestions", key = "#keyword + ':' + #size")
public List<String> suggest(String keyword, int size) {
    // ES 조회
}

// 캐시 설정
@Bean
public CacheManager cacheManager() {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5));
    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(config)
        .build();
}
```

### 3. Debounce (프론트엔드)

```javascript
// 300ms 디바운스로 API 호출 최소화
const debouncedSuggest = debounce(async (keyword) => {
  const response = await fetch(`/api/v1/search/suggest?keyword=${keyword}`);
  // ...
}, 300);
```

## Best Practices

1. **최소 입력 길이**: 2자 이상부터 자동완성 활성화
2. **결과 제한**: 기본 10개, 최대 20개
3. **캐싱**: 자주 검색되는 prefix 캐싱
4. **실시간 업데이트**: 새 상품 추가 시 즉시 반영
5. **개인화**: 사용자별 최근 검색어 제공

## 관련 문서

- [Product Index Design](./product-index-design.md) - 인덱스 설계
- [Search Service](./search-service.md) - 검색 서비스 로직
- [Search Ranking](./search-ranking.md) - 검색 결과 정렬
