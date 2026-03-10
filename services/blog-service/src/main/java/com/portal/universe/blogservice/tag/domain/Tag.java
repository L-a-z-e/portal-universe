package com.portal.universe.blogservice.tag.domain;

import com.portal.universe.commonlibrary.domain.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * 태그 Entity
 * 블로그 포스트 분류 및 검색을 위한 태그 관리
 *
 * 역정규화 전략: postCount를 직접 저장하여 빠른 인기 태그 조회
 */
@Document(collection = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tag extends BaseDocument {

    @Id
    private String id;

    /**
     * 태그 이름 (고유값, 검색 인덱스)
     * 예: "Vue.js", "Spring Boot", "MongoDB"
     */
    @TextIndexed
    @Indexed(unique = true)
    @NotBlank(message = "태그 이름은 필수입니다")
    @Size(max = 50, message = "태그 이름은 50자를 초과할 수 없습니다")
    private String name;

    /**
     * 태그가 사용된 포스트 개수 (역정규화)
     * Post 생성/삭제 시 함께 업데이트
     */
    @Builder.Default
    private Long postCount = 0L;

    @Size(max = 200, message = "태그 설명은 200자를 초과할 수 없습니다")
    private String description;

    private Instant lastUsedAt;

    public void incrementPostCount() {
        this.postCount++;
        this.lastUsedAt = Instant.now();
    }

    public void decrementPostCount() {
        if (this.postCount > 0) {
            this.postCount--;
        }
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateLastUsedAt() {
        this.lastUsedAt = Instant.now();
    }

    public boolean isUnused() {
        return this.postCount == 0;
    }

    public static String normalizeName(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase();
    }
}