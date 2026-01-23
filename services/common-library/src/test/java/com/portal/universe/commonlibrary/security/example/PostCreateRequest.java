package com.portal.universe.commonlibrary.security.example;

import com.portal.universe.commonlibrary.security.sql.NoSqlInjection;
import com.portal.universe.commonlibrary.security.xss.NoXss;
import com.portal.universe.commonlibrary.security.xss.SafeHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 블로그 게시글 생성 요청 DTO 예시
 * XSS 및 SQL Injection 방어 어노테이션 사용법을 보여줍니다.
 */
public record PostCreateRequest(
        /**
         * 제목 - 순수 텍스트만 허용 (HTML 태그 금지)
         */
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다")
        @NoXss(message = "제목에 HTML/Script 태그는 허용되지 않습니다")
        String title,

        /**
         * 본문 - 제한적인 HTML 태그 허용
         */
        @NotBlank(message = "본문은 필수입니다")
        @SafeHtml(
                allowedTags = {"p", "br", "b", "i", "u", "strong", "em", "a", "ul", "ol", "li", "img", "h1", "h2", "h3"},
                message = "허용되지 않은 HTML 태그가 포함되어 있습니다"
        )
        String content,

        /**
         * 카테고리 - 순수 텍스트
         */
        @NoXss
        @NoSqlInjection
        String category,

        /**
         * 태그 - 순수 텍스트
         */
        @NoXss
        String tags
) {
}
