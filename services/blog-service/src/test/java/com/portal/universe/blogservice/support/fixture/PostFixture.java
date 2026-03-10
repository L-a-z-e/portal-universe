package com.portal.universe.blogservice.support.fixture;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class PostFixture {

    private PostFixture() {}

    public static Post create() {
        return builder().build();
    }

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private String id = "post-1";
        private String title = "Test Title";
        private String content = "Test Content";
        private String summary = "Test Summary";
        private String authorId = "user-1";
        private String authorUsername = "testuser";
        private String authorNickname = "TestUser";
        private PostStatus status = PostStatus.PUBLISHED;
        private Set<String> tags = Set.of("java", "spring");
        private String category = "TECH";
        private Long viewCount = 0L;
        private Long likeCount = 0L;
        private Long commentCount = 0L;
        private Instant publishedAt = Instant.now();
        private String thumbnailUrl;
        private List<String> images = List.of();

        public PostBuilder id(String id) { this.id = id; return this; }
        public PostBuilder title(String title) { this.title = title; return this; }
        public PostBuilder content(String content) { this.content = content; return this; }
        public PostBuilder summary(String summary) { this.summary = summary; return this; }
        public PostBuilder authorId(String authorId) { this.authorId = authorId; return this; }
        public PostBuilder authorUsername(String authorUsername) { this.authorUsername = authorUsername; return this; }
        public PostBuilder authorNickname(String authorNickname) { this.authorNickname = authorNickname; return this; }
        public PostBuilder status(PostStatus status) { this.status = status; return this; }
        public PostBuilder tags(Set<String> tags) { this.tags = tags; return this; }
        public PostBuilder category(String category) { this.category = category; return this; }
        public PostBuilder viewCount(Long viewCount) { this.viewCount = viewCount; return this; }
        public PostBuilder likeCount(Long likeCount) { this.likeCount = likeCount; return this; }
        public PostBuilder commentCount(Long commentCount) { this.commentCount = commentCount; return this; }
        public PostBuilder publishedAt(Instant publishedAt) { this.publishedAt = publishedAt; return this; }
        public PostBuilder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public PostBuilder images(List<String> images) { this.images = images; return this; }

        public Post build() {
            Post post = Post.builder()
                    .title(title)
                    .content(content)
                    .summary(summary)
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .authorNickname(authorNickname)
                    .status(status)
                    .tags(tags)
                    .category(category)
                    .thumbnailUrl(thumbnailUrl)
                    .images(images)
                    .build();
            ReflectionTestUtils.setField(post, "id", id);
            ReflectionTestUtils.setField(post, "viewCount", viewCount);
            ReflectionTestUtils.setField(post, "likeCount", likeCount);
            ReflectionTestUtils.setField(post, "commentCount", commentCount);
            if (publishedAt != null) {
                ReflectionTestUtils.setField(post, "publishedAt", publishedAt);
            }
            return post;
        }
    }
}
