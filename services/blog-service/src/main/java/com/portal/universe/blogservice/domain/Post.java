package com.portal.universe.blogservice.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    private String productId;

    @Id
    private String id;

    private String title;

    private String content;

    private String authorId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Post(String title, String content, String authorId, String productId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.productId = productId;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
