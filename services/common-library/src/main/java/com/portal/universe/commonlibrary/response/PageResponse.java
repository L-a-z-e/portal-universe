package com.portal.universe.commonlibrary.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Page 내부 구조를 은닉하고 프론트엔드에 일관된 페이지네이션 응답을 제공합니다.
 * NestJS PaginatedResult와 동일한 필드명/구조를 사용합니다.
 *
 * @param <T> 페이지 항목의 타입
 */
@Getter
public class PageResponse<T> {

    private final List<T> items;
    private final int page;            // 1-based
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PageResponse(List<T> items, int page, int size, long totalElements, int totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * Spring Data Page를 PageResponse로 변환합니다.
     * Page의 0-based 페이지 번호를 1-based로 변환합니다.
     *
     * @param page Spring Data Page 객체
     * @param <T>  항목 타입
     * @return 1-based 페이지 번호를 가진 PageResponse
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
