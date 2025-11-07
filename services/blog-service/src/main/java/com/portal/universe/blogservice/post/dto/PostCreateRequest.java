package com.portal.universe.blogservice.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PostCreateRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        // PRD Phase 1: 기본 메타 정보
        @Size(max = 500, message = "요약은 500자를 초과할 수 없습니다")
        String summary,

        // PRD Phase 1: 콘텐츠 분류
        Set<String> tags,

        String category,

        // PRD Phase 1: SEO 최적화
        @Size(max = 160, message = "메타 설명은 160자를 초과할 수 없습니다")
        String metaDescription,

        // PRD Phase 2 대비: 멀티미디어
        String thumbnailUrl,

        // 즉시 발행 여부 (기본: 초안으로 저장)
        Boolean publishImmediately,

        // 기존 호환성 유지 (선택적)
        String productId
) {
    /**
     * publishImmediately가 null인 경우 기본값 false 반환
     */
    public Boolean publishImmediately() {
        return publishImmediately != null ? publishImmediately : false;
    }
}