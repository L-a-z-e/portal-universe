package com.portal.universe.blogservice.tag.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tag 도메인 테스트")
class TagTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("should_createTag_with_defaultPostCount")
        void should_createTag_with_defaultPostCount() {
            // given & when
            Tag tag = Tag.builder()
                    .name("Spring Boot")
                    .build();

            // then
            assertThat(tag.getPostCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("normalizeName() 테스트")
    class NormalizeNameTest {

        @Test
        @DisplayName("should_toLowerCaseAndTrim")
        void should_toLowerCaseAndTrim() {
            // given
            String name = "  Spring Boot  ";

            // when
            String normalized = Tag.normalizeName(name);

            // then
            assertThat(normalized).isEqualTo("spring boot");
        }

        @Test
        @DisplayName("should_returnNull_when_nameIsNull")
        void should_returnNull_when_nameIsNull() {
            // given
            String name = null;

            // when
            String normalized = Tag.normalizeName(name);

            // then
            assertThat(normalized).isNull();
        }
    }

    @Nested
    @DisplayName("incrementPostCount() 테스트")
    class IncrementPostCountTest {

        @Test
        @DisplayName("should_incrementAndUpdateLastUsedAt")
        void should_incrementAndUpdateLastUsedAt() {
            // given
            Tag tag = Tag.builder()
                    .name("Spring Boot")
                    .build();

            // when
            tag.incrementPostCount();

            // then
            assertThat(tag.getPostCount()).isEqualTo(1L);
            assertThat(tag.getLastUsedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("decrementPostCount() 테스트")
    class DecrementPostCountTest {

        @Test
        @DisplayName("should_decrementPostCount")
        void should_decrementPostCount() {
            // given
            Tag tag = Tag.builder()
                    .name("Spring Boot")
                    .build();
            tag.incrementPostCount();

            // when
            tag.decrementPostCount();

            // then
            assertThat(tag.getPostCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should_notGoBelowZero")
        void should_notGoBelowZero() {
            // given
            Tag tag = Tag.builder()
                    .name("Spring Boot")
                    .build();

            // when
            tag.decrementPostCount();

            // then
            assertThat(tag.getPostCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("isUnused() 테스트")
    class IsUnusedTest {

        @Test
        @DisplayName("should_returnTrue_when_postCountIsZero")
        void should_returnTrue_when_postCountIsZero() {
            // given
            Tag tag = Tag.builder()
                    .name("Spring Boot")
                    .build();

            // when & then
            assertThat(tag.isUnused()).isTrue();
        }
    }
}
