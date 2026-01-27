package com.portal.universe.blogservice.series.service;

import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.dto.PostSummaryResponse;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.blogservice.series.domain.Series;
import com.portal.universe.blogservice.series.dto.*;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시리즈 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final PostRepository postRepository;

    /**
     * 시리즈 생성
     */
    public SeriesResponse createSeries(SeriesCreateRequest request, String authorId, String authorName) {
        Series series = Series.builder()
                .name(request.name())
                .description(request.description())
                .authorId(authorId)
                .authorName(authorName)
                .thumbnailUrl(request.thumbnailUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        seriesRepository.save(series);
        return toResponse(series);
    }

    /**
     * 시리즈 수정
     */
    public SeriesResponse updateSeries(String seriesId, SeriesUpdateRequest request, String authorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        if (!series.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.SERIES_UPDATE_FORBIDDEN);
        }

        series.update(request.name(), request.description(), request.thumbnailUrl());
        seriesRepository.save(series);
        return toResponse(series);
    }

    /**
     * 시리즈 삭제
     */
    public void deleteSeries(String seriesId, String authorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        if (!series.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.SERIES_DELETE_FORBIDDEN);
        }

        seriesRepository.delete(series);
    }

    /**
     * 시리즈 상세 조회
     */
    @Transactional(readOnly = true)
    public SeriesResponse getSeriesById(String seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));
        return toResponse(series);
    }

    /**
     * 작성자별 시리즈 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SeriesListResponse> getSeriesByAuthor(String authorId) {
        List<Series> seriesList = seriesRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        return seriesList.stream()
                .map(this::toListResponse)
                .toList();
    }

    /**
     * 시리즈에 포스트 추가
     */
    public SeriesResponse addPostToSeries(String seriesId, String postId, String authorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        if (!series.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.SERIES_ADD_POST_FORBIDDEN);
        }

        series.addPost(postId);
        seriesRepository.save(series);
        return toResponse(series);
    }

    /**
     * 시리즈에서 포스트 제거
     */
    public SeriesResponse removePostFromSeries(String seriesId, String postId, String authorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        if (!series.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.SERIES_REMOVE_POST_FORBIDDEN);
        }

        series.removePost(postId);
        seriesRepository.save(series);
        return toResponse(series);
    }

    /**
     * 시리즈 내 포스트 순서 변경
     */
    public SeriesResponse reorderPosts(String seriesId, SeriesPostOrderRequest request, String authorId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        if (!series.getAuthorId().equals(authorId)) {
            throw new CustomBusinessException(BlogErrorCode.SERIES_REORDER_FORBIDDEN);
        }

        series.reorderPosts(request.postIds());
        seriesRepository.save(series);
        return toResponse(series);
    }

    /**
     * 시리즈에 포함된 포스트 목록 조회 (시리즈 내 순서 유지)
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getSeriesPosts(String seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

        List<String> postIds = series.getPostIds();
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }

        List<Post> posts = postRepository.findAllById(postIds);
        Map<String, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        return postIds.stream()
                .map(postMap::get)
                .filter(post -> post != null)
                .map(this::convertToPostSummary)
                .toList();
    }

    /**
     * 특정 포스트가 포함된 시리즈 조회
     */
    @Transactional(readOnly = true)
    public List<SeriesListResponse> getSeriesByPostId(String postId) {
        List<Series> seriesList = seriesRepository.findByPostIdsContaining(postId);
        return seriesList.stream()
                .map(this::toListResponse)
                .toList();
    }

    // ========== 변환 메서드 ==========

    private SeriesResponse toResponse(Series series) {
        return new SeriesResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getAuthorId(),
                series.getAuthorName(),
                series.getThumbnailUrl(),
                series.getPostIds(),
                series.getPostCount(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private SeriesListResponse toListResponse(Series series) {
        return new SeriesListResponse(
                series.getId(),
                series.getName(),
                series.getDescription(),
                series.getAuthorId(),
                series.getAuthorName(),
                series.getThumbnailUrl(),
                series.getPostCount(),
                series.getCreatedAt(),
                series.getUpdatedAt()
        );
    }

    private PostSummaryResponse convertToPostSummary(Post post) {
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
                post.getCommentCount() != null ? post.getCommentCount() : 0L,
                post.getPublishedAt(),
                estimatedReadTime
        );
    }

    private int calculateReadTime(String content) {
        if (content == null || content.isEmpty()) return 1;
        int charCount = content.length();
        int readTime = (int) Math.ceil(charCount / 200.0);
        return Math.max(1, readTime);
    }
}