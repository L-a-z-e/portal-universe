package com.portal.universe.blogservice.comment.domain;

import com.portal.universe.commonlibrary.domain.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;

/**
 * 댓글 Entity
 * 블로그 포스트에 대한 댓글 및 대댓글 관리
 */
@Document(collection = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor  // Lombok이 빌더를 위해 필요
@Builder
public class Comment extends BaseDocument {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "게시물 ID는 필수입니다")
    private String postId;

    @Indexed
    @NotBlank(message = "작성자 ID는 필수입니다")
    private String authorId;

    private String authorUsername;

    private String authorNickname;

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;

    /**
     * 대댓글 구분
     * null이면 루트 댓글, 값이 있으면 해당 댓글의 자식
     */
    private String parentCommentId;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Boolean isDeleted = false;

    public void update(String content) {
        this.content = content;
        // updatedAt is managed by BaseDocument @LastModifiedDate
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public boolean isRootComment() {
        return parentCommentId == null;
    }
}
