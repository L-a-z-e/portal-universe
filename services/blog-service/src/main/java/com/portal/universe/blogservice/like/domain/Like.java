package com.portal.universe.blogservice.like.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 좋아요 Entity
 * 포스트에 대한 사용자의 좋아요 기록 관리
 * (postId, userId) 복합 인덱스로 중복 방지
 */
@Document(collection = "likes")
@CompoundIndex(name = "postId_userId_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Like {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "게시물 ID는 필수입니다")
    private String postId;

    @Indexed
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    /**
     * 사용자 이름 (표시용)
     */
    private String userName;

    @CreatedDate
    private LocalDateTime createdAt;
}
