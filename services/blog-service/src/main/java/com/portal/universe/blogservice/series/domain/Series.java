package com.portal.universe.blogservice.series.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시리즈 Entity
 * 블로그 포스트를 연재물로 묶어서 관리
 *
 * 예시: "Vue.js 완벽 가이드" 시리즈
 *   1편: Vue 시작하기
 *   2편: 컴포넌트 이해하기
 *   3편: 상태 관리
 */
@Document(collection = "series")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Series {

    @Id
    private String id;

    /**
     * 시리즈 제목
     */
    @NotBlank(message = "시리즈 제목은 필수입니다")
    @Size(max = 100, message = "시리즈 제목은 100자를 초과할 수 없습니다")
    private String name;

    /**
     * 시리즈 설명
     */
    @Size(max = 500, message = "시리즈 설명은 500자를 초과할 수 없습니다")
    private String description;

    /**
     * 작성자 ID
     */
    @Indexed
    @NotBlank(message = "작성자는 필수입니다")
    private String authorId;

    /**
     * 작성자 이름 (표시용)
     */
    private String authorName;

    /**
     * 시리즈 썸네일 이미지 URL
     */
    private String thumbnailUrl;

    /**
     * 시리즈에 포함된 포스트 ID 목록 (순서 유지)
     * 리스트의 인덱스가 곧 순서
     */
    @Builder.Default
    private List<String> postIds = new ArrayList<>();

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    // ========== 비즈니스 메서드 ==========

    /**
     * 시리즈 정보 수정
     */
    public void update(String name, String description, String thumbnailUrl) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 시리즈에 포스트 추가 (맨 뒤)
     */
    public void addPost(String postId) {
        if (!this.postIds.contains(postId)) {
            this.postIds.add(postId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 시리즈에 포스트 추가 (특정 위치)
     */
    public void addPostAt(String postId, int index) {
        if (!this.postIds.contains(postId)) {
            if (index < 0 || index > this.postIds.size()) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            this.postIds.add(index, postId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 시리즈에서 포스트 제거
     */
    public void removePost(String postId) {
        this.postIds.remove(postId);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 포스트 순서 변경
     */
    public void reorderPosts(List<String> newPostIds) {
        // 검증: 기존 포스트 ID와 동일한지 확인
        if (!this.postIds.containsAll(newPostIds) || !newPostIds.containsAll(this.postIds)) {
            throw new IllegalArgumentException("Post IDs mismatch");
        }
        this.postIds = new ArrayList<>(newPostIds);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 특정 포스트가 시리즈에 포함되어 있는지 확인
     */
    public boolean containsPost(String postId) {
        return this.postIds.contains(postId);
    }

    /**
     * 특정 포스트의 순서(인덱스) 반환
     * @return 0-based index, 없으면 -1
     */
    public int getPostOrder(String postId) {
        return this.postIds.indexOf(postId);
    }

    /**
     * 시리즈에 포함된 포스트 개수
     */
    public int getPostCount() {
        return this.postIds.size();
    }

    /**
     * 시리즈가 비어있는지 확인
     */
    public boolean isEmpty() {
        return this.postIds.isEmpty();
    }
}