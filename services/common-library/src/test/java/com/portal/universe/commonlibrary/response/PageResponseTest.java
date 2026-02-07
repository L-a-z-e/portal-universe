package com.portal.universe.commonlibrary.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResponse 테스트")
class PageResponseTest {

    @Test
    @DisplayName("Page를 PageResponse로 변환 - 기본 케이스")
    void from_basic() {
        List<String> content = List.of("a", "b", "c");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);

        PageResponse<String> result = PageResponse.from(page);

        assertThat(result.getItems()).containsExactly("a", "b", "c");
        assertThat(result.getPage()).isEqualTo(1);       // 0-based → 1-based
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 페이지 변환")
    void from_emptyPage() {
        Page<String> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        PageResponse<String> result = PageResponse.from(page);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("중간 페이지 변환 - 0-based 2 → 1-based 3")
    void from_middlePage() {
        List<String> content = List.of("x", "y");
        Page<String> page = new PageImpl<>(content, PageRequest.of(2, 5), 12);

        PageResponse<String> result = PageResponse.from(page);

        assertThat(result.getPage()).isEqualTo(3);       // 0-based 2 → 1-based 3
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(3);  // ceil(12/5) = 3
    }

    @Test
    @DisplayName("마지막 페이지 변환")
    void from_lastPage() {
        List<String> content = List.of("last");
        Page<String> page = new PageImpl<>(content, PageRequest.of(4, 3), 13);

        PageResponse<String> result = PageResponse.from(page);

        assertThat(result.getPage()).isEqualTo(5);       // 0-based 4 → 1-based 5
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(13);
        assertThat(result.getTotalPages()).isEqualTo(5);  // ceil(13/3) = 5
    }
}
