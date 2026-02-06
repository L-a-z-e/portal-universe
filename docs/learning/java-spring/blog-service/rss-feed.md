# RSS Feed

## 개요

Blog Service의 RSS 피드 기능 구현을 학습합니다. 블로그 구독자를 위한 표준 RSS 2.0 형식을 제공합니다.

## RSS 2.0 형식

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Portal Universe Blog</title>
    <link>https://portal-universe.com/blog</link>
    <description>기술 블로그</description>
    <language>ko</language>
    <lastBuildDate>Wed, 22 Jan 2025 00:00:00 GMT</lastBuildDate>

    <item>
      <title>Vue.js 시작하기</title>
      <link>https://portal-universe.com/blog/posts/123</link>
      <description>Vue.js 입문 가이드...</description>
      <author>author@example.com (홍길동)</author>
      <category>Frontend</category>
      <pubDate>Tue, 21 Jan 2025 10:00:00 GMT</pubDate>
      <guid>https://portal-universe.com/blog/posts/123</guid>
    </item>

    <!-- 더 많은 item... -->
  </channel>
</rss>
```

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/feed/rss` | 전체 RSS 피드 |
| GET | `/api/v1/feed/rss/category/{category}` | 카테고리별 RSS |
| GET | `/api/v1/feed/rss/author/{authorId}` | 작성자별 RSS |
| GET | `/api/v1/feed/atom` | Atom 피드 (선택적) |

## Controller 구현

```java
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping(value = "/rss", produces = "application/rss+xml")
    public ResponseEntity<String> getRssFeed(
            @RequestParam(defaultValue = "20") int limit) {

        String rssFeed = feedService.generateRssFeed(limit);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/rss+xml; charset=UTF-8"))
            .body(rssFeed);
    }

    @GetMapping(value = "/rss/category/{category}", produces = "application/rss+xml")
    public ResponseEntity<String> getRssFeedByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "20") int limit) {

        String rssFeed = feedService.generateRssFeedByCategory(category, limit);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/rss+xml; charset=UTF-8"))
            .body(rssFeed);
    }

    @GetMapping(value = "/rss/author/{authorId}", produces = "application/rss+xml")
    public ResponseEntity<String> getRssFeedByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "20") int limit) {

        String rssFeed = feedService.generateRssFeedByAuthor(authorId, limit);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/rss+xml; charset=UTF-8"))
            .body(rssFeed);
    }
}
```

## Service 구현

### Rome 라이브러리 사용

```xml
<!-- build.gradle -->
implementation 'com.rometools:rome:2.1.0'
```

### FeedService

```java
@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;

    @Value("${app.base-url:https://portal-universe.com}")
    private String baseUrl;

    public String generateRssFeed(int limit) {
        List<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(
            PostStatus.PUBLISHED,
            PageRequest.of(0, limit)
        ).getContent();

        return buildRssFeed(posts, "Portal Universe Blog", "기술 블로그");
    }

    public String generateRssFeedByCategory(String category, int limit) {
        List<Post> posts = postRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
            category,
            PostStatus.PUBLISHED,
            PageRequest.of(0, limit)
        ).getContent();

        return buildRssFeed(posts,
            "Portal Universe Blog - " + category,
            category + " 카테고리 글");
    }

    public String generateRssFeedByAuthor(String authorId, int limit) {
        List<Post> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(
            authorId,
            PostStatus.PUBLISHED,
            PageRequest.of(0, limit)
        ).getContent();

        String authorName = posts.isEmpty() ? authorId : posts.get(0).getAuthorName();

        return buildRssFeed(posts,
            authorName + "의 블로그",
            authorName + "님의 글 모음");
    }

    private String buildRssFeed(List<Post> posts, String title, String description) {
        try {
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType("rss_2.0");
            feed.setTitle(title);
            feed.setLink(baseUrl + "/blog");
            feed.setDescription(description);
            feed.setLanguage("ko");
            feed.setPublishedDate(new Date());

            List<SyndEntry> entries = posts.stream()
                .map(this::toSyndEntry)
                .toList();

            feed.setEntries(entries);

            SyndFeedOutput output = new SyndFeedOutput();
            return output.outputString(feed);

        } catch (FeedException e) {
            throw new RuntimeException("RSS 피드 생성 실패", e);
        }
    }

    private SyndEntry toSyndEntry(Post post) {
        SyndEntry entry = new SyndEntryImpl();

        entry.setTitle(post.getTitle());
        entry.setLink(baseUrl + "/blog/posts/" + post.getId());
        entry.setUri(baseUrl + "/blog/posts/" + post.getId());
        entry.setPublishedDate(toDate(post.getPublishedAt()));
        entry.setAuthor(post.getAuthorName());

        // 요약 (Description)
        SyndContent content = new SyndContentImpl();
        content.setType("text/html");
        content.setValue(post.getSummary());
        entry.setDescription(content);

        // 카테고리
        if (post.getCategory() != null) {
            SyndCategory category = new SyndCategoryImpl();
            category.setName(post.getCategory());
            entry.setCategories(List.of(category));
        }

        return entry;
    }

    private Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return new Date();
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
```

