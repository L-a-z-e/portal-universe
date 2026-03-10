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
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final MongoTemplate mongoTemplate;

    public TagResponse createTag(TagCreateRequest request) {
        String normalizedName = Tag.normalizeName(request.name());

        // 중복 체크
        if (tagRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CustomBusinessException(BlogErrorCode.TAG_ALREADY_EXISTS);
        }

        Tag tag = Tag.builder()
                .name(normalizedName)
                .description(request.description())
                .lastUsedAt(Instant.now())
                .build();

        tagRepository.save(tag);
        log.info("Tag created: {}", normalizedName);
        return toResponse(tag);
    }

    public Tag getOrCreateTag(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);

        return tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .name(normalizedName)
                            .lastUsedAt(Instant.now())
                            .build();
                    tagRepository.save(newTag);
                    log.info("Auto-created tag: {}", normalizedName);
                    return newTag;
                });
    }

    /**
     * 여러 태그 일괄 생성 (없으면 생성, 있으면 lastUsedAt만 갱신)
     * MongoDB Bulk Upsert로 Race Condition 방지 + N쿼리 → 1쿼리
     */
    public void ensureTagsExist(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;

        List<String> normalized = tagNames.stream()
                .map(Tag::normalizeName)
                .toList();

        Instant now = Instant.now();
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Tag.class);

        for (String name : normalized) {
            Query query = Query.query(Criteria.where("name").is(name));
            Update update = new Update()
                    .setOnInsert("name", name)
                    .setOnInsert("postCount", 0L)
                    .setOnInsert("createdAt", now)
                    .set("lastUsedAt", now);
            bulkOps.upsert(query, update);
        }

        bulkOps.execute();
        log.info("Ensured {} tags exist via bulk upsert", normalized.size());
    }

    public void incrementTagPostCount(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        tag.incrementPostCount();
        tagRepository.save(tag);
    }

    public void decrementTagPostCount(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        tagRepository.findByNameIgnoreCase(normalizedName)
                .ifPresent(tag -> {
                    tag.decrementPostCount();
                    tagRepository.save(tag);
                });
    }

    public void incrementTagPostCounts(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        List<String> normalized = tagNames.stream()
                .map(Tag::normalizeName)
                .toList();
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("name").in(normalized)),
                new Update().inc("postCount", 1).set("lastUsedAt", Instant.now()),
                Tag.class
        );
    }

    public void decrementTagPostCounts(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        List<String> normalized = tagNames.stream()
                .map(Tag::normalizeName)
                .toList();
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("name").in(normalized)),
                new Update().inc("postCount", -1),
                Tag.class
        );
    }

    @Transactional(readOnly = true)
    public TagResponse getTagByName(String tagName) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));
        return toResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagStatsResponse> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByPostCountDesc(0L, pageable);

        return tags.stream()
                .map(tag -> new TagStatsResponse(tag.getName(), tag.getPostCount(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getRecentlyUsedTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByPostCountGreaterThanOrderByLastUsedAtDesc(0L, pageable);

        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByNameContainingIgnoreCaseOrderByPostCountDesc(keyword, pageable);

        return tags.stream()
                .map(this::toResponse)
                .toList();
    }

    public TagResponse updateTagDescription(String tagName, String description) {
        String normalizedName = Tag.normalizeName(tagName);
        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.TAG_NOT_FOUND));

        tag.updateDescription(description);
        tagRepository.save(tag);
        return toResponse(tag);
    }

    public void deleteUnusedTags() {
        List<Tag> unusedTags = tagRepository.findByPostCount(0L);
        tagRepository.deleteAll(unusedTags);
        log.info("Deleted {} unused tags", unusedTags.size());
    }

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