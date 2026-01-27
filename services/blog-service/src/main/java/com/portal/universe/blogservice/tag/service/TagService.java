package com.portal.universe.blogservice.tag.service;

import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.tag.domain.Tag;
import com.portal.universe.blogservice.tag.dto.*;
import com.portal.universe.blogservice.tag.repository.TagRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 태그 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 태그 생성 (수동)
     */
    public TagResponse createTag(TagCreateRequest request) {
        String normalizedName = Tag.normalizeName(request.name());

        // 중복 체크
        if (tagRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CustomBusinessException(BlogErrorCode.TAG_ALREADY_EXISTS);
        }

        Tag tag = Tag.builder()
                .name(normalizedName)
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();

        tagRepository.save(tag);
        log.info("Tag created: {}", normalizedName);
        return toResponse(tag);
    }

    /**
     * 태그 자동 생성 또는 기존 태그 반환
     * Post 생성 시 호출
     */
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

    /**
     * 태그의 포스트 카운트 증가
     * Post 생성 시 호출
     */
    public void incrementTagPostCount(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        tag.incrementPostCount();
        tagRepository.save(tag);
    }

    /**
     * 태그의 포스트 카운트 감소
     * Post 삭제 시 호출
     */
    public void decrementTagPostCount(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        tagRepository.findByNameIgnoreCase(normalizedName)
                .ifPresent(tag -> {
                    tag.decrementPostCount();
                    tagRepository.save(tag);
                });
    }

    /**
     * 여러 태그의 포스트 카운트 일괄 증가
     */
    public void incrementTagPostCounts(List<String> tagNames) {
        tagNames.forEach(this::incrementTagPostCount);
    }

    /**
     * 여러 태그의 포스트 카운트 일괄 감소
     */
    public void decrementTagPostCounts(List<String> tagNames) {
        tagNames.forEach(this::decrementTagPostCount);
    }

    /**
     * 태그 상세 조회
     */
    @Transactional(readOnly = true)
    public TagResponse getTagByName(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));
        return toResponse(tag);
    }

    /**
     * 전체 태그 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 인기 태그 조회 (postCount 기준)
     */
    @Transactional(readOnly = true)
    public List<TagStatsResponse> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByPostCountDesc(0L, pageable);

        return tags.stream()
                .map(tag -> new TagStatsResponse(tag.getName(), tag.getPostCount(), null))
                .toList();
    }

    /**
     * 최근 사용된 태그 조회
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getRecentlyUsedTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByLastUsedAtDesc(0L, pageable);

        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 태그 검색 (자동완성용)
     */
    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByNameContainingIgnoreCaseOrderByPostCountDesc(keyword, pageable);

        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 태그 설명 업데이트
     */
    public TagResponse updateTagDescription(String tagName, String description) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        tag.updateDescription(description);
        tagRepository.save(tag);
        return toResponse(tag);
    }

    /**
     * 사용되지 않는 태그 삭제 (관리자용)
     */
    public void deleteUnusedTags() {
        List<Tag> unusedTags = tagRepository.findByPostCount(0L);
        tagRepository.deleteAll(unusedTags);
        log.info("Deleted {} unused tags", unusedTags.size());
    }

    /**
     * 태그 강제 삭제
     */
    public void deleteTag(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        if (!tag.isUnused()) {
            log.warn("Deleting tag '{}' with {} posts", normalizedName, tag.getPostCount());
        }

        tagRepository.delete(tag);
        log.info("Tag deleted: {}", normalizedName);
    }

    // ========== 변환 메서드 ==========

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getPostCount(),
                tag.getDescription(),
                tag.getCreatedAt(),
                tag.getLastUsedAt()
        );
    }
}