## 수동 XML 생성 (라이브러리 없이)

```java
private String buildRssFeedManually(List<Post> posts, String title, String description) {
    StringBuilder sb = new StringBuilder();

    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<rss version=\"2.0\">\n");
    sb.append("  <channel>\n");
    sb.append("    <title>").append(escapeXml(title)).append("</title>\n");
    sb.append("    <link>").append(baseUrl).append("/blog</link>\n");
    sb.append("    <description>").append(escapeXml(description)).append("</description>\n");
    sb.append("    <language>ko</language>\n");
    sb.append("    <lastBuildDate>").append(formatRfc822(LocalDateTime.now())).append("</lastBuildDate>\n");

    for (Post post : posts) {
        sb.append("    <item>\n");
        sb.append("      <title>").append(escapeXml(post.getTitle())).append("</title>\n");
        sb.append("      <link>").append(baseUrl).append("/blog/posts/").append(post.getId()).append("</link>\n");
        sb.append("      <description>").append(escapeXml(post.getSummary())).append("</description>\n");
        sb.append("      <author>").append(escapeXml(post.getAuthorName())).append("</author>\n");

        if (post.getCategory() != null) {
            sb.append("      <category>").append(escapeXml(post.getCategory())).append("</category>\n");
        }

        sb.append("      <pubDate>").append(formatRfc822(post.getPublishedAt())).append("</pubDate>\n");
        sb.append("      <guid>").append(baseUrl).append("/blog/posts/").append(post.getId()).append("</guid>\n");
        sb.append("    </item>\n");
    }

    sb.append("  </channel>\n");
    sb.append("</rss>");

    return sb.toString();
}

private String escapeXml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
}

private String formatRfc822(LocalDateTime dateTime) {
    if (dateTime == null) return "";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    return dateTime.atZone(ZoneId.systemDefault()).format(formatter);
}
```

## 캐싱 적용

```java
@Service
@RequiredArgsConstructor
public class FeedService {

    private final CacheManager cacheManager;

    @Cacheable(value = "rssFeed", key = "'main-' + #limit")
    public String generateRssFeed(int limit) {
        // RSS 생성 로직
    }

    @Cacheable(value = "rssFeed", key = "'category-' + #category + '-' + #limit")
    public String generateRssFeedByCategory(String category, int limit) {
        // RSS 생성 로직
    }

    // 게시물 변경 시 캐시 무효화
    @CacheEvict(value = "rssFeed", allEntries = true)
    public void invalidateRssFeedCache() {
        log.info("RSS feed cache invalidated");
    }
}
```

## 프론트엔드: RSS 구독 링크

### HTML Head

```html
<head>
  <!-- RSS Auto-discovery -->
  <link rel="alternate" type="application/rss+xml"
        title="Portal Universe Blog RSS Feed"
        href="/api/v1/feed/rss" />
</head>
```

### RSS 아이콘 컴포넌트

```vue
<template>
  <a :href="rssUrl" class="rss-link" target="_blank" rel="noopener">
    <RssIcon />
    <span>RSS 구독</span>
  </a>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps<{
  category?: string
  authorId?: string
}>()

const rssUrl = computed(() => {
  if (props.category) {
    return `/api/v1/feed/rss/category/${props.category}`
  }
  if (props.authorId) {
    return `/api/v1/feed/rss/author/${props.authorId}`
  }
  return '/api/v1/feed/rss'
})
</script>
```

## Atom 피드 (선택적)

```java
@GetMapping(value = "/atom", produces = "application/atom+xml")
public ResponseEntity<String> getAtomFeed(@RequestParam(defaultValue = "20") int limit) {
    // Rome 라이브러리에서 feedType을 "atom_1.0"으로 변경
    feed.setFeedType("atom_1.0");
    // ...
}
```

## 테스트

### cURL로 확인

```bash
# 전체 RSS
curl -H "Accept: application/rss+xml" http://localhost:8082/api/v1/feed/rss

# 카테고리별
curl http://localhost:8082/api/v1/feed/rss/category/Frontend

# 작성자별
curl http://localhost:8082/api/v1/feed/rss/author/user123
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 형식 | RSS 2.0 표준 |
| 라이브러리 | Rome (권장) 또는 수동 XML |
| 캐싱 | 성능을 위해 필수 |
| Content-Type | application/rss+xml |
| 인코딩 | UTF-8 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/feed/` (생성 필요)
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
