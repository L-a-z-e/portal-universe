package com.portal.universe.blogservice.support.fixture;

import com.portal.universe.blogservice.comment.domain.Comment;

public final class CommentFixture {

    private CommentFixture() {}

    public static Comment create() {
        return builder().build();
    }

    public static CommentBuilder builder() {
        return new CommentBuilder();
    }

    public static class CommentBuilder {
        private String id = "comment-1";
        private String postId = "post-1";
        private String authorId = "user-1";
        private String authorUsername = "testuser";
        private String authorNickname = "TestUser";
        private String content = "Test comment content";
        private String parentCommentId;
        private Long likeCount = 0L;
        private Boolean isDeleted = false;

        public CommentBuilder id(String id) { this.id = id; return this; }
        public CommentBuilder postId(String postId) { this.postId = postId; return this; }
        public CommentBuilder authorId(String authorId) { this.authorId = authorId; return this; }
        public CommentBuilder authorUsername(String authorUsername) { this.authorUsername = authorUsername; return this; }
        public CommentBuilder authorNickname(String authorNickname) { this.authorNickname = authorNickname; return this; }
        public CommentBuilder content(String content) { this.content = content; return this; }
        public CommentBuilder parentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; return this; }
        public CommentBuilder likeCount(Long likeCount) { this.likeCount = likeCount; return this; }
        public CommentBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }

        public Comment build() {
            return Comment.builder()
                    .id(id)
                    .postId(postId)
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .authorNickname(authorNickname)
                    .content(content)
                    .parentCommentId(parentCommentId)
                    .likeCount(likeCount)
                    .isDeleted(isDeleted)
                    .build();
        }
    }
}
