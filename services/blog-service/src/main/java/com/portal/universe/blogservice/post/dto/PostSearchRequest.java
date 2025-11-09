package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.PostSortType;
import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.domain.SortDirection;

import java.time.LocalDateTime;
import java.util.List;

public record PostSearchRequest(
        // 키워드 검색 (제목 + 내용)
        String keyword,

        // 카테고리 필터
        String category,

        // 태그 필터 (다중 선택)
        List<String> tags,

        // 상태 필터 (관리자용)
        PostStatus status,

        // 작성자 필터
        String authorId,

        // 기간 필터
        LocalDateTime startDate,
        LocalDateTime endDate,

        // 정렬 기준
        PostSortType sortBy,

        // 정렬 방향
        SortDirection sortDirection,

        // 페이지 크기
        Integer size,

        // 페이지 번호
        Integer page
) {
    /**
     * 기본값 설정
     */
    public PostSortType sortBy() {
        return sortBy != null ? sortBy : PostSortType.PUBLISHED_AT;
    }

    public SortDirection sortDirection() {
        return sortDirection != null ? sortDirection : SortDirection.DESC;
    }

    public Integer size() {
        return size != null && size > 0 && size <= 50 ? size : 10;
    }

    public Integer page() {
        return page != null && page >= 0 ? page : 0;
    }
}