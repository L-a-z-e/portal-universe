package com.portal.universe.blogservice.post.service;

import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostSortType;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.domain.SortDirection;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.repository.PostRepository;
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
                request.thumbnailUrl()
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
    public Page<PostListResponse> getPublishedPosts(int page, int size) {
        log.info("Fetching published posts, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> getPostsByAuthor(String authorId, int page, int size) {
        log.info("Fetching posts by author: {}", authorId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> getPostsByAuthorAndStatus(String authorId, PostStatus status, int page, int size) {
        log.info("Fetching posts by author: {} and status: {}", authorId, status);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(authorId, status, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> getPostsByCategory(String category, int page, int size) {
        log.info("Fetching posts by category: {}", category);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
                category, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> getPostsByTags(List<String> tags, int page, int size) {
        log.info("Fetching posts by tags: {}", tags);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTagsInAndStatusOrderByPublishedAtDesc(
                tags, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> searchPosts(String keyword, int page, int size) {
        log.info("Searching posts with keyword: {}", keyword);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByTextSearchAndStatus(keyword, PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public Page<PostListResponse> searchPostsAdvanced(PostSearchRequest searchRequest) {
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
    public Page<PostListResponse> getPopularPosts(int page, int size) {
        log.info("Fetching popular posts, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByStatusOrderByViewCountDescPublishedAtDesc(
                PostStatus.PUBLISHED, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    @Override
    public List<PostListResponse> getRelatedPosts(String postId, int limit) {
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
    public List<PostListResponse> getRecentPosts(int limit) {
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
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt(),
                post.getProductId()
        );
    }

    private PostListResponse convertToPostListResponse(Post post) {
        // 읽기 시간 계산 (평균 200자/분 기준)
        int estimatedReadTime = calculateReadTime(post.getContent());

        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getTags(),
                post.getCategory(),
                post.getThumbnailUrl(),
                post.getViewCount(),
                post.getLikeCount(),
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
}
