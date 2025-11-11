package com.portal.universe.blogservice.tag.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

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
public class Tag {

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

    /**
     * 태그 설명 (선택적)
     */
    @Size(max = 200, message = "태그 설명은 200자를 초과할 수 없습니다")
    private String description;

    /**
     * 태그 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 태그 최종 사용일시
     */
    private LocalDateTime lastUsedAt;

    // ========== 비즈니스 메서드 ==========

    /**
     * 포스트 개수 증가
     */
    public void incrementPostCount() {
        this.postCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 포스트 개수 감소
     */
    public void decrementPostCount() {
        if (this.postCount > 0) {
            this.postCount--;
        }
    }

    /**
     * 태그 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 최종 사용일시 업데이트
     */
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 사용되지 않는 태그인지 확인
     */
    public boolean isUnused() {
        return this.postCount == 0;
    }

    /**
     * 태그 이름 정규화 (소문자 변환, 공백 제거)
     */
    public static String normalizeName(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase();
    }
}