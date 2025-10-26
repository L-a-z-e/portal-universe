package com.portal.universe.blogservice.service;

import com.portal.universe.blogservice.domain.Post;
import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;
import com.portal.universe.blogservice.dto.PostUpdateRequest;
import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.blogservice.repository.PostRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BlogService 인터페이스의 구현 클래스입니다.
 * 게시물 관련 비즈니스 로직을 실제로 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImple implements BlogService{

    private final PostRepository postRepository;

    @Override
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        // 1. 요청 DTO를 Post 도큐먼트 객체로 변환합니다.
        Post newPost = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(authorId)
                .productId(request.productId())
                .build();

        // 2. 리포지토리를 통해 데이터베이스에 저장합니다.
        Post savedPost = postRepository.save(newPost);

        // 3. 저장된 도큐먼트 객체를 응답 DTO로 변환하여 반환합니다.
        return convertToResponse(savedPost);
    }

    @Override
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findAll();

        return posts.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        return convertToResponse(post);
    }

    @Override
    public PostResponse updatePost(String postId, PostUpdateRequest request, String userId) {
        // 1. 수정할 게시물을 ID로 조회합니다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 2. 요청한 사용자가 게시물 작성자인지 확인합니다.
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
        }

        // 3. 게시물 내용을 수정합니다.
        post.update(request.title(), request.content());

        // 4. 수정된 게시물을 저장하고 응답 DTO로 변환하여 반환합니다.
        Post updatedPost = postRepository.save(post);
        return convertToResponse(updatedPost);
    }

    @Override
    public void deletePost(String postId, String userId) {
        // 1. 삭제할 게시물을 ID로 조회합니다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        // 2. 요청한 사용자가 게시물 작성자인지 확인합니다.
        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(BlogErrorCode.POST_DELETE_FORBIDDEN);
        }

        // 3. 게시물을 삭제합니다.
        postRepository.delete(post);
    }

    @Override
    public List<PostResponse> getPostsByProductId(String productId) {
        return postRepository.findByProductId(productId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Post 도큐먼트 객체를 PostResponse DTO로 변환하는 헬퍼 메서드입니다.
     * @param post 변환할 Post 객체
     * @return 변환된 PostResponse 객체
     */
    private PostResponse convertToResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getProductId()
        );
    }
}