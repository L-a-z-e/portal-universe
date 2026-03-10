package com.portal.universe.blogservice.support.fixture;

import com.portal.universe.blogservice.like.domain.Like;

public final class LikeFixture {

    private LikeFixture() {}

    public static Like create() {
        return builder().build();
    }

    public static LikeBuilder builder() {
        return new LikeBuilder();
    }

    public static class LikeBuilder {
        private String id = "like-1";
        private String postId = "post-1";
        private String userId = "user-1";
        private String userName = "testuser";
        private String nickname = "TestUser";

        public LikeBuilder id(String id) { this.id = id; return this; }
        public LikeBuilder postId(String postId) { this.postId = postId; return this; }
        public LikeBuilder userId(String userId) { this.userId = userId; return this; }
        public LikeBuilder userName(String userName) { this.userName = userName; return this; }
        public LikeBuilder nickname(String nickname) { this.nickname = nickname; return this; }

        public Like build() {
            return Like.builder()
                    .id(id)
                    .postId(postId)
                    .userId(userId)
                    .userName(userName)
                    .nickname(nickname)
                    .build();
        }
    }
}
