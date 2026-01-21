package com.portal.universe.blogservice.post.service;

import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostSortType;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.domain.SortDirection;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.blogservice.series.domain.Series;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostService 구현체
 * 기존 코드베이스 기반 + PRD Phase 1 블로그 핵심 기능 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final SeriesRepository seriesRepository;

    // ===== 기존 메서드 구현 (하위 호환성) =====

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        log.info("Creating post with title: {}, authorId: {}", request.title(), authorId);

        Post post = Post.builder()
                .title(request.title())
                .content(request.content())
                .summary(request.summary())
                .authorId(authorId)
                .authorName(extractAuthorName(authorId)) // JWT에서 추출하거나 별도 처리
                .status(request.publishImmediately() ? PostStatus.PUBLISHED : PostStatus.DRAFT)
                .tags(request.tags())
                .category(request.category())
                .metaDescription(request.metaDescription())
                .thumbnailUrl(request.thumbnailUrl())
                .productId(request.productId())
                .build();

        // 즉시 발행인 경우 발행일시 설정
        if (request.publishImmediately()) {
            post.publish();
        }

        Post savedPost = postRepository.save(post);
        log.info("Post created successfully with id: {}", savedPost.getId());

        return convertToPostResponse(savedPost);
    }

    @Override
    public List<PostResponse> getAllPosts() {
        log.info("Fetching all posts");
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PostResponse getPostById(String postId) {
        log.info("Fetching post by id: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));
        return convertToPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updatePost(String postId, PostUpdateRequest request, String userId) {
        log.info("Updating post id: {}, userId: {}", postId, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 권한 검증: 작성자만 수정 가능
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
        }

        // 도메인 메서드로 수정
        post.update(
                request.title(),
                request.content(),
                request.summary(),
                request.tags(),
                request.category(),
                request.metaDescription(),
                request.thumbnailUrl(),
                request.images()
        );

        Post updatedPost = postRepository.save(post);
        log.info("Post updated successfully: {}", postId);

        return convertToPostResponse(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(String postId, String userId) {
        log.info("Deleting post id: {}, userId: {}", postId, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 권한 검증: 작성자만 삭제 가능
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_DELETE_FORBIDDEN);
        }

        postRepository.delete(post);
        log.info("Post deleted successfully: {}", postId);
    }

    @Override
    public List<PostResponse> getPostsByProductId(String productId) {
        log.info("Fetching posts by productId: {}", productId);
        List<Post> posts = postRepository.findByProductId(productId);
        return posts.stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }

    // ===== 블로그 핵심 기능 확장 =====

    @Override
    public Page<PostSummaryResponse> getPublishedPosts(int page, int size) {
        log.info("Fetching published posts, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> getPostsByAuthor(String authorId, int page, int size) {
        log.info("Fetching posts by author: {}", authorId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> getPostsByAuthorAndStatus(String authorId, PostStatus status, int page, int size) {
        log.info("Fetching posts by author: {} and status: {}", authorId, status);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, status, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> getPostsByCategory(String category, int page, int size) {
        log.info("Fetching posts by category: {}", category);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
                category, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> getPostsByTags(List<String> tags, int page, int size) {
        log.info("Fetching posts by tags: {}", tags);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTagsInAndStatusOrderByPublishedAtDesc(
                tags, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> searchPosts(String keyword, int page, int size) {
        log.info("Searching posts with keyword: {}", keyword);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTextSearchAndStatus(keyword, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest searchRequest) {
        log.info("Advanced search with request: {}", searchRequest);

        // 기본 페이징 설정
        Pageable pageable = PageRequest.of(
                searchRequest.page(),
                searchRequest.size(),
                createSort(searchRequest.sortBy(), searchRequest.sortDirection())
        );

        // 키워드가 있으면 전문 검색
        if (searchRequest.keyword() != null && !searchRequest.keyword().isEmpty()) {
            PostStatus status = searchRequest.status() != null ? searchRequest.status() : PostStatus.PUBLISHED;
            Page<Post> posts = postRepository.findByTextSearchAndStatus(searchRequest.keyword(), status, pageable);
            return posts.map(this::convertToPostListResponse);
        }

        // 카테고리 필터
        if (searchRequest.category() != null) {
            return getPostsByCategory(searchRequest.category(), searchRequest.page(), searchRequest.size());
        }

        // 태그 필터
        if (searchRequest.tags() != null && !searchRequest.tags().isEmpty()) {
            return getPostsByTags(searchRequest.tags(), searchRequest.page(), searchRequest.size());
        }

        // 작성자 필터
        if (searchRequest.authorId() != null) {
            return getPostsByAuthor(searchRequest.authorId(), searchRequest.page(), searchRequest.size());
        }

        // 기본: 발행된 게시물 목록
        return getPublishedPosts(searchRequest.page(), searchRequest.size());
    }

    @Override
    @Transactional
    public PostResponse getPostByIdWithViewIncrement(String postId, String userId) {
        log.info("Fetching post with view increment, postId: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 조회 권한 확인
        if (!post.isViewableBy(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND);
        }

        // 조회수 증가
        post.incrementViewCount();
        postRepository.save(post);

        return convertToPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse changePostStatus(String postId, PostStatus newStatus, String userId) {
        log.info("Changing post status, postId: {}, newStatus: {}", postId, newStatus);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 권한 검증
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
        }

        // 상태 변경
        if (newStatus == PostStatus.PUBLISHED) {
            post.publish();
        } else if (newStatus == PostStatus.DRAFT) {
            post.unpublish();
        } else {
            // ARCHIVED 등 다른 상태 처리
            // post에 상태 변경 메서드 추가 필요
        }

        Post updatedPost = postRepository.save(post);
        return convertToPostResponse(updatedPost);
    }

    @Override
    public Page<PostSummaryResponse> getPopularPosts(int page, int size) {
        log.info("Fetching popular posts, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByStatusOrderByViewCountDescPublishedAtDesc(
                PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostSummaryResponse> getTrendingPosts(String period, int page, int size) {
        log.info("Fetching trending posts, period: {}, page: {}, size: {}", period, page, size);

        LocalDateTime startDate = calculateStartDateByPeriod(period);
        LocalDateTime endDate = LocalDateTime.now();

        // Phase 3: 시간 가중치 트렌딩 점수 기반 정렬
        // 전체 게시물을 가져와서 점수 계산 후 정렬 (메모리 내 정렬)
        List<Post> allPosts = postRepository.findPopularPostsInPeriod(
                PostStatus.PUBLISHED, startDate, endDate, Pageable.unpaged()).getContent();

        // 트렌딩 점수 계산 및 정렬
        List<Post> sortedPosts = allPosts.stream()
                .sorted((p1, p2) -> Double.compare(
                        calculateTrendingScore(p2, period),
                        calculateTrendingScore(p1, period)
                ))
                .toList();

        // 페이징 처리
        int start = page * size;
        int end = Math.min(start + size, sortedPosts.size());

        if (start >= sortedPosts.size()) {
            return Page.empty(PageRequest.of(page, size));
        }

        List<PostSummaryResponse> pagedContent = sortedPosts.subList(start, end).stream()
                .map(this::convertToPostListResponse)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                pagedContent,
                PageRequest.of(page, size),
                sortedPosts.size()
        );
    }

    /**
     * Phase 3: 트렌딩 점수 계산
     * 공식: score = (views * 1 + likes * 3 + comments * 5) * timeDecay
     * - 조회수: 기본 참여 지표 (가중치 1)
     * - 좋아요: 적극적 참여 지표 (가중치 3)
     * - 댓글: 최고 참여 지표 (가중치 5)
     * - 시간 감쇠: 오래된 게시물은 점수 감소
     */
    private double calculateTrendingScore(Post post, String period) {
        // 기본 점수 (가중치 적용)
        double viewScore = post.getViewCount() * 1.0;
        double likeScore = post.getLikeCount() * 3.0;
        double commentScore = (post.getCommentCount() != null ? post.getCommentCount() : 0L) * 5.0;

        double baseScore = viewScore + likeScore + commentScore;

        // 시간 감쇠 계산
        double timeDecay = calculateTimeDecay(post.getPublishedAt(), period);

        return baseScore * timeDecay;
    }

    /**
     * 시간 감쇠 계수 계산
     * 최근 게시물일수록 높은 점수, 기간에 따라 감쇠 속도 조절
     */
    private double calculateTimeDecay(LocalDateTime publishedAt, String period) {
        if (publishedAt == null) {
            return 0.1; // 발행일이 없으면 최소 점수
        }

        long hoursElapsed = java.time.Duration.between(publishedAt, LocalDateTime.now()).toHours();

        // 기간별 반감기 설정 (시간 단위)
        double halfLife = switch (period) {
            case "today" -> 6.0;    // 6시간마다 점수 반감
            case "week" -> 48.0;    // 48시간(2일)마다 점수 반감
            case "month" -> 168.0;  // 168시간(7일)마다 점수 반감
            case "year" -> 720.0;   // 720시간(30일)마다 점수 반감
            default -> 48.0;        // 기본값: 2일
        };

        // 지수 감쇠: decay = 2^(-hoursElapsed / halfLife)
        return Math.pow(2, -hoursElapsed / halfLife);
    }

    /**
     * 기간 문자열을 기준으로 시작 날짜 계산
     */
    private LocalDateTime calculateStartDateByPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "today" -> now.toLocalDate().atStartOfDay();
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(7); // 기본값: 1주일
        };
    }

    @Override
    public List<PostSummaryResponse> getRelatedPosts(String postId, int limit) {
        log.info("Fetching related posts for postId: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        List<Post> relatedPosts = postRepository.findRelatedPosts(
                post.getCategory(),
                new ArrayList<>(post.getTags()),
                PostStatus.PUBLISHED,
                postId
        );

        return relatedPosts.stream()
                .limit(limit)
                .map(this::convertToPostListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostSummaryResponse> getRecentPosts(int limit) {
        log.info("Fetching recent posts, limit: {}", limit);
        LocalDateTime since = LocalDateTime.now().minusDays(30); // 최근 30일
        Pageable pageable = PageRequest.of(0, limit);
        Page<Post> posts = postRepository.findByStatusAndPublishedAtAfterOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, since, pageable);
        return posts.map(this::convertToPostListResponse).getContent();
    }

    // ===== 통계 및 메타 정보 =====

    @Override
    public List<CategoryStats> getCategoryStats() {
        log.info("Fetching category statistics");

        List<String> categories = postRepository.findDistinctCategoriesByStatus(PostStatus.PUBLISHED);

        return categories.stream()
                .filter(Objects::nonNull)
                .map(category -> {
                    long count = postRepository.countByCategoryAndStatus(category, PostStatus.PUBLISHED);
                    // 최신 게시물 날짜 조회 (간단히 하기 위해 첫 번째 게시물 사용)
                    Page<Post> latestPost = postRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
                            category, PostStatus.PUBLISHED, PageRequest.of(0, 1));
                    LocalDateTime latestDate = latestPost.hasContent()
                            ? latestPost.getContent().get(0).getPublishedAt()
                            : null;

                    return new CategoryStats(category, count, latestDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TagStats> getPopularTags(int limit) {
        log.info("Fetching popular tags, limit: {}", limit);

        // 모든 발행된 게시물에서 태그 집계
        List<Post> posts = postRepository.findByStatusForTagAggregation(PostStatus.PUBLISHED);

        // 태그별 카운트 집계
        Map<String, Long> tagCountMap = new HashMap<>();
        Map<String, Long> tagViewsMap = new HashMap<>();

        for (Post post : posts) {
            for (String tag : post.getTags()) {
                tagCountMap.merge(tag, 1L, Long::sum);
                tagViewsMap.merge(tag, post.getViewCount(), Long::sum);
            }
        }

        // 카운트 기준으로 정렬하여 상위 limit개 반환
        return tagCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new TagStats(
                        entry.getKey(),
                        entry.getValue(),
                        tagViewsMap.getOrDefault(entry.getKey(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public AuthorStats getAuthorStats(String authorId) {
        log.info("Fetching author statistics for authorId: {}", authorId);

        long totalPosts = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED)
                + postRepository.countByAuthorIdAndStatus(authorId, PostStatus.DRAFT);
        long publishedPosts = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.PUBLISHED);

        // 작성자의 모든 게시물 조회
        List<Post> authorPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(
                authorId, Pageable.unpaged()).getContent();

        long totalViews = authorPosts.stream().mapToLong(Post::getViewCount).sum();
        long totalLikes = authorPosts.stream().mapToLong(Post::getLikeCount).sum();

        LocalDateTime firstPostDate = authorPosts.isEmpty() ? null
                : authorPosts.get(authorPosts.size() - 1).getCreatedAt();
        LocalDateTime lastPostDate = authorPosts.isEmpty() ? null
                : authorPosts.get(0).getCreatedAt();

        String authorName = authorPosts.isEmpty() ? null : authorPosts.get(0).getAuthorName();

        return new AuthorStats(
                authorId,
                authorName,
                totalPosts,
                publishedPosts,
                totalViews,
                totalLikes,
                firstPostDate,
                lastPostDate
        );
    }

    @Override
    public BlogStats getBlogStats() {
        log.info("Fetching blog statistics");

        long totalPosts = postRepository.count();
        long publishedPosts = postRepository.countByStatus(PostStatus.PUBLISHED);

        List<Post> allPosts = postRepository.findAll();
        long totalViews = allPosts.stream().mapToLong(Post::getViewCount).sum();
        long totalLikes = allPosts.stream().mapToLong(Post::getLikeCount).sum();

        // 상위 카테고리 (상위 5개)
        List<String> topCategories = getCategoryStats().stream()
                .sorted(Comparator.comparing(CategoryStats::postCount).reversed())
                .limit(5)
                .map(CategoryStats::categoryName)
                .collect(Collectors.toList());

        // 상위 태그 (상위 10개)
        List<String> topTags = getPopularTags(10).stream()
                .map(TagStats::tagName)
                .collect(Collectors.toList());

        // 최신 게시물 날짜
        LocalDateTime lastPostDate = postRepository.findByStatusOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(Post::getPublishedAt)
                .orElse(null);

        return new BlogStats(
                totalPosts,
                publishedPosts,
                totalViews,
                totalLikes,
                topCategories,
                topTags,
                lastPostDate
        );
    }

    @Override
    public PostNavigationResponse getPostNavigation(String postId, String scope) {
        log.info("Fetching post navigation for postId: {}, scope: {}", postId, scope);

        Post currentPost = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        if (currentPost.getPublishedAt() == null) {
            throw new CustomBusinessException(BlogErrorCode.POST_NOT_PUBLISHED);
        }

        PostSummaryResponse previousPost = null;
        PostSummaryResponse nextPost = null;
        SeriesNavigationResponse seriesNavigation = null;

        // scope에 따라 네비게이션 로직 선택
        String normalizedScope = scope != null ? scope.toLowerCase() : "all";

        switch (normalizedScope) {
            case "author":
                previousPost = getPreviousPostByAuthor(currentPost);
                nextPost = getNextPostByAuthor(currentPost);
                break;
            case "category":
                previousPost = getPreviousPostByCategory(currentPost);
                nextPost = getNextPostByCategory(currentPost);
                break;
            case "series":
                seriesNavigation = getSeriesNavigation(currentPost);
                // 시리즈 내부의 이전/다음은 seriesNavigation에만 포함
                break;
            default: // "all"
                previousPost = getPreviousPost(currentPost);
                nextPost = getNextPost(currentPost);
        }

        // 시리즈 정보는 항상 조회 (scope와 무관)
        if (!"series".equals(normalizedScope)) {
            seriesNavigation = getSeriesNavigation(currentPost);
        }

        return PostNavigationResponse.of(previousPost, nextPost, seriesNavigation);
    }

    private PostSummaryResponse getPreviousPost(Post currentPost) {
        return postRepository.findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private PostSummaryResponse getNextPost(Post currentPost) {
        return postRepository.findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
                        PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private PostSummaryResponse getPreviousPostByAuthor(Post currentPost) {
        return postRepository.findFirstByAuthorIdAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
                        currentPost.getAuthorId(), PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private PostSummaryResponse getNextPostByAuthor(Post currentPost) {
        return postRepository.findFirstByAuthorIdAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
                        currentPost.getAuthorId(), PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private PostSummaryResponse getPreviousPostByCategory(Post currentPost) {
        return postRepository.findFirstByCategoryAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
                        currentPost.getCategory(), PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private PostSummaryResponse getNextPostByCategory(Post currentPost) {
        return postRepository.findFirstByCategoryAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
                        currentPost.getCategory(), PostStatus.PUBLISHED, currentPost.getPublishedAt())
                .map(this::convertToPostListResponse)
                .orElse(null);
    }

    private SeriesNavigationResponse getSeriesNavigation(Post currentPost) {
        List<Series> seriesList = seriesRepository.findByPostIdsContaining(currentPost.getId());

        if (seriesList.isEmpty()) {
            return null;
        }

        // 첫 번째 시리즈 사용 (일반적으로 하나의 포스트는 하나의 시리즈에만 속함)
        Series series = seriesList.get(0);
        List<String> postIds = series.getPostIds();
        int currentIndex = postIds.indexOf(currentPost.getId());

        if (currentIndex == -1) {
            return null;
        }

        String previousPostId = currentIndex > 0 ? postIds.get(currentIndex - 1) : null;
        String nextPostId = currentIndex < postIds.size() - 1 ? postIds.get(currentIndex + 1) : null;

        return SeriesNavigationResponse.of(
                series.getId(),
                series.getName(),
                currentIndex,
                series.getPostCount(),
                previousPostId,
                nextPostId
        );
    }

    // ===== 변환 헬퍼 메서드 =====

    private PostResponse convertToPostResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getSummary(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getStatus(),
                post.getTags(),
                post.getCategory(),
                post.getMetaDescription(),
                post.getThumbnailUrl(),
                post.getImages(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt(),
                post.getProductId()
        );
    }

    private PostSummaryResponse convertToPostListResponse(Post post) {
        // 읽기 시간 계산 (평균 200자/분 기준)
        int estimatedReadTime = calculateReadTime(post.getContent());

        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getTags(),
                post.getCategory(),
                post.getThumbnailUrl(),
                post.getImages(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount() != null ? post.getCommentCount() : 0L,  // Phase 3: 댓글 수 추가
                post.getPublishedAt(),
                estimatedReadTime
        );
    }

    private int calculateReadTime(String content) {
        if (content == null || content.isEmpty()) return 1;
        int wordCount = content.length();
        int readTime = (int) Math.ceil(wordCount / 200.0);
        return Math.max(1, readTime); // 최소 1분
    }

    private Sort createSort(PostSortType sortBy, SortDirection direction) {
        Sort.Direction sortDirection = direction == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String field = switch (sortBy) {
            case CREATED_AT -> "createdAt";
            case PUBLISHED_AT -> "publishedAt";
            case VIEW_COUNT -> "viewCount";
            case LIKE_COUNT -> "likeCount";
            case TITLE -> "title";
        };

        return Sort.by(sortDirection, field);
    }

    private String extractAuthorName(String authorId) {
        // TODO: JWT 토큰에서 이름 추출하거나 User 서비스 호출
        // 현재는 간단히 authorId 반환
        return authorId;
    }

    // ===== 피드 기능 =====

    @Override
    public Page<PostSummaryResponse> getFeed(List<String> followingIds, int page, int size) {
        log.info("Fetching feed for {} following users, page: {}, size: {}", followingIds.size(), page, size);

        if (followingIds == null || followingIds.isEmpty()) {
            // 팔로잉이 없으면 빈 페이지 반환
            return Page.empty(PageRequest.of(page, size));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorIdInAndStatusOrderByPublishedAtDesc(
                followingIds, PostStatus.PUBLISHED, pageable);

        return posts.map(this::convertToPostListResponse);
    }
}
