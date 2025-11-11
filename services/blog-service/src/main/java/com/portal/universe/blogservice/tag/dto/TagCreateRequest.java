package com.portal.universe.blogservice.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 태그 생성 요청 DTO
 */
public record TagCreateRequest(
        @NotBlank(message = "태그 이름은 필수입니다")
        @Size(max = 50, message = "태그 이름은 50자를 초과할 수 없습니다")
        String name,

        @Size(max = 200, message = "태그 설명은 200자를 초과할 수 없습니다")
        String description
) {}