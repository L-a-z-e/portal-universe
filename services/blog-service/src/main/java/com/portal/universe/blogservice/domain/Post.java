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

/**
 * 블로그 게시물을 나타내는 MongoDB의 Document 클래스입니다.
 */
@Document(collection = "posts") // "posts" 컬렉션에 매핑
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA/Mongo 프레임워크를 위한 기본 생성자
public class Post {

    /**
     * 게시물과 연관된 상품의 ID
     */
    private String productId;

    /**
     * 게시물의 고유 ID (MongoDB에서 자동 생성)
     */
    @Id
    private String id;

    /**
     * 게시물 제목
     */
    private String title;

    /**
     * 게시물 내용
     */
    private String content;

    /**
     * 작성자의 고유 ID (Auth 서비스의 User ID)
     */
    private String authorId;

    /**
     * 생성 일시 (MongoDB Auditing 기능으로 자동 주입)
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 마지막 수정 일시 (MongoDB Auditing 기능으로 자동 주입)
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Post(String title, String content, String authorId, String productId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.productId = productId;
    }

    /**
     * 게시물의 제목과 내용을 수정합니다.
     * @param title 새로운 제목
     * @param content 새로운 내용
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}