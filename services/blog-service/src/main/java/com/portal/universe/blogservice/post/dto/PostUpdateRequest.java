package com.portal.universe.blogservice.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record PostUpdateRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        @Size(max = 500, message = "요약은 500자를 초과할 수 없습니다")
        String summary,

        @Size(max = 20, message = "태그는 20개를 초과할 수 없습니다")
        Set<String> tags,

        String category,

        @Size(max = 160, message = "메타 설명은 160자를 초과할 수 없습니다")
        String metaDescription,

        String thumbnailUrl,

        List<String> images
) {}