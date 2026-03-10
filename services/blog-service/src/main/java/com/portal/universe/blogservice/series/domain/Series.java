package com.portal.universe.blogservice.series.domain;

import com.portal.universe.commonlibrary.domain.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class Series extends BaseDocument {

    @Id
    private String id;

    @NotBlank(message = "시리즈 제목은 필수입니다")
    @Size(max = 100, message = "시리즈 제목은 100자를 초과할 수 없습니다")
    private String name;

    @Size(max = 500, message = "시리즈 설명은 500자를 초과할 수 없습니다")
    private String description;

    @Indexed
    @NotBlank(message = "작성자는 필수입니다")
    private String authorId;

    private String authorUsername;

    private String authorNickname;

    private String thumbnailUrl;

    /**
     * 시리즈에 포함된 포스트 ID 목록 (순서 유지)
     * 리스트의 인덱스가 곧 순서
     */
    @Builder.Default
    private List<String> postIds = new ArrayList<>();

    @Version
    private Long version;

    public void update(String name, String description, String thumbnailUrl) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        // updatedAt is managed by BaseDocument @LastModifiedDate
    }

    public void addPost(String postId) {
        if (!this.postIds.contains(postId)) {
            this.postIds.add(postId);
            // updatedAt is managed by BaseDocument @LastModifiedDate
        }
    }

    public void addPostAt(String postId, int index) {
        if (!this.postIds.contains(postId)) {
            if (index < 0 || index > this.postIds.size()) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            this.postIds.add(index, postId);
            // updatedAt is managed by BaseDocument @LastModifiedDate
        }
    }

    public void removePost(String postId) {
        this.postIds.remove(postId);
        // updatedAt is managed by BaseDocument @LastModifiedDate
    }

    public void reorderPosts(List<String> newPostIds) {
        // 검증: 기존 포스트 ID와 동일한지 확인
        if (!this.postIds.containsAll(newPostIds) || !newPostIds.containsAll(this.postIds)) {
            throw new IllegalArgumentException("Post IDs mismatch");
        }
        this.postIds = new ArrayList<>(newPostIds);
        // updatedAt is managed by BaseDocument @LastModifiedDate
    }

    public boolean containsPost(String postId) {
        return this.postIds.contains(postId);
    }

    /** @return 0-based index, 없으면 -1 */
    public int getPostOrder(String postId) {
        return this.postIds.indexOf(postId);
    }

    public int getPostCount() {
        return this.postIds.size();
    }

    public boolean isEmpty() {
        return this.postIds.isEmpty();
    }
}