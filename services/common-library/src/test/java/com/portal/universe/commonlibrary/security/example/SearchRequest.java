package com.portal.universe.commonlibrary.security.example;

import com.portal.universe.commonlibrary.security.sql.NoSqlInjection;
import com.portal.universe.commonlibrary.security.xss.NoXss;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 검색 요청 DTO 예시
 * 동적 쿼리에 사용되는 파라미터에 대한 보안 검증을 보여줍니다.
 */
public record SearchRequest(
        /**
         * 검색 키워드
         */
        @NoXss
        @NoSqlInjection
        String keyword,

        /**
         * 정렬 필드
         * 영문, 숫자, 언더스코어, 점만 허용
         */
        @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "정렬 필드는 영문, 숫자, 언더스코어, 점만 허용됩니다")
        @NoSqlInjection
        String sortBy,

        /**
         * 정렬 방향
         * ASC 또는 DESC만 허용
         */
        @Pattern(regexp = "^(ASC|DESC)$", message = "정렬 방향은 ASC 또는 DESC만 허용됩니다")
        String sortDirection,

        /**
         * 페이지 번호
         */
        @PositiveOrZero(message = "페이지 번호는 0 이상이어야 합니다")
        Integer page,

        /**
         * 페이지 크기
         */
        @Positive(message = "페이지 크기는 1 이상이어야 합니다")
        Integer size
) {
    public SearchRequest {
        // 기본값 설정
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESC";
        }
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }
}
