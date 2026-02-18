package com.portal.universe.blogservice.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.like.domain.Like;
import com.portal.universe.blogservice.like.repository.LikeRepository;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.blogservice.series.domain.Series;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import com.portal.universe.blogservice.tag.domain.Tag;
import com.portal.universe.blogservice.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 블로그 서비스 Seed Data 초기화.
 * resources/seed/ 디렉토리의 JSON 파일에서 데이터를 로드한다.
 * auth-service의 고정 UUID 사용자와 매칭되는 블로그 데이터를 생성한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final TagRepository tagRepository;
    private final SeriesRepository seriesRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    @Profile({"local", "docker"})
    public CommandLineRunner initBlogData() {
        return args -> {
            if (postRepository.count() > 0) {
                log.info("Blog seed data already exists, skipping");
                return;
            }

            log.info("Initializing blog seed data...");

            createTags();
            Map<String, String> postKeyToId = createPosts();
            createSeries(postKeyToId);
            List<Comment> comments = createComments(postKeyToId);
            createLikes(postKeyToId);
            updateDenormalizedFields(postKeyToId, comments);

            log.info("Blog seed data initialization completed");
        };
    }

    // ========== Tags ==========

    private void createTags() throws IOException {
        List<TagSeed> seeds = readSeed("tags.json", TagSeed.class);
        LocalDateTime now = LocalDateTime.now();

        for (TagSeed seed : seeds) {
            tagRepository.save(Tag.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .postCount(0L)
                    .createdAt(now)
                    .build());
        }

        log.info("Created {} tags", seeds.size());
    }

    // ========== Posts ==========

    private Map<String, String> createPosts() throws IOException {
        List<PostSeed> seeds = readSeed("posts.json", PostSeed.class);
        Map<String, String> postKeyToId = new LinkedHashMap<>();

        for (PostSeed seed : seeds) {
            Post post = postRepository.save(Post.builder()
                    .title(seed.title())
                    .content(seed.content())
                    .authorId(seed.authorId())
                    .authorName(seed.authorName())
                    .category(seed.category())
                    .tags(new HashSet<>(seed.tags()))
                    .status(PostStatus.valueOf(seed.status()))
                    .build());

            postKeyToId.put(seed.key(), post.getId());

            if (seed.publishedAt() != null) {
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("id").is(post.getId())),
                        new Update().set("publishedAt", seed.publishedAt()),
                        Post.class);
            }
        }

        updateTagPostCounts(seeds);
        log.info("Created {} posts", seeds.size());
        return postKeyToId;
    }

    private void updateTagPostCounts(List<PostSeed> seeds) {
        Map<String, Long> tagCounts = new HashMap<>();
        for (PostSeed seed : seeds) {
            if ("PUBLISHED".equals(seed.status())) {
                for (String tag : seed.tags()) {
                    tagCounts.merge(tag, 1L, Long::sum);
                }
            }
        }

        for (var entry : tagCounts.entrySet()) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("name").is(entry.getKey())),
                    new Update().set("postCount", entry.getValue())
                            .set("lastUsedAt", LocalDateTime.now()),
                    Tag.class);
        }
    }

    // ========== Series ==========

    private void createSeries(Map<String, String> postKeyToId) throws IOException {
        List<SeriesSeed> seeds = readSeed("series.json", SeriesSeed.class);
        LocalDateTime now = LocalDateTime.now();

        for (SeriesSeed seed : seeds) {
            List<String> postIds = seed.postKeys().stream()
                    .map(postKeyToId::get)
                    .toList();

            seriesRepository.save(Series.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .authorId(seed.authorId())
                    .authorName(seed.authorName())
                    .postIds(new ArrayList<>(postIds))
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        log.info("Created {} series", seeds.size());
    }

    // ========== Comments ==========

    private List<Comment> createComments(Map<String, String> postKeyToId) throws IOException {
        List<CommentSeed> seeds = readSeed("comments.json", CommentSeed.class);
        List<Comment> saved = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 10, 6, 10, 0);

        for (CommentSeed seed : seeds) {
            String parentId = null;
            if (seed.parentIndex() != null) {
                parentId = saved.get(seed.parentIndex()).getId();
            }

            Comment comment = commentRepository.save(Comment.builder()
                    .postId(postKeyToId.get(seed.postKey()))
                    .authorId(seed.authorId())
                    .authorName(seed.authorName())
                    .content(seed.content())
                    .parentCommentId(parentId)
                    .createdAt(base.plusDays(seed.daysAfterBase()))
                    .build());

            saved.add(comment);
        }

        log.info("Created {} comments", saved.size());
        return saved;
    }

    // ========== Likes ==========

    private void createLikes(Map<String, String> postKeyToId) throws IOException {
        List<LikeSeed> seeds = readSeed("likes.json", LikeSeed.class);

        for (LikeSeed seed : seeds) {
            likeRepository.save(Like.builder()
                    .postId(postKeyToId.get(seed.postKey()))
                    .userId(seed.userId())
                    .userName(seed.userName())
                    .build());
        }

        log.info("Created {} likes", seeds.size());
    }

    // ========== 역정규화 업데이트 ==========

    private void updateDenormalizedFields(Map<String, String> postKeyToId, List<Comment> comments) throws IOException {
        // commentCount
        Map<String, Long> commentCounts = new HashMap<>();
        for (Comment comment : comments) {
            commentCounts.merge(comment.getPostId(), 1L, Long::sum);
        }

        // likeCount
        List<LikeSeed> likeSeeds = readSeed("likes.json", LikeSeed.class);
        Map<String, Long> likeCounts = new HashMap<>();
        for (LikeSeed seed : likeSeeds) {
            String postId = postKeyToId.get(seed.postKey());
            likeCounts.merge(postId, 1L, Long::sum);
        }

        // 각 Post 업데이트
        for (String postId : postKeyToId.values()) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(postId)),
                    new Update()
                            .set("viewCount", ThreadLocalRandom.current().nextLong(30, 301))
                            .set("likeCount", likeCounts.getOrDefault(postId, 0L))
                            .set("commentCount", commentCounts.getOrDefault(postId, 0L)),
                    Post.class);
        }

        log.info("Updated denormalized fields for {} posts", postKeyToId.size());
    }

    // ========== JSON 로딩 ==========

    private <T> List<T> readSeed(String filename, Class<T> type) throws IOException {
        Resource resource = new ClassPathResource("seed/" + filename);
        return objectMapper.readValue(resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    // ========== Seed DTOs ==========

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TagSeed(String name, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PostSeed(String key, String title, String content, String authorId,
                    String authorName, String category, List<String> tags,
                    String status, LocalDateTime publishedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeriesSeed(String name, String description, String authorId,
                      String authorName, List<String> postKeys) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommentSeed(String postKey, Integer parentIndex, String authorId,
                       String authorName, String content, int daysAfterBase) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LikeSeed(String postKey, String userId, String userName) {}
}
