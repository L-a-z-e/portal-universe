package com.portal.universe.blogservice.series.service;

import com.portal.universe.blogservice.series.domain.Series;
import com.portal.universe.blogservice.series.dto.*;
import com.portal.universe.blogservice.series.exception.SeriesNotFoundException;
import com.portal.universe.blogservice.series.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시리즈 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SeriesService {

    private final SeriesRepository seriesRepository;

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
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        if (!series.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 수정할 수 있습니다.");
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
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        if (!series.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 삭제할 수 있습니다.");
        }

        seriesRepository.delete(series);
    }

    /**
     * 시리즈 상세 조회
     */
    @Transactional(readOnly = true)
    public SeriesResponse getSeriesById(String seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));
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
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        if (!series.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 포스트를 추가할 수 있습니다.");
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
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        if (!series.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 포스트를 제거할 수 있습니다.");
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
                .orElseThrow(() -> new SeriesNotFoundException(seriesId));

        if (!series.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("작성자만 순서를 변경할 수 있습니다.");
        }

        series.reorderPosts(request.postIds());
        seriesRepository.save(series);
        return toResponse(series);
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
}