package com.portal.universe.blogservice.tag.service;

import com.mongodb.client.result.UpdateResult;
import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.blogservice.tag.dto.TagCreateRequest;
import com.portal.universe.blogservice.tag.dto.TagResponse;
import com.portal.universe.blogservice.tag.domain.Tag;
import com.portal.universe.blogservice.tag.repository.TagRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService 테스트")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TagService tagService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createTag 메서드")
    class CreateTagTests {

        @Test
        @DisplayName("should_createTag_with_normalizedName")
        void should_createTag_with_normalizedName() {
            // given
            TagCreateRequest request = new TagCreateRequest("Java Spring", "Description");
            String normalizedName = Tag.normalizeName(request.name());

            when(tagRepository.existsByNameIgnoreCase(normalizedName)).thenReturn(false);

            Tag savedTag = Tag.builder()
                    .name(normalizedName)
                    .description(request.description())
                    .build();
            ReflectionTestUtils.setField(savedTag, "id", "tag-1");

            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // when
            TagResponse result = tagService.createTag(request);

            // then
            assertThat(result.name()).isEqualTo(normalizedName);
            verify(tagRepository).existsByNameIgnoreCase(normalizedName);
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("should_throwException_when_duplicateName")
        void should_throwException_when_duplicateName() {
            // given
            TagCreateRequest request = new TagCreateRequest("Java", "Description");
            String normalizedName = Tag.normalizeName(request.name());

            when(tagRepository.existsByNameIgnoreCase(normalizedName)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.TAG_ALREADY_EXISTS);
            verify(tagRepository, never()).save(any(Tag.class));
        }
    }

    @Nested
    @DisplayName("getOrCreateTag 메서드")
    class GetOrCreateTagTests {

        @Test
        @DisplayName("should_returnExistingTag")
        void should_returnExistingTag() {
            // given
            String normalizedName = Tag.normalizeName("Java");
            Tag existingTag = Tag.builder()
                    .name(normalizedName)
                    .description("Existing")
                    .build();
            ReflectionTestUtils.setField(existingTag, "id", "tag-1");

            when(tagRepository.findByNameIgnoreCase(normalizedName)).thenReturn(Optional.of(existingTag));

            // when
            Tag result = tagService.getOrCreateTag("Java");

            // then
            assertThat(result).isEqualTo(existingTag);
            verify(tagRepository).findByNameIgnoreCase(normalizedName);
            verify(tagRepository, never()).save(any(Tag.class));
        }

        @Test
        @DisplayName("should_createNewTag_when_notFound")
        void should_createNewTag_when_notFound() {
            // given
            String normalizedName = Tag.normalizeName("NewTag");

            when(tagRepository.findByNameIgnoreCase(normalizedName)).thenReturn(Optional.empty());

            Tag newTag = Tag.builder()
                    .name(normalizedName)
                    .build();
            ReflectionTestUtils.setField(newTag, "id", "tag-2");

            when(tagRepository.save(any(Tag.class))).thenReturn(newTag);

            // when
            Tag result = tagService.getOrCreateTag("NewTag");

            // then
            assertThat(result.getName()).isEqualTo(normalizedName);
            verify(tagRepository).findByNameIgnoreCase(normalizedName);
            verify(tagRepository).save(any(Tag.class));
        }
    }

    @Nested
    @DisplayName("incrementTagPostCounts 메서드")
    class IncrementTagPostCountsTests {

        @Test
        @DisplayName("should_bulkIncrement")
        void should_bulkIncrement() {
            // given
            List<String> tagNames = List.of("java", "spring");

            when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Tag.class)))
                    .thenReturn(UpdateResult.acknowledged(2, 2L, null));

            // when
            tagService.incrementTagPostCounts(tagNames);

            // then
            verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(Tag.class));
        }
    }

    @Nested
    @DisplayName("decrementTagPostCounts 메서드")
    class DecrementTagPostCountsTests {

        @Test
        @DisplayName("should_skipWhenEmpty")
        void should_skipWhenEmpty() {
            // when
            tagService.decrementTagPostCounts(List.of());

            // then
            verify(mongoTemplate, never()).updateMulti(any(Query.class), any(Update.class), eq(Tag.class));
        }
    }

    @Nested
    @DisplayName("deleteUnusedTags 메서드")
    class DeleteUnusedTagsTests {

        @Test
        @DisplayName("should_deleteTagsWithZeroPostCount")
        void should_deleteTagsWithZeroPostCount() {
            // given
            Tag unusedTag = Tag.builder()
                    .name("unused")
                    .description("desc")
                    .build();
            ReflectionTestUtils.setField(unusedTag, "id", "tag-1");

            when(tagRepository.findByPostCount(0L)).thenReturn(List.of(unusedTag));

            // when
            tagService.deleteUnusedTags();

            // then
            verify(tagRepository).findByPostCount(0L);
            verify(tagRepository).deleteAll(List.of(unusedTag));
        }
    }
}
